package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.util.chineseName
import net.minecraft.world.item.Item

class RItemSourceScreen(item: Item) : RScreen("${item.chineseName}的获取方法"){
    init {

        /*val jr = RJeiPlugin.jeiRuntime
        val focus = jr.jeiHelpers.focusFactory.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK,item.defaultInstance)
        jr.recipesGui.show(focus)*/
      //  mc.recipeManager.recipes
    }
}