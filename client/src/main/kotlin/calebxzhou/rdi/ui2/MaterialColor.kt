package calebxzhou.rdi.ui2

import icyllis.modernui.graphics.Color

/**
 * Material Design Color Palette
 * Complete set of Material Design colors with all shades and accent colors
 * @see https://m2.material.io/design/color/the-color-system.html#tools-for-picking-colors
 */
enum class MaterialColor(val colorValue: Int) {
    // Red
    RED_50(Color.parseColor("#FFEBEE")),
    RED_100(Color.parseColor("#FFCDD2")),
    RED_200(Color.parseColor("#EF9A9A")),
    RED_300(Color.parseColor("#E57373")),
    RED_400(Color.parseColor("#EF5350")),
    RED_500(Color.parseColor("#F44336")),
    RED_600(Color.parseColor("#E53935")),
    RED_700(Color.parseColor("#D32F2F")),
    RED_800(Color.parseColor("#C62828")),
    RED_900(Color.parseColor("#B71C1C")),
    RED_A100(Color.parseColor("#FF8A80")),
    RED_A200(Color.parseColor("#FF5252")),
    RED_A400(Color.parseColor("#FF1744")),
    RED_A700(Color.parseColor("#D50000")),

    // Pink
    PINK_50(Color.parseColor("#FCE4EC")),
    PINK_100(Color.parseColor("#F8BBD0")),
    PINK_200(Color.parseColor("#F48FB1")),
    PINK_300(Color.parseColor("#F06292")),
    PINK_400(Color.parseColor("#EC407A")),
    PINK_500(Color.parseColor("#E91E63")),
    PINK_600(Color.parseColor("#D81B60")),
    PINK_700(Color.parseColor("#C2185B")),
    PINK_800(Color.parseColor("#AD1457")),
    PINK_900(Color.parseColor("#880E4F")),
    PINK_A100(Color.parseColor("#FF80AB")),
    PINK_A200(Color.parseColor("#FF4081")),
    PINK_A400(Color.parseColor("#F50057")),
    PINK_A700(Color.parseColor("#C51162")),

    // Purple
    PURPLE_50(Color.parseColor("#F3E5F5")),
    PURPLE_100(Color.parseColor("#E1BEE7")),
    PURPLE_200(Color.parseColor("#CE93D8")),
    PURPLE_300(Color.parseColor("#BA68C8")),
    PURPLE_400(Color.parseColor("#AB47BC")),
    PURPLE_500(Color.parseColor("#9C27B0")),
    PURPLE_600(Color.parseColor("#8E24AA")),
    PURPLE_700(Color.parseColor("#7B1FA2")),
    PURPLE_800(Color.parseColor("#6A1B9A")),
    PURPLE_900(Color.parseColor("#4A148C")),
    PURPLE_A100(Color.parseColor("#EA80FC")),
    PURPLE_A200(Color.parseColor("#E040FB")),
    PURPLE_A400(Color.parseColor("#D500F9")),
    PURPLE_A700(Color.parseColor("#AA00FF")),

    // Deep Purple
    DEEP_PURPLE_50(Color.parseColor("#EDE7F6")),
    DEEP_PURPLE_100(Color.parseColor("#D1C4E9")),
    DEEP_PURPLE_200(Color.parseColor("#B39DDB")),
    DEEP_PURPLE_300(Color.parseColor("#9575CD")),
    DEEP_PURPLE_400(Color.parseColor("#7E57C2")),
    DEEP_PURPLE_500(Color.parseColor("#673AB7")),
    DEEP_PURPLE_600(Color.parseColor("#5E35B1")),
    DEEP_PURPLE_700(Color.parseColor("#512DA8")),
    DEEP_PURPLE_800(Color.parseColor("#4527A0")),
    DEEP_PURPLE_900(Color.parseColor("#311B92")),
    DEEP_PURPLE_A100(Color.parseColor("#B388FF")),
    DEEP_PURPLE_A200(Color.parseColor("#7C4DFF")),
    DEEP_PURPLE_A400(Color.parseColor("#651FFF")),
    DEEP_PURPLE_A700(Color.parseColor("#6200EA")),

    // Indigo
    INDIGO_50(Color.parseColor("#E8EAF6")),
    INDIGO_100(Color.parseColor("#C5CAE9")),
    INDIGO_200(Color.parseColor("#9FA8DA")),
    INDIGO_300(Color.parseColor("#7986CB")),
    INDIGO_400(Color.parseColor("#5C6BC0")),
    INDIGO_500(Color.parseColor("#3F51B5")),
    INDIGO_600(Color.parseColor("#3949AB")),
    INDIGO_700(Color.parseColor("#303F9F")),
    INDIGO_800(Color.parseColor("#283593")),
    INDIGO_900(Color.parseColor("#1A237E")),
    INDIGO_A100(Color.parseColor("#8C9EFF")),
    INDIGO_A200(Color.parseColor("#536DFE")),
    INDIGO_A400(Color.parseColor("#3D5AFE")),
    INDIGO_A700(Color.parseColor("#304FFE")),

