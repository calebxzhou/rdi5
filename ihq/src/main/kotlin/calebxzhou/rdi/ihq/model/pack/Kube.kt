package calebxzhou.rdi.ihq.model.pack


//魔改
data class Kube(
    //资源
    val assets: List<PackFile>,
    //数据包
    val data: List<PackFile>,
    //kjs配置
    val config: List<PackFile>,
    //客户端脚本
    val clientScripts: List<PackFile>,
    //启动脚本
    val startupScripts: List<PackFile>,
    //服务端脚本
    val serverScripts: List<PackFile>,
    //自定义魔改
    val customs: Map<ScriptType,List<Any>>
) {
}