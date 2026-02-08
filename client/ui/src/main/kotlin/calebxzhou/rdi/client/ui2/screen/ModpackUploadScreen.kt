package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.service.ModpackTester
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.service.ModpackService.UploadPayload
import calebxzhou.rdi.client.service.TestStatus
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.Console
import calebxzhou.rdi.client.ui2.comp.ConsoleState
import calebxzhou.rdi.client.ui2.comp.ModCard
import calebxzhou.rdi.common.model.Mod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2026-01-17 13:53
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpackUploadScreen(
    uploadPayload: UploadPayload,
    onBack: () -> Unit,
    updateModpackId: ObjectId? = null,
    updateModpackName: String? = null
) {

    val scope = rememberCoroutineScope()
    val modsGridState = rememberLazyGridState()
    var step: UploadStep by remember(uploadPayload, updateModpackName) {
        mutableStateOf(
            UploadStep.Confirm(
                payload = uploadPayload,
                mods = uploadPayload.mods,
                name = updateModpackName ?: uploadPayload.sourceName.take(16),
                version = uploadPayload.sourceVersion
            )
        )
    }
    var progressText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var selectedModsTab by remember { mutableStateOf(0) }
    val tester = remember(uploadPayload) { ModpackTester(uploadPayload) }
    val testStatus by tester.status.collectAsState()
    val testPassSeconds by tester.passSeconds.collectAsState()
    val testedModsSignature by tester.testedModsSignature.collectAsState()
    val testConsoleState = remember(uploadPayload) { ConsoleState(4000) }

    LaunchedEffect(step) {
        errorText = null
    }

    LaunchedEffect(selectedModsTab, step) {
        if (step is UploadStep.Confirm) {
            modsGridState.scrollToItem(0)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tester.dispose(scope)
        }
    }

    MainColumn { 
        TitleRow(
            title = when (step) {
                is UploadStep.Confirm -> updateModpackName?.let { "为整合包$it 上传新版" } ?: "确认整合包信息"
                is UploadStep.Uploading -> "上传整合包"
                is UploadStep.Done -> "上传完成"
            },
            onBack = onBack
        ) {
            errorText?.let {
                Text(it, color = MaterialTheme.colors.error)
            }
            Space8w()
            when (step) {
                is UploadStep.Confirm -> {
                    CircleIconButton("\uF058", "确认上传") {
                        val current = step as UploadStep.Confirm
                        val signatureNow = tester.currentModsSignature(current.mods)
                        if (testStatus != TestStatus.PASSED || testedModsSignature != signatureNow) {
                            errorText = "请先在“运行测试”页完成测试并通过"
                            return@CircleIconButton
                        }
                        val name = current.name.trim()
                        val version = current.version.trim()
                        if (name.isBlank()) {
                            errorText = "整合包名称不能为空"
                            return@CircleIconButton
                        }
                        if (version.isBlank()) {
                            errorText = "版本号不能为空"
                            return@CircleIconButton
                        }
                        scope.launch(Dispatchers.IO) {
                            step = UploadStep.Uploading(current.payload, current.mods, name, version) 
                            ModpackService.uploadModpack(
                                payload = current.payload,
                                mods = current.mods,
                                modpackName = name,
                                versionName = version,
                                updateModpackId = updateModpackId,
                                onProgress = { progressText = it },
                                onError = { errorText = it },
                                onDone = { summary ->
                                    step = UploadStep.Done(summary)
                                }
                            )
                        }
                    }
                }

                is UploadStep.Uploading -> {
                    // no actions while uploading
                }

                is UploadStep.Done -> {
                    CircleIconButton("\uF00C", "完成") {
                        onBack()
                    }
                }
            }
        }



        when (val current = step) {
            is UploadStep.Confirm -> {
                val sortedMods = remember(current.mods) {
                    current.mods
                        .asSequence()
                        .sortedBy { it.vo?.nameCn ?: it.vo?.name ?: it.slug }
                        .toList()
                }
                Row {

                    OutlinedTextField(
                        value = current.name,
                        onValueChange = { step = current.copy(name = it) },
                        modifier = 250.wM,
                        label = { Text("整合包名称") },
                        enabled = updateModpackName == null
                    )
                    OutlinedTextField(
                        value = current.version,
                        onValueChange = { step = current.copy(version = it) },
                        modifier = 120.wM,
                        label = { Text("版本") }
                    )
                }
                Space8h()
                TabRow(selectedTabIndex = selectedModsTab,backgroundColor = Color.White) {
                    Tab(
                        selected = selectedModsTab == 0,
                        onClick = { selectedModsTab = 0 },
                        text = { Text("Mod列表(${sortedMods.size})") }
                    )
                    Tab(
                        selected = selectedModsTab == 1,
                        onClick = { selectedModsTab = 1 },
                        text = { Text("运行测试") }
                    )
                }
                Space8h()
                if (selectedModsTab == 1) {
                    val statusText = when (testStatus) {
                        TestStatus.NOT_RUN -> "未测试"
                        TestStatus.RUNNING -> "测试中"
                        TestStatus.PASSED -> "通过${testPassSeconds?.let { "（${it}s）" } ?: ""}"
                        TestStatus.FAILED -> "失败"
                        TestStatus.STOPPED -> "已停止"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("测试状态：$statusText")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircleIconButton("\uF04B", "启动测试", bgColor = MaterialColor.GREEN_900.color) {
                                if (tester.isRunning()) {
                                    errorText = "测试服务器已经在运行中"
                                    return@CircleIconButton
                                }
                                testConsoleState.clear()
                                tester.startWithAutoFix(
                                    uiScope = scope,
                                    getMods = { (step as? UploadStep.Confirm)?.mods.orEmpty() },
                                    setMods = { updatedMods ->
                                        val latest = step as? UploadStep.Confirm ?: return@startWithAutoFix
                                        step = latest.copy(mods = updatedMods)
                                    },
                                    onError = { errorText = it },
                                    appendLog = { line -> testConsoleState.append(line) }
                                )
                            }
                            CircleIconButton("\uF04D", "停止测试", bgColor = MaterialColor.RED_900.color) {
                                tester.stop(
                                    uiScope = scope,
                                    appendLog = { line -> testConsoleState.append(line) }
                                )
                            }
                        }
                    }
                    Space8h()
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)) {
                        Console(
                            state = testConsoleState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(350.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        state = modsGridState,
                    ) {
                        items(sortedMods, key = { mod -> modStableKey(mod) }) { mod ->
                            val card = mod.vo
                            if (card != null) {
                                card.ModCard(
                                    currentSide = mod.side,
                                    onSideChange = { newSide ->
                                        if (newSide == mod.side) return@ModCard
                                        val latest = step as? UploadStep.Confirm ?: return@ModCard
                                        val updatedMods = updateModSide(
                                            mods = latest.mods,
                                            index = -1,
                                            modKey = modStableKey(mod),
                                            newSide = newSide
                                        )
                                        tester.onModsChangedAfterManualEdit()
                                        step = latest.copy(mods = updatedMods)
                                    }
                                )
                            } else {
                                Text(mod.slug)
                            }
                        }
                    }
                }
            }

            is UploadStep.Uploading -> {
                Text("正在上传整合包，请耐心等待...")
                Space8h()
                if (progressText.isNotBlank()) {
                    Text(progressText)
                }
            }

            is UploadStep.Done -> {
                Text(current.summary)
            }
        }
    }
}

private sealed class UploadStep {
    data class Confirm(
        val payload: UploadPayload,
        val mods: List<Mod>,
        val name: String = payload.sourceName,
        val version: String = payload.sourceVersion
    ) : UploadStep()

    data class Uploading(
        val payload: UploadPayload,
        val mods: List<Mod>,
        val name: String,
        val version: String
    ) : UploadStep()

    data class Done(val summary: String) : UploadStep()
}

private fun updateModSide(
    mods: List<Mod>,
    index: Int,
    modKey: String,
    newSide: Mod.Side
): List<Mod> {
    if (mods.isEmpty()) return mods
    val updated = mods.toMutableList()
    val targetIndex = when {
        index in updated.indices && modStableKey(updated[index]) == modKey -> index
        else -> updated.indexOfFirst { modStableKey(it) == modKey }
    }
    if (targetIndex < 0) return mods
    val origin = updated[targetIndex]
    updated[targetIndex] = origin.copy(side = newSide).apply { vo = origin.vo }
    return updated
}

private fun modStableKey(mod: Mod): String =
    "${mod.platform}:${mod.projectId}:${mod.fileId}:${mod.hash}"
