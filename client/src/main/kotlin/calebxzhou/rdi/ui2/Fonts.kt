package calebxzhou.rdi.ui2

import icyllis.modernui.text.Typeface

enum class Fonts(val family: String) {
    UI("OPPO Sans 4.0"),
    CODE("JetBrains Mono"),
    ART("Smiley Sans Oblique"),
    ;
    val typeface
        get() = Typeface.getSystemFont(family)
}