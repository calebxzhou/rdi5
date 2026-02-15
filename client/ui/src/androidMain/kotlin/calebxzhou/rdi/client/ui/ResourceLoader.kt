package calebxzhou.rdi.client.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun iconBitmap(icon: String): ImageBitmap {
    val assetManager = AndroidPlatform.appContext.assets
    val candidatePaths = listOf(
        "assets/icons/$icon.png",
        "icons/$icon.png",
        "images/icons/$icon.png",
        "images/$icon.png"
    )
    var lastError: Exception? = null

    for (candidatePath in candidatePaths) {
        try {
            assetManager.open(candidatePath).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                    ?: throw IllegalStateException("Failed to decode icon bitmap from: $candidatePath")
                return bitmap.asImageBitmap()
            }
        } catch (e: Exception) {
            lastError = e
        }
    }

    throw IllegalStateException(
        "Icon '$icon' not found in Android assets. Root assets: [${listRootAssets()}]. " +
            "Tried paths: [${candidatePaths.joinToString()}]",
        lastError
    )
}

actual fun loadResourceBytes(path: String): ByteArray {
    try {
        return AndroidPlatform.appContext.assets.open(path).use { it.readBytes() }
    } catch (e: Exception) {
        val rootAssets = listRootAssets()
        val iconsAssets = try {
            AndroidPlatform.appContext.assets.list("assets/icons")?.joinToString() ?: "null"
        } catch (ex: Exception) {
            "error: ${ex.message}"
        }

        throw IllegalStateException("Resource not found: $path. Root assets: [$rootAssets]. assets/icons: [$iconsAssets]", e)
    }
}

actual fun loadResourceBitmap(path: String): ImageBitmap {
    try {
        return AndroidPlatform.appContext.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream).asImageBitmap()
        }
    } catch (e: Exception) {
        val rootAssets = listRootAssets()
        throw IllegalStateException("Resource not found: $path. Root assets: [$rootAssets]", e)
    }
}

private fun listRootAssets(): String {
    return AndroidPlatform.appContext.assets.list("")?.joinToString() ?: "null"
}
