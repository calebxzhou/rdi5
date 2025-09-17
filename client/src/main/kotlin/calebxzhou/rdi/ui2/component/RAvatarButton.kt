package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.service.PlayerInfoCache
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.ui2.uiThread
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Bitmap
import icyllis.modernui.graphics.BitmapFactory
import icyllis.modernui.graphics.Image
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.TextView
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId

class RAvatarButton(
    context: Context,
    val uid: ObjectId,
    onClick: (RAvatarButton) -> Unit = {},
) : TextView(context) {
    var avatar: Image = createDefaultAvatar()
        set(value) {
            field = value
            updateAvatarDrawable()
        }

    companion object {
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

            return Image.createTextureFromBitmap(headBitmap)
                ?: throw IllegalStateException("Failed to create head texture")
        }

        /**
         * Processes legacy 64x32 skins to 64x64 format using ModernUI Bitmap
         */
        fun processLegacySkin(skinBitmap: Bitmap): Bitmap {
            if (skinBitmap.width == 64 && skinBitmap.height == 32) {
                // Create a new 64x64 bitmap
                val processedBitmap = Bitmap.createBitmap(64, 64, Bitmap.Format.RGBA_8888)

                // Copy the top 32x64 pixels (head and body)
                val topPixels = IntArray(64 * 32)
                skinBitmap.getPixels(topPixels, 0, 64, 0, 0, 64, 32)
                processedBitmap.setPixels(topPixels, 0, 64, 0, 0, 64, 32)

                // Fill the bottom 32x64 pixels with transparent/empty space
                val bottomPixels = IntArray(64 * 32) { 0 } // Transparent
                processedBitmap.setPixels(bottomPixels, 0, 64, 0, 32, 64, 32)

                // For legacy skins, we only need to copy the visible parts
                // The head extraction will work with just the top 32x64 portion
                // No need to copy arms/legs since they're not visible in the head

                return processedBitmap
            }

            // Return original bitmap if not legacy format
            return skinBitmap
        }
    }
 /*   override fun onCreateContextMenu(menu: ContextMenu) {
        menu.setHeaderView(this)
        menu.setHeaderTitle("test")
        menu.add("testtest")
        menu.add(Menu.NONE, 123543687, 0, "1231320").isEnabled = true
        *//*.setOnMenuItemClickListener(mOnContextMenuItemClickListener)
        .setEnabled(mTextView.canUndo())*//*
        menu.setQwertyMode(true)
        //super.onCreateContextMenu(menu)
    }*/
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
        // Smaller avatar size for a more compact look
        val bounds = dp(24f)
        val drawable = ImageDrawable(avatar)
        drawable.bounds.set(0, 0, bounds, bounds)
        drawable.paint.isFilter = false  // Disable bilinear filtering to keep pixels sharp

        setCompoundDrawablePadding(dp(6f))
        setCompoundDrawables(drawable, null, null, null)
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        minWidth = dp(120f)
    }


    private fun loadData() {
        ioScope.launch {
            try {
                val data = PlayerInfoCache[uid]
                val skinUrl = data.cloth.skin
                val skinResp = httpRequest<ByteArray>(false, skinUrl)

                uiThread {
                    text = data.name
                    if (skinResp.success) {
                        runBlocking {
                            val responseBytes = skinResp.body()
                            val bitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
                            if (bitmap != null) {
                                // Process legacy skin if needed
                                val processedBitmap = processLegacySkin(bitmap)
                                avatar = getHeadFromSkin(processedBitmap)

                                // Clean up original bitmap if it was replaced
                                if (processedBitmap != bitmap) {
                                    processedBitmap.recycle()
                                }
                                bitmap.recycle()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                uiThread {
                    text = "加载失败"
                }
            }
        }
    }

    init {
        text = "载入中..."
        paddingDp(16, 8, 16, 8)
        // Remove any default colored background; keep it visually flat/transparent
        background = null
        // Ensure text is white for better contrast
        setTextColor(0xFFFFFFFF.toInt())
        updateAvatarDrawable()
        loadData()
        isLongClickable=true
        setOnClickListener {
            onClick(this)
        }
    }


}