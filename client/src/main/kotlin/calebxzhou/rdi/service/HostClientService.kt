package calebxzhou.rdi.service

import calebxzhou.rdi.ui2.frag.RFragment
import org.bson.types.ObjectId

object HostClientService {
    fun play(hostId: ObjectId,port:Int,parentFragment: RFragment){
        //todo 检测整合包并启动mc
        /*//电信以外全bgp
        val bgp = LocalCredentials.read().carrier != 0
        server.request<HostStatus>("host/${hostId}/status"){
            val status = it.data
            when (status) {
                HostStatus.STARTED -> {
                    alertErr("主机正在载入中\n请稍等1~2分钟")
                    return@request
                }
                HostStatus.STOPPED -> {
                    server.requestU("host/${hostId}/start") {
                        alertOk("主机已经启动\n请稍等1~2分钟")
                        return@requestU
                    }
                }
                HostStatus.PLAYABLE -> {
                    Host.portNow = port
                    renderThread {
                        ConnectScreen.startConnecting(
                            parentFragment.mcScreen,
                            mc,
                            ServerAddress(if (bgp) server.bgpIp else server.ip, server.gamePort),
                            server.mcData(bgp),
                            false,
                            null
                        )
                    }
                }

                else -> { alertErr("主机状态未知 无法连接") }
            }
        }*/
    }
}