    // Blue
    BLUE_50(Color.parseColor("#E3F2FD")),
    BLUE_100(Color.parseColor("#BBDEFB")),
    BLUE_200(Color.parseColor("#90CAF9")),
    BLUE_300(Color.parseColor("#64B5F6")),
    BLUE_400(Color.parseColor("#42A5F5")),
    BLUE_500(Color.parseColor("#2196F3")),
    BLUE_600(Color.parseColor("#1E88E5")),
    BLUE_700(Color.parseColor("#1976D2")),
    BLUE_800(Color.parseColor("#1565C0")),
    BLUE_900(Color.parseColor("#0D47A1")),
    BLUE_A100(Color.parseColor("#82B1FF")),
    BLUE_A200(Color.parseColor("#448AFF")),
    BLUE_A400(Color.parseColor("#2979FF")),
    BLUE_A700(Color.parseColor("#2962FF")),

    // Light Blue
    LIGHT_BLUE_50(Color.parseColor("#E1F5FE")),
    LIGHT_BLUE_100(Color.parseColor("#B3E5FC")),
    LIGHT_BLUE_200(Color.parseColor("#81D4FA")),
    LIGHT_BLUE_300(Color.parseColor("#4FC3F7")),
    LIGHT_BLUE_400(Color.parseColor("#29B6F6")),
    LIGHT_BLUE_500(Color.parseColor("#03A9F4")),
    LIGHT_BLUE_600(Color.parseColor("#039BE5")),
    LIGHT_BLUE_700(Color.parseColor("#0288D1")),
    LIGHT_BLUE_800(Color.parseColor("#0277BD")),
    LIGHT_BLUE_900(Color.parseColor("#01579B")),
    LIGHT_BLUE_A100(Color.parseColor("#80D8FF")),
    LIGHT_BLUE_A200(Color.parseColor("#40C4FF")),
    LIGHT_BLUE_A400(Color.parseColor("#00B0FF")),
    LIGHT_BLUE_A700(Color.parseColor("#0091EA")),

    // Cyan
    CYAN_50(Color.parseColor("#E0F7FA")),
    CYAN_100(Color.parseColor("#B2EBF2")),
    CYAN_200(Color.parseColor("#80DEEA")),
    CYAN_300(Color.parseColor("#4DD0E1")),
    CYAN_400(Color.parseColor("#26C6DA")),
    CYAN_500(Color.parseColor("#00BCD4")),
    CYAN_600(Color.parseColor("#00ACC1")),
    CYAN_700(Color.parseColor("#0097A7")),
    CYAN_800(Color.parseColor("#00838F")),
    CYAN_900(Color.parseColor("#006064")),
    CYAN_A100(Color.parseColor("#84FFFF")),
    CYAN_A200(Color.parseColor("#18FFFF")),
    CYAN_A400(Color.parseColor("#00E5FF")),
    CYAN_A700(Color.parseColor("#00B8D4")),

    // Teal
    TEAL_50(Color.parseColor("#E0F2F1")),
    TEAL_100(Color.parseColor("#B2DFDB")),
    TEAL_200(Color.parseColor("#80CBC4")),
    TEAL_300(Color.parseColor("#4DB6AC")),
    TEAL_400(Color.parseColor("#26A69A")),
    TEAL_500(Color.parseColor("#009688")),
    TEAL_600(Color.parseColor("#00897B")),
    TEAL_700(Color.parseColor("#00796B")),
    TEAL_800(Color.parseColor("#00695C")),
    TEAL_900(Color.parseColor("#004D40")),
    TEAL_A100(Color.parseColor("#A7FFEB")),
    TEAL_A200(Color.parseColor("#64FFDA")),
    TEAL_A400(Color.parseColor("#1DE9B6")),
    TEAL_A700(Color.parseColor("#00BFA5")),

    // Green
    GREEN_50(Color.parseColor("#E8F5E9")),
    GREEN_100(Color.parseColor("#C8E6C9")),
    GREEN_200(Color.parseColor("#A5D6A7")),
    GREEN_300(Color.parseColor("#81C784")),
    GREEN_400(Color.parseColor("#66BB6A")),
    GREEN_500(Color.parseColor("#4CAF50")),
    GREEN_600(Color.parseColor("#43A047")),
    GREEN_700(Color.parseColor("#388E3C")),
    GREEN_800(Color.parseColor("#2E7D32")),
    GREEN_900(Color.parseColor("#1B5E20")),
    GREEN_A100(Color.parseColor("#B9F6CA")),
    GREEN_A200(Color.parseColor("#69F0AE")),
    GREEN_A400(Color.parseColor("#00E676")),
    GREEN_A700(Color.parseColor("#00C853")),

