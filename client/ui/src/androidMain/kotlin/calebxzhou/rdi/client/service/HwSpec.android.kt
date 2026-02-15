package calebxzhou.rdi.client.service

import android.content.res.Resources

actual fun getDisplayModes(): List<String> {
    val metrics = Resources.getSystem().displayMetrics
    return listOf("${metrics.widthPixels}x${metrics.heightPixels}")
}
