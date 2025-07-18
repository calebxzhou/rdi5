package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.protocol.SMeJoinPacket
import io.ktor.client.statement.HttpResponse
import io.ktor.util.encodeBase64

class RServer(
    val ip: String,
    val gamePort: Int,
    val hqPort: Int
) {
    companion object {
        var now: RServer? = null
        val OFFICIAL_DEBUG = RServer(
            "127.0.0.1", 28506, 28507
        )
        val OFFICIAL_NNG = RServer(
            "rdi.calebxzhou.cn",
            28506, 28507,
        )

    }

    fun connect() {
        GameNetClient.connect(this)
        if (Const.DEBUG) {
            val account = RAccount.TESTS[System.getProperty("rdi.testAccount").toInt()]
            GameNetClient.send(SMeJoinPacket(account.qq, account.pwd))

        }
        /*chafu = Bootstrap()
            .channel(channel)
            .group(eventGroup)
            .handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(channel: Channel) {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true)
                    channel.pipeline()
                        .addLast("timeout", ReadTimeoutHandler(15))
                        .addLast("splitter", RFrameDecoder())
                        .addLast(FlowControlHandler())
                        .addLast("decoder", RPacketDecoder())
                        .addLast("packet_handler", RPacketReceiver())
                        .addLast("encoder", RPacketEncoder())
                        .addLast("prepender", RFrameEncoder())

                    //.....
                }

            })
            .connect(ip, gamePort)*/
        // Add listener to check connection status
        // mc go  RLoginScreen(this)
    }

    suspend fun prepareRequest(
        post: Boolean = false,
        path: String,
        params: List<Pair<String, Any>> = listOf(),
    ): HttpResponse {
        val fullUrl = "http://${ip}:${hqPort}/${path}"
        val headers = RAccount.now?.let {
            listOf("Authorization" to "Basic ${"${it._id}:${it.pwd}".encodeBase64()}")
        } ?: listOf()
        return httpRequest(post, fullUrl, params, headers)

    }

}