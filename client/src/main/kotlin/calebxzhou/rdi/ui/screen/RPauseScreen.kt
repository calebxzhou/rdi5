package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.auth.RAccount
import calebxzhou.rdi.banner.Banner
import calebxzhou.rdi.lang.MultiLangStorage
import calebxzhou.rdi.mcAsync
import calebxzhou.rdi.model.Island
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.nav.OmniNavi
import calebxzhou.rdi.serdes.serdesJson
import calebxzhou.rdi.service.Mcmod
import calebxzhou.rdi.service.NetMetrics
import calebxzhou.rdi.service.RKeyBinds
import calebxzhou.rdi.tutorial.Tutorial
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.general.Icons
import calebxzhou.rdi.ui.general.Icons.draw
import calebxzhou.rdi.ui.general.alert
import calebxzhou.rdi.ui.general.alertErr
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.*
import calebxzhou.rdi.util.mc.*
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.GenericDirtMessageScreen
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

class RPauseScreen : RScreen("暂停") {
    override var showTitle = true


    val lookingBlockState = mc.player?.lookingAtBlock
    val maxCooldown = 20
    var nowCooldown = 0

    companion object {
        fun goHome() {
            mc go null
            RServer.now?.hqSendAsync(true, true, "island/home") {
                mc.addChatMessage("回岛成功")

            }
        }

        fun goSpawn() {
            mc go null
            RServer.now?.hqSendAsync(false, true, "goSpawn") {
                mc.addChatMessage("去主城成功")
            }
        }
        fun unpause(){
            if(mc.isPlayingServer){

            RServer.now?.hqSendAsync(false, true, "unpause")
            }
        }
    }

    override fun doTick() {
        if(mc pressingKey InputConstants.KEY_HOME){
            NetMetrics.export()
            mc.addChatMessage("已导出网络日志")
        }
        if (nowCooldown < maxCooldown) {
            nowCooldown++
        }
    }

    override fun onClose() {
        if (nowCooldown >= maxCooldown) {
            super.onClose()
            unpause()
        }/*else{
            alertErr("1秒后才能继续游戏 还剩${((maxCooldown- nowCooldown) / 20.0).toFixed(2)}秒")
        }*/
    }

