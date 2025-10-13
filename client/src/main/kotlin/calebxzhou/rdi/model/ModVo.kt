package calebxzhou.rdi.model

data class ModVo(
    val id: String,
    val img: ByteArray,
    val name: String,
    val nameCn: String?,
    val categories: List<String>,
    val info: String,
)
