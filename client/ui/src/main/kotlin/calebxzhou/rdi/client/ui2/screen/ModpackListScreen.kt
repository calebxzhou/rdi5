package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.rdiRequest
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.ui2.ModpackUploadStore
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.ConfirmDialog
import calebxzhou.rdi.client.ui2.ImageIconButton
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.Space8h
import calebxzhou.rdi.client.ui2.Space8w
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.comp.ModpackCard
import calebxzhou.rdi.common.model.Mod
import calebxzhou.rdi.common.model.Task
import calebxzhou.rdi.common.service.ModService
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.model.Modpack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * calebxzhou @ 2026-01-13 18:27
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpackListScreen(
    onBack: (() -> Unit) = {},
    onOpenUpload: (() -> Unit) = {},
    onOpenTask: ((Task, Boolean, (() -> Unit)?) -> Unit) = { _, _, _ -> },
    onOpenMcVersions: (() -> Unit) = {},
    onOpenInfo: ((String) -> Unit) = {}
) {
    data class PendingDownload(
        val payload: ModpackService.UploadPayload,
        val mods: List<Mod>
    )
    val scope = rememberCoroutineScope()
    var modpacks by remember { mutableStateOf<List<Modpack.BriefVo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var onlyMine by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf<String?>(null) }
    var pendingDownload by remember { mutableStateOf<PendingDownload?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null
        rdiRequest<List<Modpack.BriefVo>>(
            path = "modpack",
            onOk = { response -> modpacks = response.data ?: emptyList() },
            onErr = { errorMessage = "加载整合包失败: ${it.message}" },
            onDone = { loading = false }
        )
    }

    val visibleModpacks = if (onlyMine) {
        modpacks.filter { it.authorId == loggedAccount._id }
    } else {
        modpacks
    }

    MainColumn {
        TitleRow("选择整合包 · 创建地图", onBack) {
            Checkbox(
                checked = onlyMine,
                onCheckedChange = { onlyMine = it }
            )
            Text("只看我的包")
            Space8w()
            ImageIconButton("bookshelf","MC版本资源管理",
                bgColor = Color.LightGray) {
                onOpenMcVersions.invoke()
            }
            Space8w()
            val allow = loggedAccount.hasMsid
            CircleIconButton(
                "\uDB80\uDFD5",
                tooltip = if(allow)"上传整合包" else "绑定微软MC账号上传整合包",
                enabled = allow,
            ) {
                val file = pickModpackFile(
                    onError = { msg -> errorMessage = msg }
                ) ?: return@CircleIconButton
                uploadProgress = "已选择文件: ${file.name}。正在读取，请稍等几秒..."
                scope.launch(Dispatchers.IO) {
                    val parsed = ModpackService.parseUploadPayload(
                        file = file,
                        onProgress = { msg ->
                            scope.launch { uploadProgress = msg }
                        },
                        onError = { msg ->
                            scope.launch { errorMessage = msg }
                        }
                    ) ?: return@launch
                    val allModsExist = parsed.payload.mods.all { mod ->
                        DL_MOD_DIR.resolve(mod.fileName).exists()
                    }
                    scope.launch {
                        if (allModsExist) {
                            ModpackUploadStore.preset = ModpackUploadStore.Preset(
                                payload = parsed.payload,
                                mods = parsed.payload.mods,
                                requireDownload = false
                            )
                            onOpenUpload.invoke()
                        } else {
                            pendingDownload = PendingDownload(payload = parsed.payload, mods = parsed.payload.mods)
                        }
                    }
                }
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

        errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }
        uploadProgress?.takeIf { it.isNotBlank() }?.let {
            Text(it)
            Space8h()
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(280.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visibleModpacks, key = { it.id.toHexString() }) { modpack ->
                modpack.ModpackCard(onClick = { onOpenInfo(modpack.id.toHexString()) })
            }
        }
        pendingDownload?.let { pending ->
            ConfirmDialog(
                title = "需要下载Mod",
                message = "必须先下载该整合包的${pending.mods.size}个Mod。现在下载吗？\n下载完成后，请重新选择此整合包。",
                onConfirm = {
                    pendingDownload = null
                    val task = ModService.downloadModsTask(pending.mods)
                    onOpenTask(task, true) {
                        ModpackUploadStore.preset = ModpackUploadStore.Preset(
                            payload = pending.payload,
                            mods = pending.mods,
                            requireDownload = false
                        )
                        onOpenUpload.invoke()
                    }
                },
                onDismiss = {
                    pendingDownload = null
                    uploadProgress = null
                }
            )
        }
    }
}

private fun pickModpackFile(
    onError: (String) -> Unit
): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "选择整合包 (ZIP / MRPACK / 已解压目录)"
        fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
        currentDirectory = File("C:/Users/${System.getProperty("user.name")}/Downloads")
        fileFilter = FileNameExtensionFilter("整合包 (*.zip, *.mrpack)", "zip", "mrpack")
        isAcceptAllFileFilterUsed = true
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    val file = chooser.selectedFile
    if (!file.exists() || !(file.isFile || file.isDirectory)) {
        onError("未找到所选文件")
        return null
    }
    return file
}
