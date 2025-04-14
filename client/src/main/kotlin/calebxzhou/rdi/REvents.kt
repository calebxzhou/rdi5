package calebxzhou.rdi

import calebxzhou.rdi.util.WindowHandle
import calebxzhou.rdi.util.renderThread
import io.ktor.client.HttpClient
import io.ktor.client.engine.KTOR_DEFAULT_USER_AGENT
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import org.lwjgl.glfw.GLFW

@EventBusSubscriber(modid = "rdi", bus = EventBusSubscriber.Bus.MOD)
object REvents {
    @SubscribeEvent
    fun load(event: FMLClientSetupEvent) {
        lgr.info("客户端启动")
        //最大化
        renderThread {
            GLFW.glfwMaximizeWindow(WindowHandle)
        }
        HttpClient()
    }
}