package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.Room
import calebxzhou.rdi.ihq.net.randomPort
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
import org.bson.types.ObjectId
import java.time.Duration

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
    val ObjectId.asVolumeName
        get() = "data_$this"

    private fun findContainerById(containerId: String, includeStopped: Boolean = true) =
        client.listContainersCmd()
            .withIdFilter(listOf(containerId))
            .withShowAll(includeStopped)
            .exec().firstOrNull()

    fun create(containerName: String, volumeName: String, image: String): String {
        val port = randomPort
        client.createVolumeCmd().withName(volumeName).exec()
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

    fun delete(volume: String, containerId: String) {
        // First, try to force stop and remove the container if it exists
        findContainerById(containerId)?.let { container ->
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

        // Then try to remove the volume
        try {
            client.removeVolumeCmd(volume)
                .exec()
        } catch (e: Exception) {
            lgr.warn { "Error removing volume $volume: ${e.message}" }
            // Volume might not exist or be in use, continue anyway
        }
    }
    fun start(containerId: String) {

            client.startContainerCmd(containerId).exec()

    }

    fun stop(containerId: String) {
        // Check if container exists first
        val container = findContainerById(containerId)
        if (container == null) {
            lgr.warn { "Container $containerId not found, skipping stop operation" }
            return
        }

        // Only stop if the container is actually running
        if (container.state.equals("running", ignoreCase = true)) {
            client.stopContainerCmd(containerId).exec()
            lgr.info { "Successfully stopped container $containerId" }
        } else {
            lgr.warn { "Container $containerId is already in state: ${container.state}, skipping stop operation" }
        }

    }
    fun getLog(containerId: String, page: Int): String {
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
            client.logContainerCmd(containerId)
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
            lgr.warn { "Error getting logs for container $containerId: ${e.message}" }
            ""
        }
    }
    fun pause(containerId: String) {

        client.pauseContainerCmd(containerId).exec()

    }

    fun unpause(containerId: String) {
        client.unpauseContainerCmd(containerId).exec()
    }

    fun isStarted(containerId: String): Boolean {
        return try {
            val container = findContainerById(containerId)
            container?.state?.equals("running", ignoreCase = true) == true
        } catch (e: Exception) {
            lgr.warn { "Error checking container $containerId status: ${e.message}" }
            false
        }
    }

    /*fun Room.getVolumeSize(): Long {
        return try {
            // Check if container is running first
            val container = findContainerById(containerId, includeStopped = false) ?: return 0L

            // If container is not found or not running, return 0

            // Execute du command in the existing running container
            val execCreateResponse = client.execCreateCmd(containerId)
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