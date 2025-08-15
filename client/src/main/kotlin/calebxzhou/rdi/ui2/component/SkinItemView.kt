package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.frag.SkinData
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.ImageView
import icyllis.modernui.widget.LinearLayout

class SkinItemView(context: Context, val skin: SkinData) : FrameLayout(context) {
    val url = "https://littleskin.cn/preview/${skin.tid}?height=150&png"
    
    init {
        // Set layout parameters for this view - fixed size around 100x100
        layoutParams = frameLayoutParam(context.dp(150f), context.dp(150f))
        
        // Add skin image with forced uniform size
        val imageView = HttpImageView(context, url).apply {
            layoutParams = frameLayoutParam(context.dp(150f), context.dp(150f)) // Force exact size
            scaleType = ImageView.ScaleType.CENTER_CROP // Crop and center the image to fill the container
        }
        addView(imageView)
        val name = (if(skin.name.length>6) {
            skin.name.substring(0, 5) + "..." // Truncate long names
        } else {
            skin.name
        }).replace(
            //去掉特殊符号
            Regex("[^\\p{L}\\p{N}\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{InCJKUnifiedIdeographs}]"),
            ""
        )
        linearLayout {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = frameLayoutParam(PARENT, SELF) {
                gravity = Gravity.BOTTOM
            }
          //  paddingDp(8, 8, 8, 8)
            gravity = Gravity.CENTER_VERTICAL
            background = object : Drawable() {
                override fun draw(canvas: Canvas) {
                    val paint = Paint.obtain()
                    paint.setRGBA(128, 128, 128, 128)
                    paint.style = Paint.Style.FILL.ordinal
                    canvas.drawRect(bounds, paint)
                    paint.recycle()
                }
            }
            textView {
                text =  name+ " ♥️${skin.likes}"
                textSize = 15f // Smaller text for 100x100 container
                setTextColor(0xFFFFFFFF.toInt()) // White text
                layoutParams = linearLayoutParam(0, SELF) {
                    weight = 1f // Take remaining space
                    gravity = Gravity.START
                }
            }

        }
    }
    
 /*   override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Force exact size of 150x150dp
        val size = context.dp(150f)
        setMeasuredDimension(size, size)
    }*/
}