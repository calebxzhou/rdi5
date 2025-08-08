package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.MojangApi
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.httpStringRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.util.*
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.CheckBox
import icyllis.modernui.widget.LinearLayout
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val current_page: Int,
    val data: List<SkinData>
)

@Serializable
data class SkinData(
    val tid: Int,
    val name: String,
    val type: String,
    val uploader: Int,
    val public: Boolean,
    val likes: Int
) {
    val isCape: Boolean
        get() = type == "cape"
    val isSlim: Boolean
        get() = type == "steve" // Default to false, this might need adjustment based on API
}

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

class WardrobeFragment : RFragment("衣柜") {
    private val account = RAccount.now ?: RAccount.DEFAULT
    private val server = RServer.now ?: RServer.OFFICIAL_DEBUG
    private val urlPrefix = "https://littleskin.cn"
    private var page = 1
    private var loading = false
    private var capeMode = false

    private lateinit var searchBox: REditText
    private lateinit var capeBox: CheckBox
    private lateinit var skinContainer: LinearLayout

    override fun initContent() {
        contentLayout.apply {
            orientation = LinearLayout.VERTICAL

            // Search and cape toggle section
            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(16f)
                }
                gravity = Gravity.CENTER_VERTICAL

                searchBox = editText("搜索皮肤", 200f) {
                    layoutParams = linearLayoutParam(0, SELF) {
                        weight = 1f
                        rightMargin = dp(16f)
                    }
                }

                capeBox = CheckBox(fctx).apply {
                    text = "披风"
                    layoutParams = linearLayoutParam(SELF, SELF)
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != capeMode) {
                            capeMode = isChecked
                            querySkins(page, searchBox.text.toString())
                        }
                    }
                }
                this += capeBox
            }

            // Navigation buttons
            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(16f)
                }

                textButton("上一页", init = {

                }, onClick = {
                    it.isEnabled = page > 1
                    if (page > 1) {
                        page--
                        querySkins(page, searchBox.text.toString())
                    }
                })

                textButton("正版导入", init = {
                    layoutParams = linearLayoutParam(SELF, SELF) {
                        leftMargin = dp(8f)
                        rightMargin = dp(8f)
                    }

                }){showMojangSkinDialog()}

                textButton("下一页") {
                    it.isEnabled = page < 500
                        if (page < 500) {
                            page++
                            querySkins(page, searchBox.text.toString())
                        }

                }
            }

            // Skin grid container
            skinContainer = linearLayout {
                orientation = LinearLayout.VERTICAL
                layoutParams = linearLayoutParam(PARENT, PARENT)
            }
        }

        querySkins(page)
    }

    private fun querySkins(page: Int = 1, keyword: String = "") {
        if (loading) return

        title = "衣柜 第${page}页"
        loading = true

        // Clear existing skins
        skinContainer.removeAllViews()

        ioScope.launch {
            try {
                val response = httpStringRequest(
                    false,
                    "$urlPrefix/skinlib/list?filter=${if (capeMode) "cape" else "skin"}&sort=likes&page=${page}&keyword=${keyword}"
                )

                if (response.success) {
                    val body = response.body
                    val apiResponse = serdesJson.decodeFromString<ApiResponse>(body)
                    uiThread {
                        loadSkinWidgets(apiResponse.data)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uiThread {
                    toast("加载皮肤失败: ${e.message}")
                }
            } finally {
                loading = false
            }
        }
    }

    private fun loadSkinWidgets(skins: List<SkinData>) {
        skinContainer.removeAllViews()

        // Create grid layout for skins
        var currentRow: LinearLayout? = null
        val itemsPerRow = 4
        var itemsInCurrentRow = 0

        skins.forEach { skin ->
            if (itemsInCurrentRow == 0) {
                currentRow = skinContainer.linearLayout {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = linearLayoutParam(PARENT, SELF) {
                        bottomMargin = dp(8f)
                    }
                }
                itemsInCurrentRow = 0
            }

            currentRow?.let { row ->
                row.textButton(
                    skin.name, init = {
                        layoutParams = linearLayoutParam(SELF, SELF) {
                            leftMargin = dp(4f)
                            rightMargin = dp(4f)
                        }
                    },
                    onClick = {
                        showSkinConfirmDialog(skin)
                    }
                )

                itemsInCurrentRow++
                if (itemsInCurrentRow >= itemsPerRow) {
                    itemsInCurrentRow = 0
                }
            }
        }
    }

    private fun showSkinConfirmDialog(skin: SkinData) {
        // Create a simple confirmation dialog using existing pattern
        confirm("要设定${if (skin.isCape) "披风" else "皮肤"} ${skin.name}吗？") {
            updateCloth(skin)
        }
    }

    private fun updateCloth(skinData: SkinData) {
        ioScope.launch {
            try {
                val response = httpStringRequest(false, "$urlPrefix/texture/${skinData.tid}")
                if (response.success) {
                    val skin = serdesJson.decodeFromString<Skin>(response.body)
                    val newCloth = account.cloth.copy()

                    if (skinData.isCape) {
                        newCloth.cape = "$urlPrefix/textures/${skin.hash}"
                    } else {
                        newCloth.isSlim = skinData.isSlim
                        newCloth.skin = "$urlPrefix/textures/${skin.hash}"
                    }

                    setCloth(newCloth)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uiThread {
                    toast("设置皮肤失败: ${e.message}")
                }
            }
        }
    }

    private fun setCloth(cloth: RAccount.Cloth) {
        val params = mutableListOf<Pair<String, Any>>()
        params += "isSlim" to cloth.isSlim.toString()
        params += "skin" to cloth.skin
        cloth.cape?.let {
            params += "cape" to it
        }

        server.hqRequest(true, "skin", params = params) { response ->
            if (response.success) {
                account.updateCloth(cloth)
                uiThread {
                    toast("皮肤设置成功")
                    mc go ProfileFragment()
                }
            } else {
                uiThread {
                    toast("皮肤设置失败")
                }
            }
        }
    }

    private fun showMojangSkinDialog() {
        mc go MojangSkinFragment()
    }
}

class MojangSkinFragment : RFragment("导入正版皮肤") {
    private lateinit var nameInput: REditText
    private lateinit var skinCheckBox: CheckBox
    private lateinit var capeCheckBox: CheckBox

    override fun initContent() {
        contentLayout.apply {
            orientation = LinearLayout.VERTICAL

            textView {
                text = "正版玩家名"
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(8f)
                }
            }

            nameInput = editText("输入正版玩家名", 200f) {
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(16f)
                }
            }

            skinCheckBox = CheckBox(fctx).apply {
                text = "导入皮肤"
                isChecked = true
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(8f)
                }
            }
            this += skinCheckBox

            capeCheckBox = CheckBox(fctx).apply {
                text = "导入披风"
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(16f)
                }
            }
            this += capeCheckBox

            textButton("导入") {
                importMojangSkin()
            }
        }
    }

    private fun importMojangSkin() {
        val name = nameInput.text.toString().trim()
        val importSkin = skinCheckBox.isChecked
        val importCape = capeCheckBox.isChecked

        if (name.isEmpty()) {
            toast("请输入玩家名")
            return
        }

        if (!importSkin && !importCape) {
            toast("请选择皮肤或披风")
            return
        }

        ioScope.launch {
            try {
                val uuid = MojangApi.getUuidFromName(name)
                if (uuid != null) {
                    val cloth = MojangApi.getCloth(uuid)
                    if (cloth != null) {
                        val account = RAccount.now ?: return@launch
                        val server = RServer.now ?: return@launch

                        val newCloth = account.cloth.copy()
                        if (importSkin) {
                            newCloth.skin = cloth.skin
                            newCloth.isSlim = cloth.isSlim
                        }
                        if (importCape) {
                            newCloth.cape = cloth.cape
                        }

                        val params = mutableListOf<Pair<String, Any>>()
                        params += "isSlim" to newCloth.isSlim.toString()
                        params += "skin" to newCloth.skin
                        newCloth.cape?.let {
                            params += "cape" to it
                        }

                        server.hqRequest(true, "skin", params = params) { response ->
                            if (response.success) {
                                account.updateCloth(newCloth)
                                uiThread {
                                    toast("正版皮肤导入成功")
                                    close()
                                }
                            } else {
                                uiThread {
                                    toast("皮肤设置失败")
                                }
                            }
                        }
                    } else {
                        uiThread {
                            toast("没有读取到${name}的皮肤")
                        }
                    }
                } else {
                    uiThread {
                        toast("玩家${name}不存在")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uiThread {
                    toast("导入失败: ${e.message}")
                }
            }
        }
    }
}