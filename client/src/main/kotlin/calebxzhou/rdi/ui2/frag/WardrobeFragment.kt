package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.httpStringRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.SkinItemView
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.util.*
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.CheckBox
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.ScrollView
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
    private val server = RServer.now
    private val urlPrefix = "https://littleskin.cn"
    private var page = 1
    private var loading = false
    private var capeMode = false
    private var hasMoreData = true

    private lateinit var searchBox: RTextField
    private lateinit var capeBox: CheckBox
    private lateinit var skinContainer: LinearLayout
    private lateinit var scrollView: ScrollView

    override fun initContent() {
        contentLayout.apply {
            orientation = LinearLayout.VERTICAL

            // Search and cape toggle section
            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(8f)
                }
                gravity = Gravity.CENTER_HORIZONTAL

                searchBox = editText("搜索...", width = 240) {
                    onPressEnterKey {
                        refreshSkins()
                    }
                }
                button("导入正版", onClick = { mc go (MojangSkinFragment()) })

                capeBox = checkBox(
                    msg = "披风",
                    onClick = { box, chk ->
                        if (chk != capeMode) {
                            capeMode = chk
                        }
                    },
                )
            }

            // Skin container wrapped in ScrollView for scrolling
            scrollView = scrollView {
                skinContainer = linearLayout {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = linearLayoutParam(PARENT, SELF)
                }

                // Add scroll listener for auto-loading
                setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    if (!loading && hasMoreData) {
                        val view = getChildAt(childCount - 1) as LinearLayout
                        val diff = (view.bottom - (scrollY + height))

                        // Load more when within 200px of bottom
                        if (diff <= 200) {
                            loadMoreSkins(this)
                        }
                    }
                }
            }
        }

        refreshSkins()
    }

    private fun loadMoreSkins(view1: ScrollView) {
        if (loading || !hasMoreData) return

        loading = true
        page++

        ioScope.launch {
            val newSkins = querySkins(page, searchBox.txt.toString(), capeMode)
            if (newSkins.isNotEmpty()) {
                uiThread {
                    appendSkinWidgets(newSkins)
                    loading = false
                }
            } else {
                hasMoreData = false
                loading = false
                uiThread {
                    view1.toast("没有更多皮肤了")
                }
            }
        }
    }

    fun refreshSkins(){
        loading = true
        page = 1
        hasMoreData = true
        skinContainer.removeAllViews()
        ioScope.launch {
            querySkins(page, searchBox.txt.toString(),capeMode).let { skins ->
                if (skins.isNotEmpty()) {
                    uiThread {
                        loadSkinWidgets(skins)
                        loading = false
                    }
                } else {
                    uiThread {
                        searchBox.toast("没有找到相关皮肤")
                        loading = false
                        hasMoreData = false
                    }
                }
            }
        }
    }
    private suspend fun querySkins(page: Int, keyword: String, cape: Boolean): List<SkinData> = coroutineScope {
        val datas = mutableListOf<SkinData>()
        // Calculate the starting page number for this batch - reduce from 5 to 2 concurrent requests
        val startPage = (page - 1) * 2 + 1

        // Sequential requests with delay to avoid 429 errors
        for (subpage in 0..1) {
            val currentPage = startPage + subpage
            try {
                val response = httpStringRequest(
                    false,
                    "$urlPrefix/skinlib/list?filter=${if (cape) "cape" else "skin"}&sort=likes&page=$currentPage&keyword=${keyword}"
                )

                if (response.success) {
                    val body = response.body
                    val skinData = serdesJson.decodeFromString<ApiResponse>(body).data
                    datas.addAll(skinData)
                }

                // Add delay between requests to avoid rate limiting
                if (subpage < 1) {
                    kotlinx.coroutines.delay(300) // 300ms delay between requests
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Continue to next request even if one fails
            }
        }
        
        datas
    }

    private fun createSkinItemView(parent: LinearLayout, skin: SkinData, itemsInCurrentRow: Int, itemsPerRow: Int, marginBetweenItems: Int): SkinItemView {
        return SkinItemView(parent.context, skin).apply {
            layoutParams = linearLayoutParam(SELF, SELF).apply {
                leftMargin = if (itemsInCurrentRow == 0) 0 else marginBetweenItems / 2
                rightMargin = if (itemsInCurrentRow == itemsPerRow - 1) 0 else marginBetweenItems / 2
            }
            setOnClickListener {
                showSkinConfirmDialog(skin)
            }
        }
    }

    private fun addSkinsToGrid(skins: List<SkinData>, startingRow: LinearLayout? = null, startingItemCount: Int = 0) {
        // Calculate items per row based on screen width
        val screenWidth = context.resources.displayMetrics.widthPixels
        val skinItemWidth = context.dp(150f)
        val marginBetweenItems = context.dp(8f)
        val itemsPerRow = maxOf(1, (screenWidth - marginBetweenItems) / (skinItemWidth + marginBetweenItems))

        var currentRow = startingRow
        var itemsInCurrentRow = startingItemCount

        skins.forEach { skin ->
            if (currentRow == null || itemsInCurrentRow >= itemsPerRow) {
                currentRow = skinContainer.linearLayout {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = linearLayoutParam(PARENT, SELF) {
                        bottomMargin = dp(8f)
                    }
                }
                itemsInCurrentRow = 0
            }

            currentRow.let { row ->
                val skinItem = createSkinItemView(row, skin, itemsInCurrentRow, itemsPerRow, marginBetweenItems)
                row.addView(skinItem)
                itemsInCurrentRow++
            }
        }
    }

    private fun loadSkinWidgets(skins: List<SkinData>) {
        skinContainer.removeAllViews()
        addSkinsToGrid(skins)
    }

    private fun appendSkinWidgets(skins: List<SkinData>) {
        // Calculate items per row based on screen width
        val screenWidth = context.resources.displayMetrics.widthPixels
        val skinItemWidth = context.dp(150f)
        val marginBetweenItems = context.dp(8f)
        val itemsPerRow = maxOf(1, (screenWidth - marginBetweenItems) / (skinItemWidth + marginBetweenItems))

        // Get the last row if it exists and has space
        var startingRow: LinearLayout? = null
        var startingItemCount = 0

        if (skinContainer.childCount > 0) {
            val lastChild = skinContainer.getChildAt(skinContainer.childCount - 1)
            if (lastChild is LinearLayout) {
                val childCount = lastChild.childCount
                if (childCount < itemsPerRow) {
                    startingRow = lastChild
                    startingItemCount = childCount
                }
            }
        }

        addSkinsToGrid(skins, startingRow, startingItemCount)
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
                    searchBox.toast("设置皮肤失败: ${e.message}")
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
                    goto(ProfileFragment())
                    alertOk("皮肤设置成功 （半小时或重启后可见）")
                }
            } else {
                uiThread {
                    alertErr("皮肤设置失败,${response.body} ")
                }
            }
        }
    }

    private fun showMojangSkinDialog() {
        goto(MojangSkinFragment())
    }
}
