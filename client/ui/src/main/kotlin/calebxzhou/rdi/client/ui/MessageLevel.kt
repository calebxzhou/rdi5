package calebxzhou.rdi.client.ui

/**
 * calebxzhou @ 2025-08-04 22:27
 */
enum class MessageLevel(val msg: String, val color: MaterialColor) {
    ERR("× 错误", MaterialColor.RED_900),
    WARN("⚠ 警告", MaterialColor.ORANGE_800),
    INFO("ℹ 提示", MaterialColor.BLUE_900),
    OK("√ 成功", MaterialColor.LIGHT_GREEN_900)
}