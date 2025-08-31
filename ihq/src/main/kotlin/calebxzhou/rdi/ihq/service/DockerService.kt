package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.model.Room
import calebxzhou.rdi.ihq.net.randomPort
import calebxzhou.rdi.ihq.net.httpClient
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding.parse
import com.github.dockerjava.core.DockerClientBuilder
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

object DockerService {
    private val client
        get() = DockerClientBuilder.getInstance().build()


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
        client.listContainersCmd()
            .withIdFilter(listOf(containerId)).exec().firstOrNull()?.let {
                client.stopContainerCmd(it.id).exec()
                client.removeContainerCmd(it.id).exec()
            }
        client.removeVolumeCmd(volume).exec()
    }
    fun start(containerId: String){
        client.startContainerCmd(containerId).exec()
    }
    fun stop(containerId: String){
        client.stopContainerCmd(containerId).exec()
    }
    fun pause(containerId: String){
        client.pauseContainerCmd(containerId).exec()
    }
    fun unpause(containerId: String){
        client.unpauseContainerCmd(containerId).exec()
    }


}