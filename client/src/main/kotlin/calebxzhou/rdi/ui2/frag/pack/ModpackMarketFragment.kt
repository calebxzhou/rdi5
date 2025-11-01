package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.scrollView
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.component.ModpackCard
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.ui2.horizontal
import calebxzhou.rdi.ui2.vertical
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View

class ModpackMarketFragment: RFragment("整合包市场") {
    private val mockModpacks: List<Modpack> = listOf(
        Modpack(
            name = "星际探索银河版",
            icon = byteArrayOf(),
            info = """
                使用最新的航天科技模组探索群星，打造你的太空基地。
            """.trimIndent(),
            modloader = "Fabric",
            mcVer = "1.20.1",
            versions = listOf("1.3.0", "1.2.0", "1.1.0")
        ),
        Modpack(
            name = "原味进阶生存",
            icon = byteArrayOf(),
            info = "加入农业、工业与冒险模组，让传统生存焕然一新。",
            modloader = "Forge",
            mcVer = "1.19.2",
            versions = listOf("2.0.0", "1.5.4")
        ),
        Modpack(
            name = "暮色轻旅",
            icon = byteArrayOf(),
            info = """
                轻量化探索向整合包，适合朋友联机与冒险收集。
            """.trimIndent(),
            modloader = "Quilt",
            mcVer = "1.18.2",
            versions = listOf("0.9.1")
        )
    )

    init {
        contentViewInit = {
            vertical()
            paddingDp(8, 0, 8, 12)

            textView("精选整合包") {
                textSize = 18f
                gravity = Gravity.START
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(8f)
                }
            }

            scrollView {
                layoutParams = linearLayoutParam(PARENT, 0) {
                    weight = 1f
                }

                linearLayout {
                    vertical()
                    layoutParams = linearLayoutParam(PARENT, SELF)

                    mockModpacks.chunked(3).forEach { rowPacks ->
                        linearLayout {
                            horizontal()
                            layoutParams = linearLayoutParam(PARENT, SELF) {
                                bottomMargin = dp(12f)
                            }

                            rowPacks.forEachIndexed { index, pack ->
                                val card = ModpackCard(context, pack)
                                card.layoutParams = linearLayoutParam(0, SELF) {
                                    weight = 1f
                                    if (index > 0) leftMargin = dp(12f)
                                }
                                addView(card)
                            }

                            repeat(3 - rowPacks.size) {
                                addView(View(context).apply {
                                    layoutParams = linearLayoutParam(0, SELF) {
                                        weight = 1f
                                        if (childCount > 0) leftMargin = dp(12f)
                                    }
                                    visibility = View.INVISIBLE
                                })
                            }
                        }
                    }
                }
            }
        }

        bottomOptionsConfig ={
            "＋ 做包" colored MaterialColor.BLUE_900 with {}
            "\uF005 收藏" colored MaterialColor.YELLOW_900 with {}
        }
    }
}