package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class TerminalOption(
    val haveColor: Boolean = true,
    val pty: Boolean = true,
    val ptyWindowCol: Int = 164,
    val ptyWindowRow: Int = 40
)

@Serializable
data class EventTask(
    val autoStart: Boolean = false,
    val autoRestart: Boolean = true,
    val ignore: Boolean = false
)

@Serializable
data class PingConfig(
    val ip: String = "",
    val port: Int = 25565,
    val type: Int = 1
)

@Serializable
data class DockerConfig(
    val containerName: String = "",
    val image: String = "",
    val ports: List<String> = emptyList(),
    val extraVolumes: List<String> = emptyList(),
    val memory: Int = 0,
    val networkMode: String = "bridge",
    val networkAliases: List<String> = emptyList(),
    val cpusetCpus: String = "",
    val cpuUsage: Int = 0,
    val maxSpace: Int = 0,
    val io: Int = 0,
    val network: Int = 0,
    val workingDir: String = "/workspace/",
    val env: List<String> = emptyList(),
    val changeWorkdir: Boolean = true
)

@Serializable
data class ExtraServiceConfig(
    val openFrpTunnelId: String = "",
    val openFrpToken: String = ""
)

@Serializable
data class InstanceConfig(
    val nickname: String,
    val startCommand: String,
    val stopCommand: String = "stop",
    val cwd: String,
    val ie: String = "utf8",                        // 输入 encode
    val oe: String = "utf8",                        // 输出 encode
    val createDatetime: Long = System.currentTimeMillis(),
    val lastDatetime: Long = System.currentTimeMillis(),
    val type: String = "minecraft/java",                // 实例类型
    val tag: List<String> = emptyList(),
    val endTime: Long = 0L,
    val fileCode: String = "utf8",
    val processType: String = "general",
    val updateCommand: String = "",
    val runAs: String = "",
    val actionCommandList: List<String> = emptyList(),
    val crlf: Int = 2,
    val category: Int = 0,
    val basePort: Int = 10035,
    val docker: DockerConfig = DockerConfig(),

    // Steam RCON
    val enableRcon: Boolean = false,
    val rconPassword: String = "",
    val rconPort: Int = 25565,
    val rconIp: String = "",

    // 终端选项
    val terminalOption: TerminalOption = TerminalOption(),
    val eventTask: EventTask = EventTask(),
    val pingConfig: PingConfig = PingConfig(),
    val extraServiceConfig: ExtraServiceConfig = ExtraServiceConfig()
)
