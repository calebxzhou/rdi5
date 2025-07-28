package calebxzhou.rdi.ui2

import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.rdiAsset
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.platform.TextureUtil
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.Bitmap
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Image
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.mc.neoforge.MuiForgeApi
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.CaveVines.use

/**
 * calebxzhou @ 2025-07-22 19:43
 */
val BG_GRAY_BORDER
    get() = object : Drawable() {
    override fun draw(canvas: Canvas) {
        val paint = Paint.obtain()
        paint.setRGBA(128, 128, 128, 255)
        paint.style = Paint.Style.STROKE.ordinal
        paint.strokeWidth = 2f
        canvas.drawRect(bounds, paint)
        paint.recycle()
    }
}

val BG_IMAGE_MC = rdiAsset("textures/bg/1.png")
fun View.padding8dp() {
    setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
}

fun View.paddingDp(left: Int, top: Int, right: Int, bottom: Int) {
    setPadding(dp(left.toFloat()), dp(top.toFloat()), dp(right.toFloat()), dp(bottom.toFloat()))
}

fun View.paddingDp(all: Int) {
    paddingDp(all, all, all, all)
}
//frag的上下文
val Fragment.fctx
    get() = requireContext()

operator fun ViewGroup.plusAssign(view: View) {
    addView(view)
}
fun rdiDrawable(path: String) = ImageDrawable("rdi","${path}.png")
fun iconDrawable(filename: String) = ImageDrawable("rdi","gui/icons/${filename}.png")
val ResourceLocation.muiImage: Image
    get() = Image.createTextureFromBitmap(bitmap) ?: MissingTextureAtlasSprite.getLocation().muiImage



val ResourceLocation.bitmap: Bitmap
    get() {
        mc.resourceManager.getResourceOrThrow(this).open().use { inputStream ->
            val mcimg = NativeImage.read(inputStream)
            val bitmap = Bitmap.createBitmap(mcimg.width,mcimg.height, Bitmap.Format.RGBA_8888)
            bitmap.setPixels(mcimg.pixelsRGBA,0,64,0,0,mcimg.width,mcimg.height)
            return bitmap
        }
    }
val Fragment.mcScreen: Screen
    get() = MuiForgeApi.get().createScreen(this,null,mc.screen)