package calebxzhou.rdi.client.service

import java.io.File

actual fun linkOrCopyMod(source: File, target: File) {
    source.copyTo(target, overwrite = true)
}
