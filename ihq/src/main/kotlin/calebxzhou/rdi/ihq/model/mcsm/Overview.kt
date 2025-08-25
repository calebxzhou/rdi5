package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class Overview(
    val version: String,
    val specifiedDaemonVersion: String,
    val process: ProcessInfo,
    val record: Record,
    val system: SystemInfo,
    val chart: Chart,
    val remoteCount: RemoteCount,
    val remote: List<RemoteNode>
)

@Serializable
data class ProcessInfo(
    val cpu: Int,
    val memory: Long,
    val cwd: String
)

@Serializable
data class Record(
    val logined: Int,
    val illegalAccess: Int,
    val banips: Int,
    val loginFailed: Int
)

@Serializable
data class SystemInfo(
    val user: UserInfo,
    val time: Long,
    val totalmem: Long,
    val freemem: Long,
    val type: String,
    val version: String,
    val node: String,
    val hostname: String,
    val loadavg: List<Double>,
    val platform: String,
    val release: String,
    val uptime: Double,
    val cpu: Double
)

@Serializable
data class UserInfo(
    val uid: Int,
    val gid: Int,
    val username: String,
    val homedir: String,
    val shell: String?
)

@Serializable
data class Chart(
    val system: List<ChartSystemData>,
    val request: List<ChartRequestData>
)

@Serializable
data class ChartSystemData(
    val cpu: Double,
    val mem: Double
)

@Serializable
data class ChartRequestData(
    val value: Int,
    val totalInstance: Int,
    val runningInstance: Int
)

@Serializable
data class RemoteCount(
    val available: Int,
    val total: Int
)

@Serializable
data class RemoteNode(
    val version: String,
    val process: ProcessInfo,
    val instance: InstanceInfo,
    val system: RemoteSystemInfo,
    val cpuMemChart: List<ChartSystemData>,
    val uuid: String,
    val ip: String,
    val port: Int,
    val prefix: String,
    val available: Boolean,
    val remarks: String
)

@Serializable
data class InstanceInfo(
    val running: Int,
    val total: Int
)

@Serializable
data class RemoteSystemInfo(
    val type: String,
    val hostname: String,
    val platform: String,
    val release: String,
    val uptime: Double,
    val cwd: String,
    val loadavg: List<Double>,
    val freemem: Long,
    val cpuUsage: Double,
    val memUsage: Double,
    val totalmem: Long,
    val processCpu: Int,
    val processMem: Int
)
