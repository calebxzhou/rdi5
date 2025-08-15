package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.httpStringRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.component.SkinItemView
import calebxzhou.rdi.util.*
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.CheckBox
import icyllis.modernui.widget.LinearLayout
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

                searchBox = editText("搜索...", 100f) {
                    layoutParams = linearLayoutParam(SELF, SELF) {
                        weight = 1f
                        rightMargin = dp(16f)
                    }
                    setSingleLine(true)
                    onPressEnterKey {
                        refreshSkins()
                    }
                }

                capeBox = checkBox(
                    msg = "披风",
                    init = {
                    layoutParams = linearLayoutParam(SELF, SELF)
                    },
                    onClick = { box, chk ->
                        if (chk != capeMode) {
                            capeMode = chk
                        }
                    },
                )
            }

            // Navigation buttons
           /* linearLayout {
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
            }*/

            // Skin container wrapped in ScrollView for scrolling
            scrollView {
                skinContainer = linearLayout {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = linearLayoutParam(PARENT, SELF)
                }
            }
        }

        refreshSkins()
    }
    fun refreshSkins(){
        loading = true
        skinContainer.removeAllViews()
        ioScope.launch {
            querySkins(page, searchBox.text.toString(),capeMode).let { skins ->
                if (skins.isNotEmpty()) {
                    uiThread {
                        loadSkinWidgets(skins)
                        loading = false
                    }
                } else {
                    uiThread {
                        toast("没有找到相关皮肤")
                    }
                }
            }
        }
    }
    private suspend fun querySkins(page: Int, keyword: String, cape: Boolean): List<SkinData> = coroutineScope {
        val datas = mutableMapOf<Int, List<SkinData>>()
        // Calculate the starting page number for this batch
        val startPage = (page - 1) * 5 + 1
        
        // Use coroutineScope to wait for all requests to complete
        val responses = (0..4).map { subpage ->
            val currentPage = startPage + subpage
            async<Pair<Int, List<SkinData>>> {
                val response = httpStringRequest(
                    false,
                    "$urlPrefix/skinlib/list?filter=${if (cape) "cape" else "skin"}&sort=likes&page=$currentPage&keyword=${keyword}"
                )

                if (response.success) {
                    val body = response.body
                    val skinData = serdesJson.decodeFromString<ApiResponse>(body).data
                    subpage to skinData
                } else {
                    subpage to emptyList<SkinData>()
                }
            }
        }.awaitAll()
        
        // Add all responses to the map
        responses.forEach { (subpage, skinData) ->
            datas[subpage] = skinData
        }
        
        // Return sorted list by subpage key
        datas.entries
            .sortedBy { it.key }
            .flatMap { it.value }
    }

    private fun loadSkinWidgets(skins: List<SkinData>) {
        skinContainer.removeAllViews()

        // Calculate items per row based on screen width
        val screenWidth = context.resources.displayMetrics.widthPixels
        val skinItemWidth = context.dp(150f) // SkinItemView width
        val marginBetweenItems = context.dp(8f) // Margin between skin items
        val itemsPerRow = maxOf(1, (screenWidth - marginBetweenItems) / (skinItemWidth + marginBetweenItems))

        // Create grid layout for skins with multiple items per row
        var currentRow: LinearLayout? = null
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
                val skinItem = SkinItemView(row.context, skin).apply {
                    layoutParams = linearLayoutParam(SELF, SELF).apply {
                        leftMargin = if (itemsInCurrentRow == 0) 0 else marginBetweenItems / 2
                        rightMargin = if (itemsInCurrentRow == itemsPerRow - 1) 0 else marginBetweenItems / 2
                    }
                }
                row.addView(skinItem)
                
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
