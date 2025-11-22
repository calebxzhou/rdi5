package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.World
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.center
import calebxzhou.rdi.ui2.horizontal
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.radioButton
import calebxzhou.rdi.ui2.radioGroup
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import icyllis.modernui.view.View.GONE
import org.bson.types.ObjectId

class HostCreateFragment(val modpackId: ObjectId, val modpackName: String, val packVer: String,val skyblock: Boolean) : RFragment("创建服务器"){
    override var fragSize = FragmentSize.SMALL
    private var worlds: List<World> = emptyList()
    private var selectedWorldIndex: Int = 0
    private var difficulty: Int = 2
    private var gameMode: Int = 0
    private var levelType: String = "minecraft:normal"
    init {
        contentViewInit = {
            loadWorlds()
        }
    }
    private fun loadWorlds() {
        server.request<List<World>>(
            path = "world",
            showLoading = true,
            onOk = { response ->
                worlds = response.data!!
                uiThread {
                    val displayEntries = if (worlds.isEmpty()) {
                        arrayListOf()
                    } else {
                        worlds.map { it.name }.toMutableList()
                    }
                    displayEntries += "创建新存档"
                    contentView.apply {
                        minimumWidth = 600
                        center()
                        textView("整合包：$modpackName V$packVer")
                        linearLayout {
                            textView("选择存档")
                            // replace spinner with radio buttons
                            radioGroup {
                                horizontal()
                                displayEntries.forEachIndexed { idx, label ->
                                    radioButton(label) {
                                        id = idx
                                        isSelected = idx == selectedWorldIndex
                                    }
                                }
                                check(selectedWorldIndex)
                                setOnCheckedChangeListener { _, id ->
                                    selectedWorldIndex = id
                                }
                            }
                        }
                        linearLayout {
                            horizontal()
                            textView("难度")
                            radioGroup {
                                horizontal()
                                radioButton("和平") { id = 0 }
                                radioButton("简单") { id = 1 }
                                radioButton("普通") { id = 2 }
                                radioButton("困难") { id = 3 }
                                check(difficulty)
                                setOnCheckedChangeListener {_,id->
                                    difficulty = id
                                }
                            }
                        }
                        linearLayout {
                            textView("模式")
                            radioGroup {
                                horizontal()
                                radioButton("生存") { id = 0 }
                                radioButton("创造") { id = 1 }
                                radioButton("冒险") { id = 2 }
                                check(gameMode)
                                setOnCheckedChangeListener {_,id->
                                    gameMode = id
                                }
                            }
                        }
                        linearLayout {
                            textView("地形")
                            radioGroup {
                                horizontal()
                                if(skyblock){
                                    radioButton("空岛") { id = 2 }
                                }else{

                                    radioButton("普通") { id = 0 }
                                    radioButton("超平坦") { id = 1 }
                                }
                                check(0)
                                setOnCheckedChangeListener {_,id->
                                    levelType = when(id) {
                                        0 -> "minecraft:normal"
                                        1 -> "minecraft:flat"
                                        2 -> "skyblockbuilder:skyblock"
                                        else -> "minecraft:normal"
                                    }
                                }
                            }
                        }
                    }
                    contentView.bottomOptions {
                        "创建" colored MaterialColor.GREEN_900 with {
                            val selectedWorld = worlds.getOrNull(selectedWorldIndex)
                            val params = mutableMapOf(
                                "modpackId" to modpackId,
                                "packVer" to packVer,
                                "difficulty" to difficulty,
                                "gameMode" to gameMode,
                                "levelType" to levelType
                                )
                            selectedWorld?.let { params += ("worldId" to it._id) }
                            server.requestU("host", params= params) {
                                close()
                                toast("创建成功")
                            }
                        }
                    }
                }
            },
            onErr = {
                toast("拉取存档失败: ${it.msg}")
            }
        )
    }

}