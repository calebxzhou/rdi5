package calebxzhou.rdi.ui.component

import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.Font
import calebxzhou.rdi.util.mc.mcComp
import com.mojang.blaze3d.platform.Lighting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f

//物品展示控件
class RItemStackWidget(val itemStack: ItemStack, width: Int = 16, height: Int = 16) : AbstractWidget(
    0, 0,
    width, height, "".mcComp
) {
    init {
        tooltip = Tooltip.create(itemStack.hoverName, itemStack.hoverName)
    }

    override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        if (itemStack.isEmpty)
            return
        //渲染物品本身
        val bakedmodel: BakedModel = mc.itemRenderer.getModel(itemStack, null, null, 1)
        val pose = pGuiGraphics.pose()
        pose.pushPose()
        val realX = (this.x + width / 2).toFloat()
        val realY = (this.y + height / 2).toFloat()
        pose.translate(
            realX,
            realY,
            150f
        )
        try {
            pose.mulPoseMatrix(Matrix4f().scaling(1.0f, -1.0f, 1.0f))
            pose.scale(width.toFloat(), height.toFloat(), width.toFloat())
            val flag = !bakedmodel.usesBlockLight()
            if (flag) {
                Lighting.setupForFlatItems()
            }

            mc.itemRenderer.render(
                itemStack, ItemDisplayContext.GUI, false,
                pose,
                pGuiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel
            )
            pGuiGraphics.flush()
            if (flag) {
                Lighting.setupFor3DItems()
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }

        pose.popPose()

        //渲染数量
        pose.pushPose()
        pose.translate(0.0f, 0.0f, 200.0f)
        pGuiGraphics.drawString(Font, "${itemStack.count}".mcComp, x + width - 10, y + height - 10, 16777215, true)
        pose.popPose()
        //真Y要往下串点,不然跟tooltip位置对不上
        this.isHovered =
            (pMouseX >= x) && (pMouseY >= realY) && (pMouseX < x + this.width) && (pMouseY < realY + this.height + 10)
        if (this.isHoveredOrFocused) {
            //高亮显示
            pGuiGraphics.fillGradient(
                RenderType.guiOverlay(),
                x,
                y,
                x + width,
                y + height,
                0x80FFFFFF.toInt(),
                0x80FFFFFF.toInt(),
                0
            )
           /* if(mc pressingKey InputConstants.KEY_R){
                //打开jei合成表
                val ingredient =  RDIJeiPlugin.jeiRuntime.ingredientManager.createTypedIngredient(itemStack).get()//getFocusFactory()
                val focus = RDIJeiPlugin.jeiRuntime.jeiHelpers.focusFactory.createFocus(RecipeIngredientRole.OUTPUT, ingredient)
                RDIJeiPlugin.jeiRuntime.recipesGui.show(focus)
            }*/
        }
    }

    override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        pNarrationElementOutput.add(NarratedElementType.USAGE, itemStack.hoverName)
    }


}
