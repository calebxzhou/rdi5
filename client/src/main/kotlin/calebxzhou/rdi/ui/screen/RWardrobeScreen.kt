package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.auth.MojangApi
import calebxzhou.rdi.auth.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.serdes.serdesJson
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.ui.component.*
import calebxzhou.rdi.ui.component.button.RButton
import calebxzhou.rdi.ui.component.editbox.REditBox
import calebxzhou.rdi.ui.general.*
import calebxzhou.rdi.ui.justify
import calebxzhou.rdi.ui.layout.gridLayout
import calebxzhou.rdi.util.*
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.minecraft.client.gui.GuiGraphics


@Serializable
data class ApiResponse(
    val current_page: Int,
    val data: List<SkinData>
)

@Serializable
data class Skin(
    val tid: Int,
    val name: String,
    val type: String,
    val hash: String,
    val size: Int,
    val uploader: Int,
    val public: Boolean,
    val upload_at: String,
    val likes: Int
)

class RWardrobeScreen(val account: RAccount, val server: RServer) : RScreen("衣柜") {
    val urlPrefix = "https://littleskin.cn"
    override var titleX = 16
    var page = 1
    var loading = false
    val searchBox = REditBox(24, "搜索皮肤").apply {
        x = 2
        y = 2
    }.also {
        it.justify()
        addWidget(it)
    }
    val capeBox = RCheckbox("披风", false, UiWidth - 50, 2).apply {

    }.also {
        addWidget(it)
    }
    var capeMode = false
    override fun init() {
        gridLayout(this, hAlign = HAlign.CENTER) {
            button("prev", text = "上一页", active = false) {
                if (page == 1) {
                    ofWidget<RButton>("prev").active = false
                } else {
                    page--
                    querySkins(page)
                }
            }
            button("mojang", text = "正版导入") {
                mc go mojangSkinScreen
            }
            button("next", text = "下一页") {
                if (page == 500) {
                    ofWidget<RButton>("next").active = false
                } else {
                    page++
                    querySkins(page)
                }
            }
        }

        querySkins(page)
        super.init()
    }

    fun unloadWidgets() {
        (0..19).forEach { removeWidget("skin${it}") }
    }

    fun querySkins(page: Int = 1, keyword: String = "") {
        if (loading) return
        title = "衣柜 第${page}页".mcComp
        loading = true
        ofWidget<RButton>("next").active = false
        ofWidget<RButton>("prev").active = false
        unloadWidgets()
        //$urlPrefix/skinlib/list?filter=skin&keyword=22&sort=likes&page=1
        background.launch {
            val resp = httpRequest(
                false,
                "$urlPrefix/skinlib/list?filter=${if (capeMode) "cape" else "skin"}&sort=likes&page=${page}&keyword=${keyword}"
            )
            val apiResponse = serdesJson.decodeFromString<ApiResponse>(resp.body)
            loadWidgets(apiResponse.data)
            ofWidget<RButton>("next").active = true
            ofWidget<RButton>("prev").active = true
            loading = false
        }

    }

    fun loadWidgets(datas: List<SkinData>) = renderThread {

        val size = 50
        var x = 0
        var y = 20
        var paddingX = 8
        datas.map { model ->
            val widg = RSkinWidget(model, x, y, size, size) {
                confirm("要设定${if (model.isCape) "披风" else "皮肤"}“${model.name}”吗？") {
                    updateCloth(model)
                }
            }
            if (x + size > UiWidth * 0.93) {
                //换行
                x = 0
                y += (size + 10)
            } else {
                x += size + paddingX
            }
            widg
        }.forEachIndexed { i, wdg -> addWidget(wdg, "skin${i}") }
    }

    private fun updateCloth(model: SkinData) {
        background.launch {
            val response = httpRequest(false, "$urlPrefix/texture/${model.tid}")
            val apiResponse = serdesJson.decodeFromString<Skin>(response.body)
            if (model.isCape) {
                account.cloth.cape = "$urlPrefix/textures/${apiResponse.hash}"
            } else {
                account.cloth.isSlim = model.isSlim
                account.cloth.skin = "$urlPrefix/textures/${apiResponse.hash}"
            }
            setCloth(account.cloth)
        }
    }

    //todo 显示图片
    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {

        if (loading) {
            LoadingScreen.renderLoadingIcon(guiGraphics)
        } else {

        }
    }

