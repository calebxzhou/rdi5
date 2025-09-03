package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.model.Room
import calebxzhou.rdi.ihq.net.randomPort
import calebxzhou.rdi.ihq.net.httpClient
import com.github.dockerjava.api.async.ResultCallback.Adapter
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding.parse
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import java.time.Duration
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId

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

    fun create(containerName: String,volumeName: String,image: String): String {
        val port = randomPort
        client.createVolumeCmd().withName(volumeName).exec()
        return client.createContainerCmd(image)
            .withName(containerName)
            .withEnv(listOf("EULA=TRUE"))
            .withExposedPorts(ExposedPort(25565))
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withPortBindings(parse("$port:25565"))
                    .withBinds(Bind.parse("$volumeName:/data"))
                    .withCpuCount(1L)  // Limit to 2 CPUs
                    .withMemory(2L * 1024 * 1024 * 1024)  // 2GB RAM limit
            )
            .exec().id
    }
    fun delete(volume: String,containerId: String){
        try {
            // First, try to stop and remove the container if it exists
            client.listContainersCmd()
                .withIdFilter(listOf(containerId))
                .withShowAll(true) // Include stopped containers
                .exec().firstOrNull()?.let { container ->
                    try {
                        // Stop container if it's running
                        if (container.state == "running") {
                            client.stopContainerCmd(container.id).exec()
                        }
                        // Remove the container
                        client.removeContainerCmd(container.id).exec()
                    } catch (e: Exception) {
                        println("Error stopping/removing container ${container.id}: ${e.message}")
                    }
                }

            // Then try to remove the volume
            try {
                client.removeVolumeCmd(volume).exec()
            } catch (e: Exception) {
                println("Error removing volume $volume: ${e.message}")
                // Volume might not exist or be in use, continue anyway
            }
        } catch (e: Exception) {
            println("Error in Docker delete operation: ${e.message}")
            // Don't throw the exception, just log it
        }
    }

    fun start(containerId: String){
        try {
            client.startContainerCmd(containerId).exec()
        } catch (e: Exception) {
            println("Error starting container $containerId: ${e.message}")
            throw e
        }
    }

    fun stop(containerId: String){
        try {
            client.stopContainerCmd(containerId).exec()
        } catch (e: Exception) {
            println("Error stopping container $containerId: ${e.message}")
            // Don't throw for stop operations, container might already be stopped
        }
    }

    fun pause(containerId: String){
        try {
            client.pauseContainerCmd(containerId).exec()
        } catch (e: Exception) {
            println("Error pausing container $containerId: ${e.message}")
            throw e
        }
    }

    fun unpause(containerId: String){
        try {
            client.unpauseContainerCmd(containerId).exec()
        } catch (e: Exception) {
            println("Error unpausing container $containerId: ${e.message}")
            throw e
        }
    }


    fun Room.getVolumeSize(): Long {
        return try {
            // Check if container is running first
            val containers = client.listContainersCmd()
                .withIdFilter(listOf(containerId))
                .exec()

            // If container is not found or not running, return 0
            if (containers.isEmpty()) {
                return 0L
            }

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
    }

}