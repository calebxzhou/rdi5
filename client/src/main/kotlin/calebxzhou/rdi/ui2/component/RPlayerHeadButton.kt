package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.auth.RAccountService
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.uiThread
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Bitmap
import icyllis.modernui.graphics.BitmapFactory
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Image
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.Button
import io.ktor.client.call.body
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import kotlin.times

class RPlayerHeadButton(
    context: Context,
    val id: ObjectId,
    val onClick: () -> Unit = {},
) : Button(context) {
    var avatar: Image = createDefaultAvatar()
        set(value) {
            field = value
            updateAvatarDrawable()
        }
    companion object{
        private const val SKIN_HEAD_U = 8
        private const val SKIN_HEAD_V = 8
        private const val SKIN_HEAD_SIZE = 8
        private const val SKIN_HAT_U = 40
        private const val SKIN_HAT_V = 8
        private const val SKIN_TEX_WIDTH = 64
        private const val SKIN_TEX_HEIGHT = 64
        /**
         * Extracts the head portion from a skin bitmap
         */
        fun getHeadFromSkin(skinBitmap: Bitmap): Image {
            // Create a bitmap for the head (including hat layer)
            val headBitmap = Bitmap.createBitmap(SKIN_HEAD_SIZE, SKIN_HEAD_SIZE, Bitmap.Format.RGBA_8888)

            // Extract base head pixels
            val headPixels = IntArray(SKIN_HEAD_SIZE * SKIN_HEAD_SIZE)
            skinBitmap.getPixels(
                headPixels, 0, SKIN_HEAD_SIZE,
                SKIN_HEAD_U, SKIN_HEAD_V,
                SKIN_HEAD_SIZE, SKIN_HEAD_SIZE
            )

            // Extract hat layer pixels
            val hatPixels = IntArray(SKIN_HEAD_SIZE * SKIN_HEAD_SIZE)
            skinBitmap.getPixels(
                hatPixels, 0, SKIN_HEAD_SIZE,
                SKIN_HAT_U, SKIN_HAT_V,
                SKIN_HEAD_SIZE, SKIN_HEAD_SIZE
            )

            // Blend hat pixels over head pixels where hat is not transparent
            for (i in headPixels.indices) {
                val hatAlpha = hatPixels[i] ushr 24 and 0xFF
                if (hatAlpha > 0) {
                    headPixels[i] = hatPixels[i]
                }
            }

            // Set the final blended pixels to the bitmap
            headBitmap.setPixels(headPixels, 0, SKIN_HEAD_SIZE, 0, 0, SKIN_HEAD_SIZE, SKIN_HEAD_SIZE)

            return Image.createTextureFromBitmap(headBitmap) ?: throw IllegalStateException("Failed to create head texture")
        }
    }
    private fun createDefaultAvatar(): Image {
        // Create a new bitmap with proper format
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Format.RGBA_8888)

        // Create Paint for drawing
        val paint = Paint()
        paint.color = 0xFFA0A0A0.toInt()

        // Draw using paint
        paint.recycle()

        return getHeadFromSkin(bitmap)
    }

    private fun updateAvatarDrawable() {
        val bounds = dp(32f)
        val drawable = ImageDrawable(avatar)
        drawable.bounds.set(0, 0, bounds, bounds)
        drawable.paint.isFilter = false  // Disable bilinear filtering to keep pixels sharp

        setCompoundDrawablePadding(dp(8f))
        setCompoundDrawables(drawable, null, null, null)
        gravity = Gravity.CENTER
        minWidth = dp(120f)
        minimumWidth = minWidth
    }

    init {
        text = "载入中..."
        paddingDp(16,8,16,8)
        updateAvatarDrawable()  // Set initial avatar

        ioScope.launch {
            try {
                val data = RAccountService.retrievePlayerInfo(id)
                val skinUrl = data.cloth.skin
                val skinResp = httpRequest(false, skinUrl)

                uiThread {
                    text = data.name
                    if (skinResp.success) {
                        // Get response bytes and convert to bitmap
                        runBlocking {
                            val responseBytes = skinResp.body<ByteArray>()
                            val bitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
                            if (bitmap != null && bitmap.width == 64 && bitmap.height == 64) {
                                avatar = getHeadFromSkin(bitmap)
                            }
                            bitmap.recycle()
                        }
                    }

                }
            } catch (e: Exception) {
                uiThread {
                    text = "加载失败"
                }
            }
        }

        setOnClickListener {
            onClick()
        }
    }

}