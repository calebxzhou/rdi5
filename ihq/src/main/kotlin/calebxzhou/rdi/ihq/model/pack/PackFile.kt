package calebxzhou.rdi.ihq.model.pack

import kotlinx.serialization.Serializable

@Serializable
data class PackFile( val path: String,
                     val data: ByteArray)
