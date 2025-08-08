package calebxzhou.rdi.ui2

import calebxzhou.rdi.ui2.component.*
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.rdiAsset
import com.mojang.blaze3d.platform.NativeImage
import icyllis.modernui.core.Context
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.Bitmap
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Image
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.material.MaterialRadioButton
import icyllis.modernui.mc.neoforge.MuiForgeApi
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2025-07-22 21:28
 */
val PARENT = LinearLayout.LayoutParams.MATCH_PARENT
val SELF = LinearLayout.LayoutParams.WRAP_CONTENT
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
fun View.toast(msg: String, duration: Int = 2000) {
    Toast.makeText(context,msg, duration).show()
}
fun Fragment.toast(msg: String, duration: Int = 2000) {
    Toast.makeText(fctx,msg, duration).show()
}
fun toast(context: Context, msg: String, duration: Int = 2000) {
    Toast.makeText(context,msg, duration).show()
}
val Fragment.mcScreen: Screen
    get() = MuiForgeApi.get().createScreen(this,null,mc.screen)
fun linearLayoutParam(
    width: Int = PARENT,
    height: Int = SELF,
    init: LinearLayout.LayoutParams.() -> Unit = {}
): LinearLayout.LayoutParams {

    return LinearLayout.LayoutParams(width, height).apply(init)
}

fun frameLayoutParam(
    width: Int = PARENT,
    height: Int = SELF,
    init: FrameLayout.LayoutParams.() -> Unit = {}
): FrameLayout.LayoutParams {
    return FrameLayout.LayoutParams(width, height).apply(init)
}

fun ViewGroup.linearLayout(
    init: LinearLayout.() -> Unit = {}
) = LinearLayout(this.context).apply(init).also { this += it }

fun ViewGroup.radioGroup(
    init: RadioGroup.() -> Unit = {}
) = RadioGroup(this.context).apply(init).also { this += it }
//底部一堆按钮
fun ViewGroup.bottomOptions(
    init: LinearLayout.() -> Unit = {}
) = linearLayout {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_HORIZONTAL
    layoutParams = frameLayoutParam(PARENT, SELF).apply {
        gravity = Gravity.BOTTOM
    }
    init()
}

fun ViewGroup.frameLayout(
    init: FrameLayout.() -> Unit = {}
) = FrameLayout(this.context).apply(init).also { this += it }

fun ViewGroup.gridLayout(
    init: GridLayout.() -> Unit = {}
) = GridLayout(this.context).apply(init).also { this += it }

fun ViewGroup.textView(
    msg: String = "",
    init: TextView.() -> Unit = {}
) = TextView(this.context).apply{
    text = msg
    init()

}.also { this += it }

fun ViewGroup.imageView(
    init: ImageView.() -> Unit = {}
) = ImageView(this.context).apply(init).also { this += it }


fun ViewGroup.textButton(
    msg: String,
    init: RTextButton.() -> Unit = {},
    onClick: (RButton) -> Unit = {},
) = RTextButton(this.context, msg, onClick).apply(init).also { this += it }

fun ViewGroup.editText(
    msg: String = "",
    width: Float = 200f,
    init: REditText.() -> Unit = {}
) = REditText(this.context, msg, width).apply(init).also { this += it }

fun ViewGroup.editPwd(
    msg: String = "",
    width: Float = 200f,
    init: REditPassword.() -> Unit = {}
) = REditPassword(this.context, msg, width).apply(init).also { this += it }

fun ViewGroup.headButton(
    id: ObjectId,
    init: RPlayerHeadButton.() -> Unit = {},
    onClick: (RButton) -> Unit = {},
) = RPlayerHeadButton(context, id, onClick).apply(init).also { this += it }


fun ViewGroup.radioButton(
    msg:String,
    init: MaterialRadioButton.() -> Unit = {},
) = MaterialRadioButton(context,).apply{
    init()
    text=msg
}.also { this += it }


fun ViewGroup.iconButton(
    icon: String,
    text: String,
    init: RIconButton.() -> Unit = {},
    onClick: (RButton) -> Unit = {},
) = RIconButton(this.context, icon, text, onClick).apply(init).also { this += it }
fun ViewGroup.scrollView(
    init: ScrollView.() -> Unit = {}
) = ScrollView(this.context).apply(init).also { this += it }
/*

fun Context.showContextMenu(anchor: View, items: List<Pair<String, () -> Unit>>, x: Float = Float.NaN, y: Float = Float.NaN) {
    val popup = PopupMenu(this, anchor)
    if (!x.isNaN() && !y.isNaN()) {
        popup.gravity = Gravity.TOP or Gravity.START
    }
    items.forEachIndexed { index, (text, action) ->
        popup.menu.add(0, index, 0, text).setOnMenuItemClickListener {
            action()
            true
        }
    }
    popup.show()
}
*/
