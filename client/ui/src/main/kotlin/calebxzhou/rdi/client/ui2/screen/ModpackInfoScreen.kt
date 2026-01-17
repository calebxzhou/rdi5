package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.millisToHumanDateTime
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.rdiRequest
import calebxzhou.rdi.client.net.rdiRequestU
import calebxzhou.rdi.client.service.ModpackService.startInstall
import calebxzhou.rdi.client.ui2.BottomSnakebar
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.ImageIconButton
import calebxzhou.rdi.client.ui2.MainBox
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.Space8h
import calebxzhou.rdi.client.ui2.Space8w
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.asIconText
import calebxzhou.rdi.client.ui2.comp.HeadButton
import calebxzhou.rdi.client.ui2.comp.ModCard
import calebxzhou.rdi.common.model.Mod
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.model.latest
import calebxzhou.rdi.common.service.CurseForgeService.fillCurseForgeVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import io.ktor.http.HttpMethod

/**
 * calebxzhou @ 2026-01-17 20:44
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpackInfoScreen(
    modpackId: String,
    onBack: () -> Unit,
    onOpenUpload: ((ObjectId, String) -> Unit)? = null,
    onCreateHost: ((String, String, String, Boolean) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var okMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("整合包详情") }
    var loading by remember { mutableStateOf(true) }
    var modpack by remember { mutableStateOf<Modpack.DetailVo?>(null) }
    var mods by remember { mutableStateOf<List<Mod>>(emptyList()) }
    var modsLoading by remember { mutableStateOf(false) }
    var confirmDeletePack by remember { mutableStateOf(false) }
    var confirmDeleteVersion by remember { mutableStateOf<Modpack.Version?>(null) }
    var confirmRebuildVersion by remember { mutableStateOf<Modpack.Version?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    fun reload() {
        loading = true
        errorMessage = null
        scope.rdiRequest<Modpack.DetailVo>(
            path = "modpack/$modpackId",
            onOk = { response ->
                modpack = response.data
                val versions = response.data?.versions.orEmpty()
                if (versions.isNotEmpty()) {
                    val latest = versions.latest
                    modsLoading = true
                    mods = emptyList()
                    scope.launch {
                        val loaded = withContext(Dispatchers.IO) {
                            runCatching { latest.mods.fillCurseForgeVo() }.getOrNull()
                        }
                        if (loaded != null) {
                            mods = loaded
                        } else {
                            errorMessage = "载入Mod信息失败"
                        }
                        modsLoading = false
                    }
                } else {
                    modsLoading = false
                    mods = emptyList()
                }
            },
            onErr = { errorMessage = "加载整合包信息失败: ${it.message}" },
            onDone = { loading = false }
        )
    }

    LaunchedEffect(modpackId) {
        reload()
    }
    LaunchedEffect(okMessage) {
        okMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            okMessage = null
        }
    }

    val pack = modpack
    val isAuthor = pack?.authorId == loggedAccount._id

    MainBox {
        MainColumn {
            TitleRow(title, onBack) {
                errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }

                pack?.let { pack->
                    HeadButton(pack.authorId)
                    Space8w()
                    ImageIconButton("grass_block")
                    Text(pack.mcVer.mcVer)
                    ImageIconButton(pack.modloader.name)
                }
                if (isAuthor) {
                    Space8w()
                    CircleIconButton(
                        icon = "\uF01F",
                        tooltip = "修改信息",
                        bgColor = MaterialColor.YELLOW_900.color
                    ) {   }
                    Space8w()
                    CircleIconButton(
                        icon = "\uEA81",
                        tooltip = "删除整合包",
                        bgColor = MaterialColor.RED_900.color
                    ) { confirmDeletePack = true }
                }
            }
            Space8h()

            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (!loading && pack == null) {
                Text("未找到整合包信息")
            }

            if (pack != null) {
                title="整合包 · "+pack.name
                val tabTitles = listOf(
                    "Mod列表(${pack.modCount})",
                    "简介",
                    "\uF019 下载版本(${pack.versions.size})"
                )
                TabRow(selectedTabIndex = selectedTab, backgroundColor = Color.White) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                Space8h()
                when (selectedTab) {
                    0 -> {
                        if (pack.versions.isEmpty()) {
                            Text("此整合包暂无可用版本，等待作者上传....", color = Color.Gray)
                        } else {
                            if (modsLoading) {
                                Text("正在载入${pack.modCount}个Mod的详细信息...")
                            }
                            Space8h()
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(320.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(mods, key = { it.hash }) { mod ->
                                    val card = mod.vo
                                    if (card != null) {
                                        card.ModCard()
                                    } else {
                                        Text(mod.slug)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        Text(pack.info)
                    }
                    else -> {
                        Space8h()
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pack.versions, key = { it.name }) { version ->
                                val statusText = when (version.status) {
                                    Modpack.Status.OK -> "\uF058 可用"
                                    Modpack.Status.BUILDING -> "\uEEFF 构建中"
                                    Modpack.Status.FAIL -> "\uEA87 构建失败"
                                    Modpack.Status.WAIT -> "\uE641 等待构建"
                                }
                                val statusColor = when (version.status) {
                                    Modpack.Status.OK -> MaterialColor.GREEN_700.color
                                    Modpack.Status.BUILDING -> MaterialColor.BLUE_700.color
                                    Modpack.Status.FAIL -> MaterialColor.RED_700.color
                                    Modpack.Status.WAIT -> MaterialColor.GRAY_700.color
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "V${version.name} - \uE641 ${version.time.millisToHumanDateTime} - \uF0C7${version.totalSize?.humanFileSize ?: ""}".asIconText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(statusText.asIconText, color = statusColor)
                                    Space8w()
                                    if (version.status == Modpack.Status.OK) {
                                        CircleIconButton(
                                            icon = "\uF04B",
                                            tooltip = "创建地图",
                                            bgColor = MaterialColor.GREEN_700.color
                                        ) {
                                            val skyblock = version.mods.any { it.slug == "skyblock-builder" }
                                            if (onCreateHost != null) {
                                                onCreateHost(pack._id.toHexString(), pack.name, version.name, skyblock)
                                            } else {
                                                okMessage = "暂不支持在此页面创建地图"
                                            }
                                        }
                                    }
                                    if (isAuthor) {
                                        Space8w()
                                        CircleIconButton(
                                            icon = "\uEA81",
                                            tooltip = "删除版本",
                                            bgColor = MaterialColor.RED_900.color
                                        ) { confirmDeleteVersion = version }
                                        Space8w()
                                        CircleIconButton(
                                            icon = "\uF0AD",
                                            tooltip = "重构",
                                            bgColor = MaterialColor.BLUE_700.color
                                        ) { confirmRebuildVersion = version }
                                    }
                                    if (version.status == Modpack.Status.OK) {
                                        Space8w()
                                        CircleIconButton(
                                            icon = "\uF019",
                                            tooltip = "下载整合包"
                                        ) {
                                            version.startInstall(pack.mcVer, pack.modloader, pack.name)
                                        }
                                    }
                                }
                            }
                            item {
                                if (pack.versions.isEmpty()) {
                                    Text("此整合包暂无可用版本，等待作者上传....", color = Color.Gray)
                                    Space8h()
                                    Button(onClick = {
                                        if (onOpenUpload != null) {
                                            onOpenUpload(pack._id, pack.name)
                                        } else {
                                            okMessage = "暂不支持上传新版"
                                        }
                                    }) {
                                        Text("上传新版")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        BottomSnakebar(snackbarHostState)
    }

    if (confirmDeletePack && pack != null) {
        AlertDialog(
            onDismissRequest = { confirmDeletePack = false },
            title = { Text("确认删除") },
            text = { Text("确定要永久删除整合包“${pack.name}”吗？无法恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeletePack = false
                    scope.rdiRequestU(
                        path = "modpack/${pack._id}",
                        method = HttpMethod.Delete,
                        onOk = {
                            okMessage = "删完了"
                            onBack()
                        },
                        onErr = { errorMessage = "删除失败: ${it.message}" }
                    )
                }) {
                    Text("删除", color = MaterialTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeletePack = false }) {
                    Text("取消")
                }
            }
        )
    }

    confirmDeleteVersion?.let { version ->
        AlertDialog(
            onDismissRequest = { confirmDeleteVersion = null },
            title = { Text("确认删除版本") },
            text = { Text("确定要永久删除版本 V${version.name} 吗？无法恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteVersion = null
                    scope.rdiRequestU(
                        path = "modpack/$modpackId/version/${version.name}",
                        method = HttpMethod.Delete,
                        onOk = {
                            okMessage = "删完了"
                            reload()
                        },
                        onErr = { errorMessage = "删除失败: ${it.message}" }
                    )
                }) {
                    Text("删除", color = MaterialTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteVersion = null }) {
                    Text("取消")
                }
            }
        )
    }

    confirmRebuildVersion?.let { version ->
        AlertDialog(
            onDismissRequest = { confirmRebuildVersion = null },
            title = { Text("确认重构版本") },
            text = { Text("将使用最新版rdi核心重新构建此版本，确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRebuildVersion = null
                    scope.rdiRequestU(
                        path = "modpack/$modpackId/version/${version.name}/rebuild",
                        method = HttpMethod.Post,
                        onOk = {
                            okMessage = "提交请求了 完事了发信箱告诉你"
                            reload()
                        },
                        onErr = { errorMessage = "重构失败: ${it.message}" }
                    )
                }) {
                    Text("重构")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRebuildVersion = null }) {
                    Text("取消")
                }
            }
        )
    }
}
