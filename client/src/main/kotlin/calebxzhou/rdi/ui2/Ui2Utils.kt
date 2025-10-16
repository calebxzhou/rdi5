package calebxzhou.rdi.ui2

import calebxzhou.rdi.lgr
import calebxzhou.rdi.ui2.component.*
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.util.McWindowHandle
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.rdiAsset
import com.mojang.blaze3d.platform.NativeImage
import icyllis.modernui.ModernUI
import icyllis.modernui.core.Context
import icyllis.modernui.core.Core
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.fragment.FragmentManager
import icyllis.modernui.fragment.FragmentTransaction
import icyllis.modernui.graphics.Bitmap
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Image
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.mc.MuiScreen
import icyllis.modernui.mc.neoforge.MuiForgeApi
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.KeyEvent
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import org.bson.types.ObjectId
import org.lwjgl.glfw.GLFW

/**
 * calebxzhou @ 2025-07-22 21:28
 */
val mui = ModernUI.getInstance()
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

val BG_IMAGE_PATH = "bg/1.jpg"
val BG_IMAGE_MC: ResourceLocation
    get() = rdiAsset("textures/$BG_IMAGE_PATH")
val BG_IMAGE_MUI
    get()= rdiDrawable(BG_IMAGE_PATH)
fun uiThread(run: () -> Unit) {
    Core.getUiHandlerAsync().post(run)
}
/**
 * Converts density-independent pixels (dp) to actual pixels
 */
fun Context.dp(dp: Float): Int = (dp * resources.displayMetrics.density).toInt()

fun View.padding8dp() {
    setPadding(context.dp(8f), context.dp(8f), context.dp(8f), context.dp(8f))
}

fun View.paddingDp(left: Int, top: Int, right: Int, bottom: Int) {
    setPadding(context.dp(left.toFloat()), context.dp(top.toFloat()), context.dp(right.toFloat()), context.dp(bottom.toFloat()))
}

fun View.paddingDp(all: Int) {
    paddingDp(all, all, all, all)
}
//frag的上下文
val Fragment.fctx: Context
    get() = ModernUI.getInstance()//requireContext()

operator fun ViewGroup.plusAssign(view: View) {
    addView(view)
}
fun rdiDrawable(path: String) = ImageDrawable("rdi", path)
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
fun View.toast(msg: String, duration: Int = 2000)=uiThread {
    Toast.makeText(context,msg, duration).show()
}
fun toast( msg: String, duration: Int = 2000)= uiThread {
    Toast.makeText(mui,msg, duration).show()
}
fun View.onPressEnterKey(handler: () -> Unit) {
    setOnKeyListener { _, keyCode, event ->
        if ((keyCode == KeyEvent.KEY_ENTER || keyCode == KeyEvent.KEY_KP_ENTER) && event.action == KeyEvent.ACTION_UP) {
            handler()
            true
        } else {
            false
        }
    }
}
val Fragment.mcScreen: Screen
    get() = MuiForgeApi.get().createScreen(this,null,mc.screen)
val Minecraft.fragment
    get() = (mc.screen as? MuiScreen)?.fragment as? RFragment

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


fun ViewGroup.button(
    msg: String,
    color: MaterialColor = MaterialColor.WHITE,
    width: Int = 100,
    init: RButton.() -> Unit = {},
    onClick: (RButton) -> Unit = {},
) = RButton(this.context,color,onClick = onClick).apply{
    layoutParams = linearLayoutParam(context.dp(width.toFloat()), SELF)
    text=msg

    init()
}.also { this += it }

fun ViewGroup.editText(
    msg: String = "",
    width: Int = 200,
    init: RTextField.() -> Unit = {}
) = RTextField(this.context, msg, width.toFloat()).apply{
    init()
    padding8dp()

}.also { this += it }
fun ViewGroup.checkBox(
    msg: String = "",
    init: CheckBox.() -> Unit = {},
    onClick: (CheckBox, Boolean) -> Unit = {_,_->}
) = CheckBox(this.context).apply{
    text=msg
    setOnCheckedChangeListener { v,chk->
        onClick(v as CheckBox, chk)
    }
    init()
}.also { this += it }

fun ViewGroup.editPwd(
    msg: String = "",
    width: Float = 200f,
    init: REditPassword.() -> Unit = {}
) = REditPassword(this.context, msg, width).apply(init).also { this += it }

fun ViewGroup.headButton(
    id: ObjectId,
    init: RAvatarView.() -> Unit = {},
    onClick: (RAvatarView) -> Unit = {},
) = RAvatarView(context, id, onClick).apply(init).also { this += it }


fun ViewGroup.radioButton(
    msg:String,
    init: RadioButton.() -> Unit = {},
) = RadioButton(context,).apply{
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
fun View.center(){
    layoutParams = when (this) {
        is LinearLayout -> {
            linearLayoutParam(SELF, SELF) {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        is FrameLayout -> {
            frameLayoutParam(SELF, SELF) {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }

        else -> {
            lgr.warn("尝试默认居中 $this")
            linearLayoutParam(SELF, SELF) {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
    }
}
fun TextView.leadingIcon(icon: String){
    compoundDrawablePadding = dp(8f)
    setCompoundDrawables(iconDrawable(icon).apply { setBounds(0,0,dp(24f),dp(24f)) },null,null,null)
}
fun LinearLayout.horizontal() = apply { orientation = LinearLayout.HORIZONTAL }
fun LinearLayout.vertical() = apply { orientation = LinearLayout.VERTICAL }
fun drawable(drawing: Drawable.(Canvas) -> Unit): Drawable = object : Drawable() {
    override fun draw(canvas: Canvas) {
        drawing(canvas)
    }
}
fun FragmentManager.transaction(handler: FragmentTransaction.() -> Unit){
    beginTransaction().apply { handler() }.commit()
}
val layoutInflater = object : LayoutInflater(){}
val WINDOW_HANDLE = if(isMcStarted)McWindowHandle else ModernUI.getInstance().window.handle
fun copyToClipboard(s: String) {
    GLFW.glfwSetClipboardString(WINDOW_HANDLE, s)
}
fun <T> Result<T>.failAlertPrint(): Result<T> {
     return onFailure { alertErr(it.toString());it.printStackTrace() }
}
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
