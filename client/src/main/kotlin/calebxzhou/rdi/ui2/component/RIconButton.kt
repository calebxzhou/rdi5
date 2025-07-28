package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.auth.RAccountService
import calebxzhou.rdi.ui2.iconDrawable
import calebxzhou.rdi.ui2.muiImage
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Image
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.widget.Button
import kotlinx.coroutines.launch
import net.minecraft.client.resources.DefaultPlayerSkin
import org.bson.types.ObjectId

class RIconButton(
    context: Context,
    val icon: String,
    val msg: String="",
    val onClick: () -> Unit = {},
) : Button(context){
    init {
        text = msg
        paddingDp(16,8,16,8)

        // Set the icon as a compound drawable
        val drawable = iconDrawable(icon)
        drawable.setBounds(0, 0, dp(24f), dp(24f))
        setCompoundDrawablePadding(dp(8f))  // Add 8dp spacing between icon and text
        setCompoundDrawables(drawable, null, null, null)

        setOnClickListener {
            onClick()
        }
    }

}