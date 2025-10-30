package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.model.pack.Mod
import kotlinx.serialization.Serializable

//主机里的额外mod 可删可增
@Serializable
data class ExtraMod(
    val opr:Operand,
    val mod: Mod
) {
    @Serializable
    enum class Operand{
        ADD,
        DEL,
    }
}