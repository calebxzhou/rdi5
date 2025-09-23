package calebxzhou.rdi.ihq.model

data class Kube(
    val assets: List<Asset>,

) {
    data class Asset(
        val path: String,
        val data: ByteArray
    )
}