    // Light Green
    LIGHT_GREEN_50(Color.parseColor("#F1F8E9")),
    LIGHT_GREEN_100(Color.parseColor("#DCEDC8")),
    LIGHT_GREEN_200(Color.parseColor("#C5E1A5")),
    LIGHT_GREEN_300(Color.parseColor("#AED581")),
    LIGHT_GREEN_400(Color.parseColor("#9CCC65")),
    LIGHT_GREEN_500(Color.parseColor("#8BC34A")),
    LIGHT_GREEN_600(Color.parseColor("#7CB342")),
    LIGHT_GREEN_700(Color.parseColor("#689F38")),
    LIGHT_GREEN_800(Color.parseColor("#558B2F")),
    LIGHT_GREEN_900(Color.parseColor("#33691E")),
    LIGHT_GREEN_A100(Color.parseColor("#CCFF90")),
    LIGHT_GREEN_A200(Color.parseColor("#B2FF59")),
    LIGHT_GREEN_A400(Color.parseColor("#76FF03")),
    LIGHT_GREEN_A700(Color.parseColor("#64DD17")),

    // Lime
    LIME_50(Color.parseColor("#F9FBE7")),
    LIME_100(Color.parseColor("#F0F4C3")),
    LIME_200(Color.parseColor("#E6EE9C")),
    LIME_300(Color.parseColor("#DCE775")),
    LIME_400(Color.parseColor("#D4E157")),
    LIME_500(Color.parseColor("#CDDC39")),
    LIME_600(Color.parseColor("#C0CA33")),
    LIME_700(Color.parseColor("#AFB42B")),
    LIME_800(Color.parseColor("#9E9D24")),
    LIME_900(Color.parseColor("#827717")),
    LIME_A100(Color.parseColor("#F4FF81")),
    LIME_A200(Color.parseColor("#EEFF41")),
    LIME_A400(Color.parseColor("#C6FF00")),
    LIME_A700(Color.parseColor("#AEEA00")),

    // Yellow
    YELLOW_50(Color.parseColor("#FFFDE7")),
    YELLOW_100(Color.parseColor("#FFF9C4")),
    YELLOW_200(Color.parseColor("#FFF59D")),
    YELLOW_300(Color.parseColor("#FFF176")),
    YELLOW_400(Color.parseColor("#FFEE58")),
    YELLOW_500(Color.parseColor("#FFEB3B")),
    YELLOW_600(Color.parseColor("#FDD835")),
    YELLOW_700(Color.parseColor("#FBC02D")),
    YELLOW_800(Color.parseColor("#F9A825")),
    YELLOW_900(Color.parseColor("#F57F17")),
    YELLOW_A100(Color.parseColor("#FFFF8D")),
    YELLOW_A200(Color.parseColor("#FFFF00")),
    YELLOW_A400(Color.parseColor("#FFEA00")),
    YELLOW_A700(Color.parseColor("#FFD600")),

    // Amber
    AMBER_50(Color.parseColor("#FFF8E1")),
    AMBER_100(Color.parseColor("#FFECB3")),
    AMBER_200(Color.parseColor("#FFE082")),
    AMBER_300(Color.parseColor("#FFD54F")),
    AMBER_400(Color.parseColor("#FFCA28")),
    AMBER_500(Color.parseColor("#FFC107")),
    AMBER_600(Color.parseColor("#FFB300")),
    AMBER_700(Color.parseColor("#FFA000")),
    AMBER_800(Color.parseColor("#FF8F00")),
    AMBER_900(Color.parseColor("#FF6F00")),
    AMBER_A100(Color.parseColor("#FFE57F")),
    AMBER_A200(Color.parseColor("#FFD740")),
    AMBER_A400(Color.parseColor("#FFC400")),
    AMBER_A700(Color.parseColor("#FFAB00")),

    // Orange
    ORANGE_50(Color.parseColor("#FFF3E0")),
    ORANGE_100(Color.parseColor("#FFE0B2")),
    ORANGE_200(Color.parseColor("#FFCC80")),
    ORANGE_300(Color.parseColor("#FFB74D")),
    ORANGE_400(Color.parseColor("#FFA726")),
    ORANGE_500(Color.parseColor("#FF9800")),
    ORANGE_600(Color.parseColor("#FB8C00")),
    ORANGE_700(Color.parseColor("#F57C00")),
    ORANGE_800(Color.parseColor("#EF6C00")),
    ORANGE_900(Color.parseColor("#E65100")),
    ORANGE_A100(Color.parseColor("#FFD180")),
    ORANGE_A200(Color.parseColor("#FFAB40")),
    ORANGE_A400(Color.parseColor("#FF9100")),
    ORANGE_A700(Color.parseColor("#FF6D00")),

