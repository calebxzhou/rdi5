package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.component.editbox.REditBox
import calebxzhou.rdi.ui.layout.RLinearLayout
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.chineseName
import calebxzhou.rdi.util.mc.justify
import me.towdium.pinin.PinIn
import me.towdium.pinin.searchers.Searcher
import me.towdium.pinin.searchers.TreeSearcher
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import org.anti_ad.mc.common.vanilla.alias.ForgeRegistries

class RItemSelectScreen(val onOk: (Item) -> Unit) : RScreen("请选择物品") {
    val pinyinSearcher = TreeSearcher<Int>(Searcher.Logic.CONTAIN, PinIn())
    val allItems = ForgeRegistries.ITEMS.mapIndexed { i, item ->
        val cn = item.asItem().chineseName.string
        pinyinSearcher.put(cn, i)
        cn to item
    }
    var searchResult = arrayListOf<Block>()
    lateinit var searchResultLayout : RLinearLayout
    var prevSearchValue = ""
    lateinit var searchBox: REditBox

    override fun doInit() {
        searchBox= REditBox(24,"搜索:拼音/首拼").apply {
            x=2
            y=20
        }.also {
            it.justify()
            addWidget(it)
        }
        searchResultLayout = linearLayout(this) {
            startX=2
            startY=40
            center=false
            autoWrap=true
            allItems.take(50).forEach { (name,item) ->
                item(
                    item = item,
                    click = {onOk(item)}
                )
            }
        }
    }
    override fun doTick() {
        if (prevSearchValue != searchBox.value) {
            search()
        }
    }
    fun search(){
        searchResult.clear()
        val txt = searchBox.value
        prevSearchValue = txt
        val searchedIndexes = pinyinSearcher.search(txt).take(25)
    }
}