package calebxzhou.rdi.client.service

import calebxzhou.rdi.common.exception.ModpackException
import java.io.File
import java.nio.file.Files

actual fun linkOrCopyMod(source: File, target: File) {
    val dst = target.toPath()
    Files.deleteIfExists(dst)
    runCatching {
        Files.createSymbolicLink(dst, source.toPath())
    }.onFailure {
        throw ModpackException("无管理员权限 无法创建Mod链接")
    }
}
