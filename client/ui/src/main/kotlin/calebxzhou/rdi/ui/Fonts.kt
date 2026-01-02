package calebxzhou.rdi.ui

import icyllis.modernui.text.Typeface

//字体文件名必须小写 不然mc不加载
enum class Fonts(val family: String) {
    UI("OPPO Sans 4.0"),
    CODE("JetBrains Mono"),
    ART("Smiley Sans Oblique"),
    ;
    val typeface
        get() = Typeface.getSystemFont(family)
}