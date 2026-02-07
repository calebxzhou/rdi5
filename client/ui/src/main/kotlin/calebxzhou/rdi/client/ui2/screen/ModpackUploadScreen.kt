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
import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.rdi.client.service.GameService
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.service.ModpackService.UploadPayload
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.Console
import calebxzhou.rdi.client.ui2.comp.ConsoleState
import calebxzhou.rdi.client.ui2.comp.ModCard
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.model.Mod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private enum class TestStatus {
    NOT_RUN, RUNNING, PASSED, FAILED, STOPPED
}

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
    var testStatus by remember(uploadPayload) { mutableStateOf(TestStatus.NOT_RUN) }
    var testPassSeconds by remember(uploadPayload) { mutableStateOf<String?>(null) }
    var testedModsSignature by remember(uploadPayload) { mutableStateOf<String?>(null) }
    var crashTriggered by remember(uploadPayload) { mutableStateOf(false) }
    val testConsoleState = remember(uploadPayload) { ConsoleState(4000) }
    var testProcess by remember(uploadPayload) { mutableStateOf<Process?>(null) }
    var testWorkDir by remember(uploadPayload) { mutableStateOf<File?>(null) }
    var autoFixedModKeys by remember(uploadPayload) { mutableStateOf<Set<String>>(emptySet()) }

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
            runCatching {
                if (testProcess?.isAlive == true) {
                    testProcess?.destroyForcibly()
                }
                testWorkDir?.deleteRecursivelyNoSymlink()
            }
        }
    }

    fun destroyTestWorkDir() {
        testWorkDir?.let { dir ->
            runCatching { dir.deleteRecursivelyNoSymlink() }
            testWorkDir = null
        }
    }

    fun stopTestServer(markStopped: Boolean = true) {
        val process = testProcess
        if (process != null) {
            runCatching {
                if (process.isAlive) process.destroy()
            }
            testProcess = null
            if (markStopped && testStatus != TestStatus.PASSED) {
                testStatus = TestStatus.STOPPED
            }
            scope.launch {
                testConsoleState.append("[RDI] 已发送停止测试服务器指令")
            }
        }
        destroyTestWorkDir()
    }

    fun currentModsSignature(mods: List<Mod>): String =
        mods.asSequence()
            .sortedBy { modStableKey(it) }
            .joinToString("|") { "${modStableKey(it)}:${it.side.name}" }

    val passRegex = remember { Regex("""Done \((\d+(?:\.\d+)?)s\)! For help""") }
    val crashTriggerKeywords = remember {
        listOf(
            "Preparing crash report",
            "Failed to start the minecraft server",
            "Minecraft Crash Report",
            "Missing or unsupported mandatory dependencies"
        )
    }

    fun startTestWithAutoFix(confirm: UploadStep.Confirm) {
        val loaderVer = confirm.payload.mcVersion.loaderVersions[confirm.payload.modloader]
        if (loaderVer == null) {
            errorText = "缺少加载器版本配置，无法启动测试服务器"
            return
        }
        stopTestServer(markStopped = false)
        testConsoleState.clear()
        testConsoleState.append("[RDI] 启动测试服务器...")
        testStatus = TestStatus.RUNNING
        testPassSeconds = null
        testedModsSignature = null
        crashTriggered = false

        scope.launch(Dispatchers.IO) {
            runCatching {
                destroyTestWorkDir()
                val workDir = createServerTestWorkDir(confirm.payload, confirm.mods)
                scope.launch { testWorkDir = workDir }
                val process = GameService.startServer(
                    mcVer = confirm.payload.mcVersion,
                    loaderVer = loaderVer,
                    workDir = workDir
                ) { line ->
                    scope.launch {
                        testConsoleState.append(line)
                        if(line.contains("Error: could not open")){
                            testConsoleState.append("${confirm.payload.mcVersion.mcVer}-${confirm.payload.modloader.name}文件不完整，请前往mc资源界面重新下载")
                        }
                        val matched = passRegex.find(line)
                        if (matched != null) {
                            val latest = step as? UploadStep.Confirm
                            val normalizedMods = latest?.mods?.map { mod ->
                                if (mod.side == Mod.Side.UNKNOWN) {
                                    mod.copy(side = Mod.Side.BOTH).apply { vo = mod.vo }
                                } else mod
                            }.orEmpty()
                            val changedUnknown = latest?.mods
                                ?.count { it.side == Mod.Side.UNKNOWN }
                                ?: 0
                            if (latest != null && changedUnknown > 0) {
                                step = latest.copy(mods = normalizedMods)
                                testConsoleState.append("[RDI] 测试通过，已将 ${changedUnknown} 个未识别运行侧Mod标记为 BOTH")
                            }
                            testPassSeconds = matched.groupValues.getOrNull(1)
                            testStatus = TestStatus.PASSED
                            testedModsSignature = currentModsSignature(
                                if (normalizedMods.isNotEmpty()) normalizedMods
                                else (step as? UploadStep.Confirm)?.mods.orEmpty()
                            )
                        } else if (
                            crashTriggerKeywords.any { keyword -> line.contains(keyword, ignoreCase = true) }
                        ) {
                            if (testStatus != TestStatus.PASSED) {
                                crashTriggered = true
                                testStatus = TestStatus.FAILED
                            }
                        }
                    }
                }
                scope.launch { testProcess = process }
                val exitCode = process.waitFor()
                scope.launch {
                    if (testProcess == process) {
                        testProcess = null
                    }
                    if (testStatus == TestStatus.PASSED) return@launch

                    val latest = step as? UploadStep.Confirm
                    if (latest != null) {
                        val fix = autoFixClientSideFromCrashReport(
                            mods = latest.mods,
                            workDir = workDir,
                            alreadyFixed = autoFixedModKeys
                        )
                        if (fix != null) {
                            val newMods = updateModSide(
                                mods = latest.mods,
                                index = -1,
                                modKey = fix.modKey,
                                newSide = Mod.Side.CLIENT
                            )
                            autoFixedModKeys = autoFixedModKeys + fix.modKey
                            step = latest.copy(mods = newMods)
                            testConsoleState.append("[RDI] 自动修复：将 ${fix.modName} 标记为客户端Mod，重试测试...")
                            (step as? UploadStep.Confirm)?.let { startTestWithAutoFix(it) }
                            return@launch
                        }
                    }
                    testStatus = if (crashTriggered) TestStatus.FAILED else TestStatus.STOPPED
                    if (exitCode != 0 && crashTriggered) {
                        errorText = "测试服务器异常退出: $exitCode"
                    }

                }
            }.onFailure {
                scope.launch {
                    testStatus = TestStatus.FAILED
                    errorText = "启动测试服务器失败: ${it.message}"
                    stopTestServer(markStopped = false)
                }
            }
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
                        val signatureNow = currentModsSignature(current.mods)
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
                                if (testProcess?.isAlive == true) {
                                    errorText = "测试服务器已经在运行中"
                                    return@CircleIconButton
                                }
                                startTestWithAutoFix(current)
                            }
                            CircleIconButton("\uF04D", "停止测试", bgColor = MaterialColor.RED_900.color) {
                                stopTestServer()
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
                                        testedModsSignature = null
                                        if (testStatus == TestStatus.PASSED) {
                                            testStatus = TestStatus.NOT_RUN
                                            testPassSeconds = null
                                        }
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

private fun createServerTestWorkDir(
    payload: UploadPayload,
    mods: List<Mod>
): File {
    val testDir = Files.createTempDirectory(ModpackService.PACK_PROC_DIR.toPath(), "servertest-").toFile()
    val libsSource = GameService.DIR.resolve("libraries")
    if (libsSource.exists()) {
        val libsTarget = testDir.resolve("libraries")
        runCatching {
            Files.deleteIfExists(libsTarget.toPath())
            Files.createSymbolicLink(libsTarget.toPath(), libsSource.toPath())
        }.getOrElse {
            throw IllegalStateException("创建测试目录 libraries 软链接失败: ${it.message}")
        }
    }
    val sourceDir = payload.sourceDir
    val overridesDir = sourceDir.resolve("overrides")
    if (overridesDir.exists() && overridesDir.isDirectory) {
        copyDirectoryContent(overridesDir, testDir)
    } else {
        sourceDir.listFiles()?.forEach { child ->
            if (child.name.equals("mods", ignoreCase = true)) return@forEach
            if (child.name.equals("manifest.json", ignoreCase = true)) return@forEach
            if (child.name.equals("modrinth.index.json", ignoreCase = true)) return@forEach
            val target = testDir.resolve(child.name)
            if (child.isDirectory) {
                child.copyRecursively(target, overwrite = true)
            } else {
                target.parentFile?.mkdirs()
                Files.copy(child.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
    val modsDir = testDir.resolve("mods").apply { mkdirs() }
    mods.filter { it.side != Mod.Side.CLIENT }.forEach { mod ->
        val source = DL_MOD_DIR.resolve(mod.fileName)
        if (!source.exists()) return@forEach
        val target = modsDir.resolve(source.name)
        runCatching {
            Files.deleteIfExists(target.toPath())
            Files.createSymbolicLink(target.toPath(), source.toPath())
        }.onFailure {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
    return testDir
}

private fun copyDirectoryContent(source: File, target: File) {
    source.listFiles()?.forEach { child ->
        val dest = target.resolve(child.name)
        if (child.isDirectory) {
            child.copyRecursively(dest, overwrite = true)
        } else {
            dest.parentFile?.mkdirs()
            Files.copy(child.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

private data class CrashAutoFixMatch(
    val modKey: String,
    val modName: String
)

private fun autoFixClientSideFromCrashReport(
    mods: List<Mod>,
    workDir: File,
    alreadyFixed: Set<String>
): CrashAutoFixMatch? {
    val crashFile = workDir.resolve("crash-reports")
        .listFiles()
        ?.filter { it.isFile && it.name.startsWith("crash-") && it.extension.equals("txt", true) }
        ?.maxByOrNull { it.lastModified() }
        ?: return null
    val lines = runCatching { crashFile.readLines() }.getOrNull() ?: return null
    val modFiles = linkedSetOf<String>()
    val modSlugs = linkedSetOf<String>()
    val invalidDistRegex =
        Regex("""Attempted to load class .* invalid dist\s+DEDICATED_SERVER""", RegexOption.IGNORE_CASE)
    lines.forEachIndexed { index, line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("Mod File:", ignoreCase = true)) {
            modFiles += trimmed.substringAfter(":").trim().substringAfterLast('/').substringAfterLast('\\')
        }
        if (trimmed.startsWith("-- MOD ")) {
            modSlugs += trimmed.removePrefix("-- MOD ").removeSuffix(" --").trim().lowercase()
        }
        if (trimmed.startsWith("-- Mod loading issue for:", ignoreCase = true)) {
            modSlugs += trimmed.substringAfter(":").trim().lowercase()
        }
        if (invalidDistRegex.containsMatchIn(trimmed)) {
            for (i in index + 1 until minOf(lines.size, index + 40)) {
                val nearby = lines[i].trim()
                if (nearby.startsWith("Mod file:", ignoreCase = true)) {
                    modFiles += nearby.substringAfter(":").trim().substringAfterLast('/').substringAfterLast('\\')
                    break
                }
            }
        }
    }
    val candidate = mods.firstOrNull { mod ->
        if (mod.side == Mod.Side.CLIENT) return@firstOrNull false
        val key = modStableKey(mod)
        if (key in alreadyFixed) return@firstOrNull false
        val byFile = modFiles.any {
            it.equals(mod.fileName, true) ||
                it.contains(mod.hash, ignoreCase = true) ||
                it.contains(mod.slug, ignoreCase = true)
        }
        val bySlug = modSlugs.contains(mod.slug.lowercase())
        byFile || bySlug
    } ?: return null

    val modName = candidate.vo?.nameCn?.takeIf { it.isNotBlank() }
        ?: candidate.vo?.name?.takeIf { it.isNotBlank() }
        ?: candidate.slug

    return CrashAutoFixMatch(
        modKey = modStableKey(candidate),
        modName = modName
    )
}
