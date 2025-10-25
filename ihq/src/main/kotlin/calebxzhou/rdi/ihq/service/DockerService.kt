package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.ServerStatus
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.BuildResponseItem
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding.parse
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import java.io.Closeable
import java.time.Duration
import com.github.dockerjava.api.exception.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DockerService {
    private val client by lazy {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("tcp://localhost:2375") // Use TCP instead of named pipes
            .withDockerTlsVerify(false)
            .build()

        val httpClient: DockerHttpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
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

    fun createContainer(port: Int, containerName: String, volumeName: String?, image: String): String {
        val hostConfig = HostConfig.newHostConfig()
            .withPortBindings(parse("$port:65232"))
            .withCpuCount(2L)  // Limit to 2 CPUs
            .withMemory(2L * 1024 * 1024 * 1024)  // 2GB RAM limit
            .withMemorySwap(4L * 1024 * 1024 * 1024)  //4G swap
            
        volumeName?.let { hostConfig.withBinds(Bind.parse("$it:/data")) }

        return client.createContainerCmd(image)
            .withName(containerName)
            .withEnv(listOf("EULA=TRUE"))
            .withExposedPorts(ExposedPort(65232))
            .withHostConfig(hostConfig)
            .withTty(true)
            .withStdinOpen(true)
            .withAttachStdin(true)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec().id
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

    fun update(containerName: String, image: String): String {

        // Inspect current container to reuse settings
        val inspect = client.inspectContainerCmd(containerName).exec()
        val name = inspect.name?.trim('/') ?: (inspect.id ?: containerName)
        val hostConfig = inspect.hostConfig
        val config = inspect.config

        // Pull latest image
        val repoTag = if (image.contains(":")) image else "$image:latest"

        val hasLocal = imageExistsLocally(repoTag)
        if (hasLocal) {
            lgr.info { "Image $repoTag found locally; skipping pull." }
        } else {
            throw RequestError("找不到此整合包")
        }

        // Determine running state and stop
        val wasRunning = isStarted(containerName)
        if (wasRunning) {
            try {
                client.stopContainerCmd(containerName).exec()
            } catch (_: Exception) {
            }
        }
        // Remove old container but keep volumes
        try {
            client.removeContainerCmd(containerName)
                .withForce(true)
                .withRemoveVolumes(false)
                .exec()
        } catch (_: Exception) {
        }

        // Recreate container with same options
        val createImageRef = repoTag
        val createCmd = client.createContainerCmd(createImageRef)
            .withName(name)

        // Env
        config?.env?.let { if (it.isNotEmpty()) createCmd.withEnv(it.toList()) }
        // Exposed ports
        config?.exposedPorts?.let { createCmd.withExposedPorts(*it) }
        // Host config (ports/volumes/limits)
        hostConfig?.let { createCmd.withHostConfig(it) }

        val newId = createCmd.exec().id

        if (wasRunning) {
            client.startContainerCmd(newId).exec()
        }
        lgr.info { "Updated container $name to image $repoTag" }
        return newId
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
    fun getLog(containerName: String, startLine: Int =0, endLine: Int): String {
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

    fun getContainerStatus(containerName: String): ServerStatus {
        return try {
            val container = findContainer(containerName) ?: return ServerStatus.UNKNOWN
            val state = container.state?.lowercase() ?: return ServerStatus.UNKNOWN
            when (state) {
                "running" -> ServerStatus.STARTED
                "paused" -> ServerStatus.PAUSED
                "exited", "created", "dead", "removing", "stopped" -> ServerStatus.STOPPED
                else -> ServerStatus.UNKNOWN
            }
        } catch (e: Exception) {
            lgr.warn { "Error getting status for container $containerName: ${e.message}" }
            ServerStatus.UNKNOWN
        }
    }
    suspend fun buildImage(name: String, contextPath: String,onLine: (String) -> Unit,
                   onError: (Throwable) -> Unit = {},
                   onFinished: () -> Unit = {}): String {
        return withContext(Dispatchers.IO) {
            try {
                val buildResponse = client.buildImageCmd()
                    .withDockerfile(java.io.File(contextPath).resolve("Dockerfile"))
                    .withBaseDirectory(java.io.File(contextPath))
                    .withTags(setOf(name))
                    .exec(object : com.github.dockerjava.api.command.BuildImageResultCallback() {
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