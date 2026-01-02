package calebxzhou.rdi.ui.frag

import calebxzhou.mykotutils.std.millisToHumanDateTime
import calebxzhou.rdi.Const
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.World
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.alertOk
import calebxzhou.rdi.ui.component.alertWarn
import org.bson.types.ObjectId

class HostCreateFragment(val modpackId: ObjectId, val modpackName: String, val packVer: String, val skyblock: Boolean) :
    RFragment("创建服务器") {
    override var fragSize = FragmentSize.MEDIUM
    private var selectedWorldIndex: Int = 0
    private var difficulty: Int = 2
    private var gameMode: Int = 0
    private var levelType: String = "minecraft:normal"
    private val idCreateNewSave = 100
    private val idNoSave = 101
    private val overrideGameRules = mutableMapOf<String, String>()
    init {
        contentViewInit = {
            server.request<List<World>>(
                path = "world",
                showLoading = true,
                onOk = { loadWorlds(it.data!!) }
            )
        }
    }

    private fun loadWorlds(worlds: List<World>) = uiThread {

        contentView.apply {
            minimumWidth = 600
            center()
            textView("整合包：$modpackName V$packVer")
            linearLayout {
                textView("选择存档")
                // replace spinner with radio buttons
                radioGroup {
                    worlds.map { "${it.name} (${(it._id.timestamp * 1000L).millisToHumanDateTime})" }
                        .forEachIndexed { idx, label ->
                            radioButton(label) {
                                id = idx
                                isSelected = idx == selectedWorldIndex
                            }


                        }
                    if(worlds.size<5){

                        radioButton("创建新存档") {
                            id = idCreateNewSave
                        }
                    }
                    radioButton("不存档") {
                        id = idNoSave
                        setOnClickListener {
                            if(!Const.DEBUG)
                            alertWarn("如果不用存档，所有的地图、背包内容都不保存，\n关服后自动删除，仅供测试使用。\n谨慎选择！")
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
                    setOnCheckedChangeListener { _, id ->
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
                    setOnCheckedChangeListener { _, id ->
                        gameMode = id
                    }
                }
            }
            linearLayout {
                textView("地形")
                radioGroup {
                    horizontal()
                    if (skyblock) {
                        radioButton("空岛") { id = 2 }
                    } else {

                        radioButton("普通") { id = 0 }
                        radioButton("超平坦") { id = 1 }
                    }
                    check(0)
                    setOnCheckedChangeListener { _, id ->
                        levelType = when (id) {
                            0 -> "minecraft:normal"
                            1 -> "minecraft:flat"
                            2 -> "skyblockbuilder:skyblock"
                            else -> "minecraft:normal"
                        }
                    }
                }
            }
            button("设置游戏规则"){
                GameRulesFragment(overrideGameRules).go()
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
                    "levelType" to levelType,
                    "gameRules" to overrideGameRules.json
                )
                selectedWorld?.let {
                    params += "useWorld" to it._id
                    params += "worldOpr" to "use"
                } ?: let {
                    if (selectedWorldIndex == idCreateNewSave) {
                        params += "worldOpr" to "create"
                    } else if (selectedWorldIndex == idNoSave) {
                        params += "worldOpr" to "no"
                    }
                }

                server.requestU("host", params = params) {
                    close()
                    alertOk("已提交创建请求 完成后信箱通知你")
                }
            }
        }
    }
}




