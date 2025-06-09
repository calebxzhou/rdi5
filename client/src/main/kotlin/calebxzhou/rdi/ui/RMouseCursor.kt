package calebxzhou.rdi.ui

import calebxzhou.rdi.RDI
import org.lwjgl.glfw.GLFW

object RMouseCursor {
    @JvmField
    var handCursor: Long = 0

    @JvmField
    var ibeamCursor: Long = 0

    @JvmField
    var arrowCursor: Long = 0
    
    init {
        handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR)
        ibeamCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR)
        arrowCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)

    }
}