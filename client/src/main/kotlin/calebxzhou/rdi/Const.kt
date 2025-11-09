package calebxzhou.rdi

import org.bson.types.ObjectId

object Const {

    const val MODID = "rdi"
    //是否为调试模式,本地用
    @JvmStatic
    var DEBUG = System.getProperty("rdi.debug").toBoolean()
    var USE_MOCK_DATA = System.getProperty("rdi.mockData").toBoolean()
    //版本号与协议号
    const val VERSION = 0x500
    const val IHQ_VERSION = 0x500
    val SEED = 1145141919810L
    val DEFAULT_MODPACK_ID = ObjectId("abcdefabcdefabcdefabcdef")
    //显示版本
    const val VERSION_NUMBER = "5.5.1"

    val CF_AKEY = byteArrayOf(
        36, 50, 97, 36, 49, 48, 36, 55, 87, 87, 86, 49, 87, 69, 76, 99, 119, 88, 56, 88,
        112, 55, 100, 54, 56, 77, 72, 115, 46, 53, 103, 114, 84, 121, 90, 86, 97, 54,
        83, 121, 110, 121, 101, 83, 121, 77, 104, 49, 114, 115, 69, 56, 57, 110, 73,
        97, 48, 57, 122, 79
    ).let { String(it) }
    @JvmStatic
    val VERSION_DISP = "RDI ${if(DEBUG )"DEBUG" else ""} $VERSION_NUMBER"
    const val VERSION_STR = "5.0"

}
