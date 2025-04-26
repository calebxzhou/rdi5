package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.component.button.RItemButton
import calebxzhou.rdi.ui.component.editbox.REditBox
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.*
import calebxzhou.rdi.util.mc.*
import me.towdium.pinin.PinIn
import me.towdium.pinin.searchers.Searcher
import me.towdium.pinin.searchers.TreeSearcher
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.level.block.Block
import org.anti_ad.mc.common.vanilla.alias.ForgeRegistries

class RBlockSelectScreen(val onOk: (List<Block>)->Unit) : RScreen("选择方块") {
    val pinyinSearcher = TreeSearcher<Int>(Searcher.Logic.CONTAIN, PinIn())
    val allBlocks = ForgeRegistries.BLOCKS.mapIndexed { i, block ->
        val cn = block.asItem().chineseName.string
        pinyinSearcher.put(cn, i)
        cn to block
    }
    var selected = arrayListOf<Block>()
    var searched = arrayListOf<Block>()
    var prevSearchValue = ""
    val searchBox = REditBox(48,"拼音/首拼").apply {
        x=2
        y=20
    }.also {
        it.justify()
        addWidget(it)
    }



    override fun tick() {

        if (prevSearchValue != searchBox.value) {
           search()
        }
        super.tick()
    }
    fun search(){

        searched.clear()
        val txt = searchBox.value
        prevSearchValue = txt
        val searchedIndexes = pinyinSearcher.search(txt).take(25)
        var x = 2
        var y = 90
        searchedIndexes.map { allBlocks[it].second }.forEachIndexed { i, block ->
            val item = block.asItem()
            if (x + Font.width(item.chineseName) + 16 > UiWidth) {
                x = 2
                y += 20
            }
            val btn = RItemButton(
                item = item
            ) {
                select(block)
            }

            this += btn
            searched += block
            x += btn.width + 5

        }
    }
    fun select(block: Block){
        val item = block.asItem()
        /*val btn = RItemButton(
            item = item,
            comp = item.chineseName + "\n" + item.id.toString(),
            x=2,
            y=55,
            scale = 0.9
        ) {
            selectedBlocks.remove(block to it)
            this -= it
        }
        val lastBtn = selectedBlocks.lastOrNull()?.second
        btn.x = (lastBtn?.x?:2)+(lastBtn?.width?:0)
        this += btn
        selectedBlocks += block to btn*/
    }

    override fun doInit() {
        linearLayout (this) {
            center=true
            icon("success", text = "确认") {
                onOk(selected)
            }
            icon("reset", text = "还原") {

            }
        }

    }
    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.drawText("已选择：",x=2,y=40)
        guiGraphics.drawText("已搜到："+searched.size+"个",x=2,y=77)
        /*selectedBlocks.forEachIndexed { i,block ->
            RIconButton(
                item = block.asItem(),
                comp = block.asItem().chineseName + "\n",
                x=2,
                y=50

            ) { selectedBlocks.remove(block) }.also { it.x=2+i*it.width }
        }*/
    }
}