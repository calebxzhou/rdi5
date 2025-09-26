package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.ServerStatus
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.Bind
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

    fun createContainer(port: Int, containerName: String, volumeName: String, image: String): String {
        return client.createContainerCmd(image)
            .withName(containerName)
            .withEnv(listOf("EULA=TRUE"))
            .withExposedPorts(ExposedPort(65232))
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withPortBindings(parse("$port:65232"))
                    .withBinds(Bind.parse("$volumeName:/data"))
                    .withCpuCount(2L)  // Limit to 2 CPUs
                    .withMemory(2L * 1024 * 1024 * 1024)  // 2GB RAM limit
            )
            .exec().id
    }

    fun createVolume(volumeName: String) {
        client.createVolumeCmd().withName(volumeName).exec()
    }

    fun deleteContainer(containerName: String) {
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
                    .withRemoveVolumes(true)
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
        client.startContainerCmd(container.id).exec()

    }

    fun stop(containerName: String) {
        // Check if container exists first
        val container = findContainer(containerName) ?: throw RequestError("找不到此容器")
        // Only stop if the container is actually running
        if (container.state.equals("running", ignoreCase = true)) {
            client.stopContainerCmd(container.id).exec()
            lgr.info { "Successfully stopped container $containerName" }
        } else {
            throw RequestError("早就停了")
        }

    }

    fun getLog(containerName: String, page: Int): String {
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

            // Reverse logs to show most recent first, then paginate
            val allLogs = callback.logs.reversed()
            val startIndex = page * 50
            val endIndex = (page + 1) * 50

            // Return the requested page of logs
            if (startIndex >= allLogs.size) {
                ""
            } else {
                val pageEndIndex = minOf(endIndex, allLogs.size)
                allLogs.subList(startIndex, pageEndIndex).joinToString("\n")
            }
        } catch (e: Exception) {
            lgr.warn { "Error getting logs for container $containerName: ${e.message}" }
            ""
        }
    }

    /**
     * Follow the container log stream and invoke [onLine] for each new line.
     * Returns a [Closeable] you should close to stop streaming.
     */
    fun followLog(
        containerName: String,
        tail: Int = 50,
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
            .withTail(tail)
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