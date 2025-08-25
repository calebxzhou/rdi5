package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class TerminalOption(
    val haveColor: Boolean = false,
    val pty: Boolean = true
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
    // Add docker configuration properties as needed
    // This can be expanded based on the actual docker config structure
    val image: String? = null,
    val containerName: String? = null,
    val ports: List<String>? = null,
    val volumes: List<String>? = null
)

@Serializable
data class InstanceConfig(
    val nickname: String,
    val startCommand: String,
    val stopCommand: String,
    val cwd: String,
    val ie: String = "utf-8",                        // 输入 encode
    val oe: String = "utf-8",                        // 输出 encode
    val createDatetime: Long,
    val lastDatetime: Long,
    val type: String = "universal",                // 实例类型
    val tag: List<String> = emptyList(),
    val endTime: Long,
    val fileCode: String = "utf8",
    val processType: String = "general",
    val updateCommand: String = "",
    val actionCommandList: List<String> = emptyList(),
    val crlf: Int = 2,
    val docker: DockerConfig? = null,

    // Steam RCON
    val enableRcon: Boolean = false,
    val rconPassword: String = "",
    val rconPort: Int = 25565,
    val rconIp: String = "",

    // 终端选项
    val terminalOption: TerminalOption = TerminalOption(),
    val eventTask: EventTask = EventTask(),
    val pingConfig: PingConfig = PingConfig()
)