    override fun doInit() {
        if (mc.player == null) return
        if(mc.isPlayingServer){
            RServer.now?.hqSendAsync(false, true, "pause")
        }
        holdButton(InputConstants.KEY_DELETE,60) {
            RServer.now?.hqSendAsync(false, true, "kill"){
                mc go null
            }
        }
        linearLayout(this) {
            center = false
            startX = 5
            startY = UiHeight / 3 + 20
           /* icon("rain", text = "下雨") {
                RServer.now?.hqSendAsync(true, true, "rain") {
                    mc go null
                }
            }*/
            icon("server", text = "顶服"){
                "https://play.mcmod.cn/sv20188037.html".openAsUri()
            }
            icon("heart",text="赞助"){
                mc go RSponsorScreen()
            }/*
            icon("navi", text = "方块导航") {
                //mc go BlockSelectScreen()
                alert("没开发完")
            }*/
            icon("mcmod", text = "MC百科") {
                //mc go RMcmodScreen()
                alert("鼠标放在你想搜索的物品上\n按下${RKeyBinds.MCMOD.translatedKeyMessage.string}")
            }/*
            icon("chest", text = "翻箱倒柜") {
                //mc go RMcmodScreen()
                alert("没开发完")
            }*/
            //mc百科
            //导出存档
            //合成表
        }
        linearLayout(this) {
            startY = UiHeight / 3
            startX = 5
            center = false
            RServer.now?.let { server ->
                RAccount.now?.let { account ->
                    icon(icon = "home", text = "回岛") {
                        goHome()
                    }

                    icon(
                        icon = "city",
                        text = "主城",
                        click = { goSpawn() }
                    )
                    icon("spectate", text = "旁观") {
                        mc go null
                        server.hqSendAsync(false, true, "spectator") {
                            mc.addChatMessage("已进入旁观模式,可以按三下数字1观察其他玩家.退出按H键")
                        }
                    }
                    icon("partner", text = "参观") {
                        mc go RVisitIslandScreen(server)
                    }
                    icon(icon = "island", text = "岛屿中心") {
                        server.hqSendAsync(true, false, "island/my") { resp ->
                            val island = serdesJson.decodeFromString<Island>(resp.body)
                            mc go IslandCenterScreen(account, server, island)
                        }
                    }
                }
            }

            icon("camera", text = "摄影") {
mc set RPhotographScreen()
            }
            icon("settings", text = "设置") {
                mc go RSettingsScreen(mc.options)
            }
            icon("exit", text = "退出") {
                Banner.reset()
                OmniNavi.reset()
                mc.level?.disconnect()
                if (mc.isLocalServer) {
                    mc.clearLevel(GenericDirtMessageScreen("存档中，请稍候".mcComp))
                    Tutorial.now?.quit()
                    mc.goHome()
                } else {
                    mc.clearLevel()
                    RAccount.now?.let { ac ->
                        RServer.now?.let { sv ->
                            mc go RProfileScreen()
                        }
                    } ?: let {
                        alert("账号信息为空，即将回到主页")
                        mc.goHome()
                    }
                }


            }
        }
        var y = 20
        lookingBlockState?.let { blockState ->
            val block = blockState.block
            val item = block.asItem()
            val itemStack = item.defaultInstance
            itemLangLayout(item, "eye", y)
            y += 20
        }
        mc.player?.mainHandItem?.let { handItem ->
            if (!handItem.`is`(Items.AIR)) {

                itemLangLayout(handItem.item, "hand", y)
                y += 20
            }
        }
    }

    private fun itemLangLayout(item: Item, leadingIcon: String, inY: Int) {
        val ofLangKey = { key: String, langId: String, langName: String ->
            MultiLangStorage[key]?.get(langId)?.let { langName to it }
        }
        val ll1 = linearLayout(this) {
            center = false
            startX = 5
            startY = inY
            marginY = 2
            icon(icon = leadingIcon)
            item(
                item = item,
                init = {
                    tooltip = "点击打开MC百科".mcTooltip
                }
            ) {
                mcAsync {
                    try {
                        Mcmod.search(item.id, item.chineseName.string)?.openAsUri()
                            ?: Mcmod.search(item.id, item.englishName.string)?.openAsUri()
                            ?: alert("没有在mc百科搜到${item.chineseName.string}")
                    } catch (e: Exception) {
                        alertErr("请求失败,详见日志")
                        e.printStackTrace()
                    }
                }
            }
        }
        linearLayout(this) {
            startX = ll1.totalWidth + 10
            startY = inY
            center = false
            marginX = -2
            listOf(
                "en_us" to "英",
                "zh_tw" to "繁中",
                "ja_jp" to "日",
                "ko_kr" to "韩",
                "fr_fr" to "法",
                "de_de" to "德",
                "ru_ru" to "俄",
                "it_it" to "意"
            ).mapNotNull { ofLangKey(item.descriptionId, it.first, it.second) }
                .forEach { (langName, langVal) ->
                    text(
                        comp = langVal.mcComp.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                        init = {
                            tooltip = langName.mcTooltip
                            scale = 0.7
                        }
                    )
                }
        }


    }

    //   iconButton(item = Items.NAME_TAG, text = "标签 ${blockState.tags.toArray().size}x方/${item.builtInRegistryHolder().tags().toArray().size}x物", hoverText = "方块标签/物品标签")
    override fun doRender(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        var x = 10
        var y = 40
        gg.drawText("功能菜单", x = 5, y = UiHeight / 4)
        if(nowCooldown<maxCooldown){
            Icons["lock"].draw(gg,2,2)
        }
    }


}