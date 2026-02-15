package calebxzhou.rdi.client.service

import org.lwjgl.glfw.GLFW.*

actual fun getDisplayModes(): List<String> {
    // Initialize GLFW if not already initialized
    if (!glfwInit()) {
        return emptyList()
    }

    val monitors = glfwGetMonitors() ?: return emptyList()
    val modesList = mutableListOf<String>()

    for (i in 0 until monitors.limit()) {
        val monitor = monitors.get(i)
        val vidModes = glfwGetVideoModes(monitor)
        if (vidModes != null) {
            for (j in 0 until vidModes.limit()) {
                val mode = vidModes.get(j)
                // Format: WidthxHeight@RefreshRate
                modesList.add("${mode.width()}x${mode.height()}@${mode.refreshRate()}")
            }
        }
    }
    // Return unique modes
    return modesList.distinct()
}
