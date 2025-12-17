package calebxzhou.rdi.util

import calebxzhou.rdi.lgr
import org.lwjgl.glfw.GLFW

/**
 * Utilities for getting display information using GLFW
 * Works in headless mode unlike AWT GraphicsEnvironment
 */
object DisplayUtils {
    
    /**
     * Get display modes using GLFW - works in headless mode
     * @return List of display mode strings in format "WIDTHxHEIGHT@REFRESHRATEHz"
     */
    fun getDisplayModes(): List<String> {
        return try {
            // Initialize GLFW if not already initialized
            if (!GLFW.glfwInit()) {
                lgr.warn("Failed to initialize GLFW for display detection")
                return emptyList()
            }
            
            val monitors = GLFW.glfwGetMonitors() ?: return emptyList()
            val displayModes = mutableListOf<String>()
            
            for (i in 0 until monitors.remaining()) {
                val monitor = monitors.get(i)
                val videoMode = GLFW.glfwGetVideoMode(monitor)
                
                videoMode?.let { mode ->
                    displayModes.add("${mode.width()}x${mode.height()}@${mode.refreshRate()}Hz")
                }
            }
            
            lgr.debug("Detected ${displayModes.size} display modes via GLFW: $displayModes")
            displayModes
        } catch (e: Exception) {
            lgr.warn("Failed to get display modes via GLFW", e)
            emptyList()
        }
    }

}
