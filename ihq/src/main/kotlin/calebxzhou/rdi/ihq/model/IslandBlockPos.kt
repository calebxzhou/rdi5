package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Serializable

@Serializable
data class IslandBlockPos(val data: ByteArray = byteArrayOf(0,64,0)) {
    val x: Byte
        get() = data[0]
    val y: Byte
        get() = data[1]
    val z: Byte
        get() = data[2]

    override fun toString(): String {
        return "IslandBlockPos(x=$x, y=$y, z=$z)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IslandBlockPos

        if (!data.contentEquals(other.data)) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}