package calebxzhou.rdi.master.service

import calebxzhou.rdi.master.CONF
import calebxzhou.rdi.master.exception.RequestError
import calebxzhou.rdi.common.model.HostStatus
import calebxzhou.mykotutils.log.Loggers
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.command.BuildImageResultCallback
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.*
import com.github.dockerjava.api.model.PortBinding.parse
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object DockerService {
    private val lgr by Loggers
    private val client: DockerClient by lazy {
        val dockerConfig = CONF.docker
        val configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("tcp://${dockerConfig.host}:${dockerConfig.port}")
            .withApiVersion(dockerConfig.apiVersion)

        if (dockerConfig.tlsEnabled) {
            configBuilder.withDockerTlsVerify(dockerConfig.tlsVerify)
            dockerConfig.certPath.takeIf { it.isNotBlank() }?.let { configBuilder.withDockerCertPath(it) }
        } else {
            configBuilder.withDockerTlsVerify(false)
        }

        val config = configBuilder.build()
        val httpClient: DockerHttpClient = OkDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .connectTimeout(300_000)
            .readTimeout(300_000)
            .build()

        DockerClientBuilder.getInstance(config)
            .withDockerHttpClient(httpClient)
            .build()
    }


    private fun findContainer(containerName: String, includeStopped: Boolean = true) =
        client.listContainersCmd()
            .withNameFilter(listOf(containerName))
            .withShowAll(includeStopped)
            .exec().firstOrNull()

    fun listContainers(includeStopped: Boolean = true) =
        client.listContainersCmd()
            .withShowAll(includeStopped)
            .exec()

    fun createContainer(
        port: Int,
        containerName: String,
        mounts: List<Mount>,
        image: String,
        env: List<String>,
        cmd: List<String>? = null
    ): String {

        val hostConfig = HostConfig.newHostConfig()
            .withPortBindings(parse("$port:$port"))
            .withCpuCount(4L)  // Limit to 2 CPUs
            .withMemory(4L * 1024 * 1024 * 1024)  // 8GB RAM limit
            .withMemorySwap(8L * 1024 * 1024 * 1024)  //4G swap
            .withPidsLimit(512L)
            .withExtraHosts("host.docker.internal:host-gateway")
            .withMounts(mounts)


        val createCmd = client.createContainerCmd(image)
            .withName(containerName)
            .withEnv(env)
            .withExposedPorts(ExposedPort(port))
            .withHostConfig(hostConfig)


        if (!cmd.isNullOrEmpty()) {
            createCmd.withCmd(cmd).cmd
        }

        return createCmd.exec().id
    }

    fun sendCommand(containerName: String, command: String) {
        if (command.isBlank()) throw RequestError("命令不能为空")

        val container = findContainer(containerName)
            ?: throw RequestError("找不到此容器")

        if (!container.state.equals("running", ignoreCase = true)) {
            throw RequestError("主机未启动")
        }

        val sanitized = command.trimEnd().replace("'", "'\"'\"'")
        val shellCmd = "if [ -w /proc/1/fd/0 ]; then printf '%s\\n' '$sanitized' > /proc/1/fd/0; else exit 42; fi"

        val execCreate = client.execCreateCmd(container.id)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd("sh", "-c", shellCmd)
            .exec()

        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val callback = object : Adapter<Frame>() {
            override fun onNext(frame: Frame) {
                val payload = String(frame.payload)
                when (frame.streamType) {
                    StreamType.STDERR -> stderr.append(payload)
                    StreamType.STDOUT -> stdout.append(payload)
                    else -> Unit
                }
            }
        }

        client.execStartCmd(execCreate.id)
            .withDetach(false)
            .exec(callback)
            .awaitCompletion()

        val exitCode = client.inspectExecCmd(execCreate.id).exec().exitCodeLong

        if (exitCode != null && exitCode != 0L) {
            val msg = stderr.toString().ifBlank { stdout.toString() }
            throw RequestError("命令发送失败: ${msg.trim()}")
        }

        if (stderr.isNotEmpty()) {
            lgr.warn { "Command '$command' stderr: ${stderr.toString().trim()}" }
        }
    }

    fun createVolume(volumeName: String) {
        client.createVolumeCmd().withName(volumeName).exec()
    }

    fun cloneVolume(sourceVolume: String, targetVolume: String) {
        createVolume(targetVolume)
        val helperContainer = client.createContainerCmd("busybox:latest")
            .withCmd("sh", "-c", "cp -a /from/. /to/ || true")
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withBinds(
                        Bind.parse("$sourceVolume:/from:ro"),
                        Bind.parse("$targetVolume:/to")
                    )
            )
            .exec().id
        try {
            client.startContainerCmd(helperContainer).exec()
            client.waitContainerCmd(helperContainer).start().awaitStatusCode()
        } catch (e: Exception) {
            lgr.warn { "Error cloning volume $sourceVolume -> $targetVolume: ${e.message}" }
            throw RequestError("复制存档失败")
        } finally {
            try {
                client.removeContainerCmd(helperContainer)
                    .withForce(true)
                    .withRemoveVolumes(false)
                    .exec()
            } catch (_: Exception) {
            }
        }
    }

    fun deleteContainer(containerName: String, removeVolumes: Boolean = false) {
        // First, try to force stop and remove the container if it exists
        findContainer(containerName)?.let { container ->
            try {
                // Force kill container if it's running
                if (container.state == "running") {
                    client.killContainerCmd(container.id).exec()
                }
                // Force remove the container
                client.removeContainerCmd(container.id)
                    .withForce(true)
                    .withRemoveVolumes(removeVolumes)
                    .exec()
            } catch (e: Exception) {
                lgr.warn { "Error force stopping/removing container ${container.id}: ${e.message}" }
            }
        }
    }

    fun deleteVolume(volume: String) {
        try {
            client.removeVolumeCmd(volume)
                .exec()
        } catch (e: Exception) {
            lgr.warn { "Error removing volume $volume: ${e.message}" }
            // Volume might not exist or be in use, continue anyway
        }
    }

    fun uploadFile(containerName: String, localPath: Path, targetPath: String) {
        uploadFiles(containerName, listOf(localPath), targetPath)
    }

    fun uploadFiles(containerName: String, localPaths: List<Path>, targetPath: String) {
        if (localPaths.isEmpty()) throw RequestError("本地文件列表不得为空")

        val container = findContainer(containerName, includeStopped = true)
            ?: throw RequestError("找不到此容器")

        val normalizedTarget = targetPath.replace('\\', '/').trim()
        if (normalizedTarget.isEmpty()) {
            throw RequestError("目标路径不能为空")
        }
        val targetSegments = normalizedTarget.split('/')
            .filter { it.isNotEmpty() }
        if (targetSegments.any { it == "." || it == ".." }) {
            throw RequestError("目标路径非法")
        }

        val remoteDir = when {
            normalizedTarget == "/" -> "/"
            normalizedTarget.endsWith('/') -> normalizedTarget.trimEnd('/').ifEmpty { "/" }
            else -> normalizedTarget
        }.let { path -> if (path.startsWith('/')) path else "/$path" }

        val tarBytes = ByteArrayOutputStream()
        try {
            // Docker copy API accepts a tar stream; wrap the provided files accordingly.
            TarArchiveOutputStream(tarBytes).use { tar ->
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)

                localPaths.forEach { localPath ->
                    if (!Files.exists(localPath) || !Files.isRegularFile(localPath)) {
                        throw RequestError("本地文件不存在或不可读: ${localPath}")
                    }

                    val entryName = localPath.fileName?.toString()
                        ?.takeIf { it.isNotEmpty() }
                        ?: throw RequestError("目标文件名不能为空")

                    val entry = TarArchiveEntry(entryName)
                    entry.size = Files.size(localPath)
                    tar.putArchiveEntry(entry)
                    Files.newInputStream(localPath).use { input ->
                        input.copyTo(tar)
                    }
                    tar.closeArchiveEntry()
                }
            }

            ByteArrayInputStream(tarBytes.toByteArray()).use { tarInput ->
                client.copyArchiveToContainerCmd(container.id)
                    .withRemotePath(remoteDir)
                    .withTarInputStream(tarInput)
                    .exec()
            }
        } catch (e: Exception) {
            lgr.warn(e) { "uploadFile失败: $containerName -> $targetPath" }
            throw RequestError("上传文件失败: ${e.message ?: "未知错误"}")
        }
    }

    fun deleteFile(containerName: String, remotePath: String) {
        val container = findContainer(containerName, includeStopped = true)
            ?: throw RequestError("找不到此容器")
        val started = isStarted(containerName)
        //不开机删不了文件
        if (!started) start(containerName)
        val normalizedPath = remotePath.replace('\\', '/').trim()
        if (normalizedPath.isEmpty()) {
            throw RequestError("目标路径不能为空")
        }

        val validatedPath = when {
            normalizedPath.startsWith('/') -> normalizedPath
            else -> "/$normalizedPath"
        }

        if (validatedPath == "/" || validatedPath == "//") {
            throw RequestError("不能删除根目录")
        }

        // Convert to POSIX-friendly path for sh commands
        val sanitizedPath = validatedPath.replace("'", "'\"'\"'")

        try {
            val execCreate = client.execCreateCmd(container.id)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("sh", "-c", "rm -rf '$sanitizedPath'")
                .exec()

            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val callback = object : Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    val payload = String(frame.payload)
                    when (frame.streamType) {
                        StreamType.STDOUT -> stdout.append(payload)
                        StreamType.STDERR -> stderr.append(payload)
                        else -> Unit
                    }
                }
            }

            client.execStartCmd(execCreate.id)
                .withDetach(false)
                .exec(callback)
                .awaitCompletion()

            val exitCode = client.inspectExecCmd(execCreate.id).exec().exitCodeLong
            if (exitCode != null && exitCode != 0L) {
                val errorMsg = stderr.toString().ifBlank { stdout.toString() }
                throw RequestError("删除文件失败: ${errorMsg.trim().ifEmpty { "未知错误" }}")
            }

            if (stdout.isNotEmpty()) {
                lgr.info { "deleteFile stdout for $containerName:$validatedPath -> ${stdout.toString()}" }
            }
            if (stderr.isNotEmpty()) {
                lgr.warn { "deleteFile stderr for $containerName:$validatedPath -> ${stderr.toString()}" }
            }
            //如果删除之前主机是关的 现在给他关了
            if (!started) stop(containerName)
        } catch (e: Exception) {
            if (e is RequestError) throw e
            lgr.warn(e) { "deleteFile失败: $containerName -> $remotePath" }
            throw RequestError("删除文件失败: ${e.message ?: "未知错误"}")
        }
    }

    private fun imageExistsLocally(repoTag: String): Boolean {
        return try {
            client.inspectImageCmd(repoTag).exec()
            true
        } catch (_: NotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun start(containerName: String) {
        val container = findContainer(containerName) ?: throw RequestError("找不到此容器")
        client.startContainerCmd(container.id)
            .exec()
    }

    fun stop(containerName: String) {
        // Check if container exists first
        val container = findContainer(containerName) ?: throw RequestError("找不到此容器")
        // Only stop if the container is actually running
        if (container.state.equals("running", ignoreCase = true)) {
            client.stopContainerCmd(container.id).exec()
        } else {
            throw RequestError("早就停了")
        }

    }

    fun restart(containerName: String) {
        // Check if container exists first
        val container = findContainer(containerName) ?: throw RequestError("找不到此容器")
        client.restartContainerCmd(container.id).exec()
    }

    // startLine/endLine are indices from the newest line (0 = newest), slicing [startLine, endLine)
    // 两个参数都是“从后往前”的行号：0 表示最新一行，返回区间为 [startLine, endLine)
    fun getLog(containerName: String, startLine: Int = 0, endLine: Int): String {
        return try {
            val callback = object : Adapter<Frame>() {
                val logs = mutableListOf<String>()

                override fun onNext(frame: Frame) {
                    val logLine = String(frame.payload).trim()
                    if (logLine.isNotEmpty()) {
                        logs.add(logLine)
                    }
                }
            }

            // Get all logs first
            client.logContainerCmd(containerName)
                .withStdOut(true)
                .withStdErr(true)
                .exec(callback)
                .awaitCompletion()

            // Reverse logs to most-recent-first so index 0 is the newest line
            val allLogs = callback.logs.reversed()
            val total = allLogs.size

            if (total == 0) return ""

            // Normalize and clamp the requested range [startLine, endLine)
            val from = startLine.coerceAtLeast(0)
            val toExclusive = endLine.coerceAtMost(total)

            if (from >= toExclusive) return ""

            allLogs.subList(from, toExclusive).joinToString("\n")
        } catch (e: Exception) {
            lgr.warn { "Error getting logs for container $containerName: ${e.message}" }
            ""
        }
    }

    /**
     * Follow the container log stream and invoke [onLine] for each new line.
     * Returns a [Closeable] you should close to stop streaming.
     */
    fun listenLog(
        containerName: String,
        onLine: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
        onFinished: () -> Unit = {}
    ): Closeable {
        val callback = object : Adapter<Frame>() {
            override fun onNext(frame: Frame) {
                val logLine = String(frame.payload).trim()
                if (logLine.isNotEmpty()) {
                    try {
                        onLine(logLine)
                    } catch (_: Throwable) {
                    }
                }
            }

            override fun onError(throwable: Throwable) {
                try {
                    onError(throwable)
                } catch (_: Throwable) {
                }
                super.onError(throwable)
            }

            override fun onComplete() {
                try {
                    onFinished()
                } catch (_: Throwable) {
                }
                super.onComplete()
            }
        }
        client.logContainerCmd(containerName)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .withTail(0)
            .exec(callback)
        return callback
    }

    fun pause(containerName: String) {

        client.pauseContainerCmd(containerName).exec()

    }

    fun unpause(containerName: String) {
        client.unpauseContainerCmd(containerName).exec()
    }

    fun isStarted(containerName: String): Boolean {
        return try {
            val container = findContainer(containerName)
            container?.state?.equals("running", ignoreCase = true) == true
        } catch (e: Exception) {
            lgr.warn { "Error checking container $containerName status: ${e.message}" }
            false
        }
    }

    fun getContainerStatus(containerName: String): HostStatus {
        return try {
            val container = findContainer(containerName) ?: return HostStatus.UNKNOWN
            val state = container.state?.lowercase() ?: return HostStatus.UNKNOWN
            when (state) {
                "running" -> HostStatus.STARTED
                "paused" -> HostStatus.PAUSED
                "exited", "created", "dead", "removing", "stopped" -> HostStatus.STOPPED
                else -> HostStatus.UNKNOWN
            }
        } catch (e: Exception) {
            lgr.warn { "Error getting status for container $containerName: ${e.message}" }
            HostStatus.UNKNOWN
        }
    }

    suspend fun buildImage(
        name: String, contextPath: File, onLine: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
        onFinished: () -> Unit = {}
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val buildCmd = client.buildImageCmd()
                    .withDockerfile((contextPath).resolve("Dockerfile"))
                    .withBaseDirectory((contextPath))
                    .withTags(setOf(name))

                CONF.proxy?.let { proxy ->
                    val proxyUrl = "http://${proxy.host}:${proxy.port}"
                    //用主机代理
                    buildCmd//.withNetworkMode("host")
                        .withBuildArg("HTTP_PROXY", proxyUrl)
                        .withBuildArg("HTTPS_PROXY", proxyUrl)
                        .withBuildArg("NO_PROXY", "localhost,127.0.0.1")

                }

                val buildResponse = buildCmd
                    .exec(object : BuildImageResultCallback() {
                        override fun onNext(item: BuildResponseItem) {
                            val logLine = item.stream?.trim() ?: item.errorDetail?.message?.trim().orEmpty()
                            if (logLine.isNotEmpty()) {
                                try {
                                    onLine(logLine)
                                } catch (_: Throwable) {
                                }
                            }
                            super.onNext(item)
                        }

                        override fun onError(throwable: Throwable) {
                            try {
                                onError(throwable)
                            } catch (_: Throwable) {
                            }
                            super.onError(throwable)
                        }

                        override fun onComplete() {
                            try {
                                onFinished()
                            } catch (_: Throwable) {
                            }
                            super.onComplete()
                        }
                    })
                    .awaitImageId()

                buildResponse ?: throw RequestError("镜像构建失败")
            } catch (e: Exception) {
                lgr.warn { "Error building image $name: ${e.message}" }
                throw RequestError("镜像构建失败: ${e.message}")
            }
        }
    }

    fun deleteImage(tag: String, force: Boolean = true) {
        try {
            client.removeImageCmd(tag)
                .withForce(force)
                .withNoPrune(false)
                .exec()
            lgr.info { "删除镜像成功: $tag" }
        } catch (_: NotFoundException) {
            lgr.info { "尝试删除不存在的镜像: $tag" }
        } catch (e: Exception) {
            lgr.warn(e) { "删除镜像失败: $tag" }
        }
    }
    /*fun Room.getVolumeSize(): Long {
        return try {
            // Check if container is running first
            val container = findContainerById(containerName, includeStopped = false) ?: return 0L

            // If container is not found or not running, return 0

            // Execute du command in the existing running container
            val execCreateResponse = client.execCreateCmd(containerName)
                .withCmd("sh", "-c", "du -sb /data | cut -f1")
                .withAttachStdout(true)
                .exec()

            val outputStream = java.io.ByteArrayOutputStream()
            val callback = object : Adapter<Frame>() {
                override fun onNext(frame: Frame) {
                    outputStream.write(frame.payload)
                }
            }

            client.execStartCmd(execCreateResponse.id)
                .exec(callback)
                .awaitCompletion()

            val output = outputStream.toString().trim()
            output.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }*/

}