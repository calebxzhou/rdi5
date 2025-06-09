package calebxzhou.rdi.ui.component

import calebxzhou.rdi.lgr
import calebxzhou.rdi.ui.LineHeight
import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.component.editbox.REditBox
import calebxzhou.rdi.ui.drawText
import calebxzhou.rdi.ui.general.alertErr
import calebxzhou.rdi.ui.layout.RLinearLayout
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.ui.screen.RScreen
import calebxzhou.rdi.ui.width
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen


data class RFormScreenSubmitHandler(val screen: Screen, val formData: Map<String, String>) {
    operator fun get(key: String) = formData[key]!!
    fun finish() {
    }
}

class RFormScreen(title: String, val layoutBuilder: RLinearLayout.() -> Unit, var submit: suspend (RFormScreen) -> Unit = {}) :
    RScreen(title) {
    lateinit var layout: RLinearLayout
    //提交以后才会有值
    val formData = hashMapOf<String, String>()

    /*
        get() = */
    override fun doInit() {
        layout = RLinearLayout(this).apply {
            startY =   50
            horizontal = false
            layoutBuilder()
        }.build()
        linearLayout(this) {
            startY = UiHeight - 20
            icon("success", text = "提交") {
                onSubmit()
            }
        }
    }

    operator fun get(formDataId: String): String {
        return formData.get(formDataId)?:let {
            throw IllegalArgumentException("表单数据不可能获取不到 控件id肯定写错了")
        }
    }

    override fun doTick() {

    }

    override fun onPressEnterKey() {
        onSubmit()
    }

    fun onSubmit() {
        val allFailReasons = widgets
            .filter { it.value is REditBox }
            .map { it.value as REditBox }
            .mapNotNull { it.validate() }
        if (allFailReasons.isNotEmpty()) {
            alertErr(allFailReasons.joinToString("\n"))
            return
        }
        collectFormData()
        try {
            background.launch {
                submit(this@RFormScreen)
            }
        } catch (e: Exception) {
            alertErr(e.localizedMessage)
            e.printStackTrace()
        }
    }
    fun collectFormData(){
        formData.clear()
        widgets
            .filter { it.value is REditBox || it.value is RCheckbox }
            .map { (id, widget) ->
                id to when (widget) {
                    is REditBox -> widget.value.trim()
                    is RCheckbox -> {
                        widget.selected().toString()
                    }

                    else -> {
                        lgr.error("不支持的控件类型：${widget.javaClass.name}")
                        ""
                    }
                }
            }.forEach { formData += it }
    }

    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        widgets.forEach { (id, widget) ->
            if (widget is REditBox) {
                //无内容->在输入框前边画label
                if (widget.value.trim().isNotBlank() || widget.isFocused) {
                    guiGraphics.drawText(
                        widget.label + "：",
                        x = widget.x - 10 - widget.label.width,
                        y = widget.y + LineHeight / 2
                    )
                }
            }
        }
    }

}