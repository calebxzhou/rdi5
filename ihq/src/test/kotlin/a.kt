import calebxzhou.rdi.ihq.model.mcsm.InstanceConfig
import calebxzhou.rdi.ihq.service.McsmService
import java.io.File

/**
 * calebxzhou @ 2025-08-24 21:04
 */
suspend fun main() {
    val instanceConfig = InstanceConfig(
        nickname = "test",
        startCommand = "java -jar test.jar",
        cwd = File(".", "rdi").absolutePath.toString(),
    )
    val uid = McsmService.createInstance(instanceConfig).data.instanceUuid
    McsmService.updateInstance(uid,instanceConfig).also { print(it) }
}