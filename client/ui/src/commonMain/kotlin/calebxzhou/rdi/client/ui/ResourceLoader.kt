package calebxzhou.rdi.client.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Load an icon image from the common resources.
 * @param icon icon name without extension (e.g. "modpack", "host")
 */
expect fun iconBitmap(icon: String): ImageBitmap

/**
 * Load raw bytes from a classpath resource.
 * @param path resource path (e.g. "assets/icons/modpack.png")
 */
expect fun loadResourceBytes(path: String): ByteArray

/**
 * Load an image from a classpath resource as ImageBitmap.
 * @param path resource path (e.g. "assets/icons/modpack.png")
 */
expect fun loadResourceBitmap(path: String): ImageBitmap