    // Deep Orange
    DEEP_ORANGE_50(Color.parseColor("#FBE9E7")),
    DEEP_ORANGE_100(Color.parseColor("#FFCCBC")),
    DEEP_ORANGE_200(Color.parseColor("#FFAB91")),
    DEEP_ORANGE_300(Color.parseColor("#FF8A65")),
    DEEP_ORANGE_400(Color.parseColor("#FF7043")),
    DEEP_ORANGE_500(Color.parseColor("#FF5722")),
    DEEP_ORANGE_600(Color.parseColor("#F4511E")),
    DEEP_ORANGE_700(Color.parseColor("#E64A19")),
    DEEP_ORANGE_800(Color.parseColor("#D84315")),
    DEEP_ORANGE_900(Color.parseColor("#BF360C")),
    DEEP_ORANGE_A100(Color.parseColor("#FF9E80")),
    DEEP_ORANGE_A200(Color.parseColor("#FF6E40")),
    DEEP_ORANGE_A400(Color.parseColor("#FF3D00")),
    DEEP_ORANGE_A700(Color.parseColor("#DD2C00")),

    // Brown
    BROWN_50(Color.parseColor("#EFEBE9")),
    BROWN_100(Color.parseColor("#D7CCC8")),
    BROWN_200(Color.parseColor("#BCAAA4")),
    BROWN_300(Color.parseColor("#A1887F")),
    BROWN_400(Color.parseColor("#8D6E63")),
    BROWN_500(Color.parseColor("#795548")),
    BROWN_600(Color.parseColor("#6D4C41")),
    BROWN_700(Color.parseColor("#5D4037")),
    BROWN_800(Color.parseColor("#4E342E")),
    BROWN_900(Color.parseColor("#3E2723")),

    // Gray
    GRAY_50(Color.parseColor("#FAFAFA")),
    GRAY_100(Color.parseColor("#F5F5F5")),
    GRAY_200(Color.parseColor("#EEEEEE")),
    GRAY_300(Color.parseColor("#E0E0E0")),
    GRAY_400(Color.parseColor("#BDBDBD")),
    GRAY_500(Color.parseColor("#9E9E9E")),
    GRAY_600(Color.parseColor("#757575")),
    GRAY_700(Color.parseColor("#616161")),
    GRAY_800(Color.parseColor("#424242")),
    GRAY_900(Color.parseColor("#212121")),

    // Blue Gray
    BLUE_GRAY_50(Color.parseColor("#ECEFF1")),
    BLUE_GRAY_100(Color.parseColor("#CFD8DC")),
    BLUE_GRAY_200(Color.parseColor("#B0BEC5")),
    BLUE_GRAY_300(Color.parseColor("#90A4AE")),
    BLUE_GRAY_400(Color.parseColor("#78909C")),
    BLUE_GRAY_500(Color.parseColor("#607D8B")),
    BLUE_GRAY_600(Color.parseColor("#546E7A")),
    BLUE_GRAY_700(Color.parseColor("#455A64")),
    BLUE_GRAY_800(Color.parseColor("#37474F")),
    BLUE_GRAY_900(Color.parseColor("#263238")),

    // Black & White
    BLACK(Color.parseColor("#000000")),
    WHITE(Color.parseColor("#FFFFFF"));

    companion object {
        /**
         * Get color by RGB values
         */
        fun rgb(red: Int, green: Int, blue: Int): Int {
            return Color.rgb(red, green, blue)
        }

        /**
         * Get color by ARGB values
         */
        fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
            return Color.argb(alpha, red, green, blue)
        }

        /**
         * Parse color from hex string
         */
        fun parseColor(colorString: String): Int {
            return Color.parseColor(colorString)
        }

        // Convenience getters for commonly used colors
        val PRIMARY_COLORS = mapOf(
            "RED" to RED_500,
            "PINK" to PINK_500,
            "PURPLE" to PURPLE_500,
            "DEEP_PURPLE" to DEEP_PURPLE_500,
            "INDIGO" to INDIGO_500,
            "BLUE" to BLUE_500,
            "LIGHT_BLUE" to LIGHT_BLUE_500,
            "CYAN" to CYAN_500,
            "TEAL" to TEAL_500,
            "GREEN" to GREEN_500,
            "LIGHT_GREEN" to LIGHT_GREEN_500,
            "LIME" to LIME_500,
            "YELLOW" to YELLOW_500,
            "AMBER" to AMBER_500,
            "ORANGE" to ORANGE_500,
            "DEEP_ORANGE" to DEEP_ORANGE_500,
            "BROWN" to BROWN_500,
            "GRAY" to GRAY_500,
            "BLUE_GRAY" to BLUE_GRAY_500
        )
    }
}