    fun search() {
        querySkins(1, searchBox.value)
    }

    override fun tick() {
        if (searchBox.isFocused && mc pressingKey InputConstants.KEY_RETURN)
            search()
        if (capeBox.selected() != capeMode) {
            capeMode = capeBox.selected()
            querySkins(page, searchBox.value)
        }
        super.tick()
    }


    //设定服饰
    private fun setCloth(cloth: RAccount.Cloth) {
        val params = arrayListOf<Pair<String, String>>()
        params += "isSlim" to cloth.isSlim.toString()
        params += "skin" to cloth.skin.toString()
        cloth.cape?.let {

            params += "cape" to it
        }
        background.launch {
            server.hqSendAsync(true, true, "skin", params)
            account.updateCloth(cloth)
            mc go RProfileScreen()
        }
    }

    //从图床导入服饰

    //blessing skin皮肤站,通过链接获取皮肤hash从而得到图片
    /* private fun setBlessingServerSkinCape(handler: RFormScreenSubmitHandler) {
         var skin = handler.formData["skin"] ?: ""
         var cape = handler.formData["cape"] ?: ""
         val isSlim = handler.formData["slim"] == "true"
         if (skin.isNotBlank()) {
             if (!skin.isValidHttpUrl()) {
                 alertOs("皮肤链接格式错误")
                 return
             }
             if (!skin.contains("/skinlib/show/")) {
                 alertOs("仅支持blessing skin架构的皮肤站")
                 return
             }
             val url = skin.extractDomain() + "texture/" + skin.substringAfterLast('/')
             val response = HttpClients.createDefault().execute(HttpGet(url))
             val entity = response.entity
             val statusCode = response.statusLine.statusCode

             if (statusCode !in 200..299) {
                 alertOs("获取皮肤失败：$statusCode\n")
                 lgr.error("$statusCode ${EntityUtils.toString(entity)}")
                 return
             }

             val hash = Json.parseToJsonElement(EntityUtils.toString(entity).also { lgr.info(it) })
                 .jsonObject["hash"]?.jsonPrimitive?.content
                 ?: let {
                     alertOs("无法获取皮肤hash数据")
                     return
                 }

             skin = skin.extractDomain() + "textures/" + hash
         }

         if (cape.isNotBlank()) {
             if (!cape.isValidHttpUrl()) {
                 alertOs("披风链接格式错误")
                 return
             }
             if (!cape.contains("/skinlib/show/")) {
                 alertOs("仅支持blessing skin架构的皮肤站")
                 return
             }
             val url = cape.extractDomain() + "texture/" + cape.substringAfterLast('/')
             val response = HttpClients.createDefault().execute(HttpGet(url))
             val entity = response.entity
             val statusCode = response.statusLine.statusCode

             if (statusCode !in 200..299) {
                 alertOs("获取披风失败：$statusCode\n")
                 lgr.error("$statusCode ${EntityUtils.toString(entity)}")
                 return
             }

             val hash = Json.parseToJsonElement(
                 EntityUtils.toString(entity).also { lgr.info(it) }).jsonObject["hash"]?.jsonPrimitive?.content
                 ?: let {
                     alertOs("无法获取披风hash数据")
                     return
                 }
             cape = cape.extractDomain() + "textures/" + hash
         }


         setCloth(RAccount.Cloth(isSlim, skin, cape))
     }

 */

    private val mojangSkinScreen
        get() = RFormScreen(
            title = "导入正版皮肤披风",
            layoutBuilder = {
                textBox(
                    id = "name",
                    label = "正版玩家名",
                    length = 16
                )
                checkBox(
                    id = "skin",
                    label = "导入皮肤"
                )
                checkBox(
                    id = "cape",
                    label = "导入披风"
                )
            },
            submit = {
                val name = it["name"]
                val importSkin = it["skin"] == "true"
                //todo 披风没有 不应该设置空字符串
                val importCape = it["cape"] == "true"
                if (!importSkin && !importCape) {
                    alert("请选择皮肤或披风")
                    return@RFormScreen
                }
                background.launch {

                    MojangApi.getUuidFromName(name)?.let { uuid ->
                        MojangApi.getCloth(uuid)?.let { cloth ->
                            setCloth(cloth)
                        } ?: alertErr("没有读取到${name}的皮肤")
                    } ?: alertErr("玩家${name}不存在")
                }

            })


}