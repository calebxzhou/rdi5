package calebxzhou.rdi.ihq.model.mcsm

import kotlinx.serialization.Serializable

@Serializable
data class InstanceInfo(
    val currentPlayers: Int = -1,
    val fileLock: Int = 0,
    val maxPlayers: Int = -1,
    val openFrpStatus: Boolean = false,
    val playersChart: List<Int> = emptyList(),
    val version: String = ""
)

@Serializable
data class ProcessInfo(
    val cpu: Double = 0.0,
    val memory: Long = 0,
    val ppid: Int = 0,
    val pid: Int = 0,
    val ctime: Long = 0,
    val elapsed: Long = 0,
    val timestamp: Long = 0
)

@Serializable
data class InstanceDetail(
    val config: InstanceConfig,
    val info: OInstanceInfo,
    val instanceUuid: String,
    val processInfo: OProcessInfo,
    val space: Long = 0,
    val started: Int = 0, // 启动次数
    val status: Int = 0   // -1 = 忙碌, 0 = 停止, 1 = 停止中, 2 = 启动中, 3 = 运行中
)

@Serializable
data class InstanceListData(
    val maxPage: Int,
    val pageSize: Int,
    val data: List<InstanceDetail>
)
