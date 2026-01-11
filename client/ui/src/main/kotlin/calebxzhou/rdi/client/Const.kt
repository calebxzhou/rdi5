package calebxzhou.rdi.client

import org.bson.types.ObjectId

object Const {

    const val MODID = "rdi"
    //是否为调试模式,本地用
    @JvmStatic
    var DEBUG = System.getProperty("rdi.debug").toBoolean()
    var USE_MOCK_DATA = System.getProperty("rdi.mockData").toBoolean()
    val SEED = 1145141919810L
    val DEFAULT_MODPACK_ID = ObjectId("abcdefabcdefabcdefabcdef")
    //显示版本
    val VERSION_NUMBER: String = loadVersionNumber()


    private fun loadVersionNumber(): String {
        val manifestVersion = Const::class.java.`package`?.implementationVersion?.takeIf { it.isNotBlank() }
        return manifestVersion
            ?: System.getProperty("rdi.version")
            ?: "dev"
    }



}
