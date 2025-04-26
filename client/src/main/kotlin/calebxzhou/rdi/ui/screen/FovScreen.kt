package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.general.HAlign
import calebxzhou.rdi.ui.layout.gridLayout
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.Font
import calebxzhou.rdi.util.mc.mcComp
import calebxzhou.rdi.util.toFixed
import net.minecraft.client.gui.GuiGraphics
import net.minecraftforge.client.gui.widget.ForgeSlider
import kotlin.math.tan

class FovScreen  : RScreen("视野") {
    override var showTitle=false
    val fovOption
        get() = mc.options.fov()
    val fovBtn= ForgeSlider(15,0,300,15, "视野范围".mcComp, "度".mcComp,1.0,175.0,fovOption.get().toDouble(),0.1,0,true )
    val foclen  get() = calculateFocalLength(fovOption.get().toDouble())
    override fun renderBackground(pGuiGraphics: GuiGraphics) {
    }

    fun calculateFocalLength(fovAngle: Double): Double {
        // Convert the FOV angle from degrees to radians
        val fovRadians = Math.toRadians(fovAngle)
        // Calculate the focal length using the formula: focal length = (sensor width / 2) / tan(fov / 2)
        val sensorWidth = 36.0 // 35mm full-frame sensor width in millimeters
        return (sensorWidth / 2) / tan(fovRadians / 2)
    }
    override fun init() {
        //val fovBtn = mc.options.fov().createButton(minecraft!!.options, 20, 0, 300)
        addWidget(fovBtn)
        gridLayout(this, hAlign = HAlign.CENTER){
            button("超超广角"){fovBtn.value=120.0}
            button("超广角"){fovBtn.value=103.0}
            button("广角"){fovBtn.value=81.0}
            button("标准"){fovBtn.value=54.0}
            button("中焦"){fovBtn.value=40.0}
            button("长焦"){fovBtn.value=24.0}
            button("超长焦"){fovBtn.value=6.0}
            button("超超长焦"){fovBtn.value=2.0}
        }
        super.init()
    }

    override fun tick() {
        if(fovOption.get().toDouble() !=fovBtn.value){
            fovOption.set(fovBtn.value.toInt())
        }
        super.tick()
    }

    override fun onClose() {
        mc go null
    }
    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.drawCenteredString(Font, foclen.toFixed(1) +"mm",40,5, WHITE)
    }
}