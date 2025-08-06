package calebxzhou.rdi.integrate.jei

import calebxzhou.rdi.lgr
import calebxzhou.rdi.util.rdiAsset
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.resources.ResourceLocation

@JeiPlugin
class RJeiPlugin : IModPlugin {
    companion object {
        var jeiRuntime: IJeiRuntime? = null
    }

    override fun getPluginUid(): ResourceLocation {
        return rdiAsset("jei_plugin")
    }

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        lgr.info("rdi-jei插件可用")
        Companion.jeiRuntime = jeiRuntime
    }

     override fun onRuntimeUnavailable() {
         lgr.info("rdi-jei插件停用")
         jeiRuntime=null
     }

}