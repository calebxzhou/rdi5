package calebxzhou.rdi.client.ui.screen

import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ClientDirs
import calebxzhou.rdi.client.service.ModpackLocalDir
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.model.Task
import calebxzhou.rdi.common.model.TaskProgress
import calebxzhou.rdi.client.service.ModpackService.startInstall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual fun selectRdiPackFiles(): List<File>? {
    val chooser = JFileChooser().apply {
        dialogTitle = "选择资源包 (*.mc.rdipack)"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = true
        currentDirectory = File("C:/Users/${System.getProperty("user.name")}/Downloads")
        fileFilter = FileNameExtensionFilter("RDI资源包 (*.rdipack)", "rdipack")
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    val files = chooser.selectedFiles?.toList()
        ?: chooser.selectedFile?.let { listOf(it) }
        ?: return null
    return files.filter { it.exists() && it.isFile && it.name.endsWith(".rdipack", ignoreCase = true) }
        .takeIf { it.isNotEmpty() }
}

private fun selectRdiModpackFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "选择RDI整合包 (*.rdimodpack)"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = false
        currentDirectory = File("C:/Users/${System.getProperty("user.name")}/Downloads")
        fileFilter = FileNameExtensionFilter("RDI整合包 (*.rdimodpack)", "rdimodpack")
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    return chooser.selectedFile
        ?.takeIf { it.exists() && it.isFile && it.name.endsWith(".rdimodpack", ignoreCase = true) }
}

actual fun buildImportPackTask(zipFile: File): Task {
    return Task.Leaf("导入 ${zipFile.name}") { ctx ->
        val targetRoot = ClientDirs.mcDir.canonicalFile
        val totalFiles = ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().count { !it.isDirectory }
        }.coerceAtLeast(1)
        var processed = 0
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val name = entry.name.replace('\\', '/').trimStart('/')
                if (name.isEmpty()) return@forEach
                val outFile = targetRoot.resolve(name)
                val normalized = outFile.canonicalFile
                if (!normalized.path.startsWith(targetRoot.path)) return@forEach
                if (entry.isDirectory) {
                    normalized.mkdirs()
                } else {
                    normalized.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        Files.newOutputStream(
                            normalized.toPath(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                        ).use { output -> input.copyTo(output) }
                    }
                    processed += 1
                    ctx.emitProgress(
                        TaskProgress("解压 ${entry.name}", processed.toFloat() / totalFiles)
                    )
                }
            }
        }
        ctx.emitProgress(TaskProgress("完成", 1f))
    }
}

actual suspend fun exportRdiModpack(
    packdir: ModpackLocalDir,
    onProgress: (String) -> Unit
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val packZip = ClientDirs.dlPacksDir.resolve("${packdir.vo.id}_${packdir.verName}.zip")
        if (!packZip.exists()) {
            throw IllegalStateException("整合包文件不存在，请先下载")
        }
        val version = server.makeRequest<Modpack.Version>(
            "modpack/${packdir.vo.id}/version/${packdir.verName}"
        ).data ?: throw IllegalStateException("无法获取整合包版本信息")

        val chooser = JFileChooser().apply {
            dialogTitle = "选择导出位置"
            fileSelectionMode = JFileChooser.FILES_ONLY
            val safeName = packdir.vo.name.ifBlank { "modpack" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val defaultName = "${safeName}_${packdir.verName}.rdimodpack"
            selectedFile = File(System.getProperty("user.home"), defaultName)
            fileFilter = FileNameExtensionFilter("RDI整合包 (*.rdimodpack)", "rdimodpack")
        }
        val result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return@runCatching

        var outputFile = chooser.selectedFile
        if (!outputFile.name.endsWith(".rdimodpack", ignoreCase = true)) {
            outputFile = File(outputFile.parentFile, "${outputFile.name}.rdimodpack")
        }

        val missingMods = mutableListOf<String>()
        val total = version.mods.size + 1
        var processed = 0
        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(packZip.name))
            packZip.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()
            processed += 1
            onProgress("导出整合包 ${processed}/${total}")

            version.mods.forEach { mod ->
                val modFile = DL_MOD_DIR.resolve(mod.fileName)
                if (!modFile.exists()) {
                    missingMods += mod.fileName
                    return@forEach
                }
                zipOut.putNextEntry(ZipEntry("mods/${mod.fileName}"))
                modFile.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
                processed += 1
                onProgress("导出MOD ${processed}/${total}")
            }
        }

        if (missingMods.isNotEmpty()) {
            runCatching { outputFile.delete() }
            throw IllegalStateException("缺少 ${missingMods.size} 个MOD文件：${missingMods.take(3).joinToString()}${if (missingMods.size > 3) "..." else ""}")
        }
    }
}

actual suspend fun importRdiModpack(
    onProgress: (String) -> Unit
): Task = withContext(Dispatchers.IO) {
    val file = selectRdiModpackFile() ?: throw IllegalStateException("未选择整合包文件")
    val packZipName: String
    ZipFile(file).use { zip ->
        val packEntry = zip.entries().asSequence()
            .firstOrNull { !it.isDirectory && it.name.endsWith(".zip", ignoreCase = true) && !it.name.startsWith("mods/") }
            ?: throw IllegalStateException("整合包内未找到modpack.zip")

        packZipName = File(packEntry.name).name
        val packTarget = ClientDirs.dlPacksDir.resolve(packZipName)
        packTarget.parentFile?.mkdirs()
        val modEntries = zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith("mods/") }
            .toList()
        val total = modEntries.size + 1
        var processed = 0
        zip.getInputStream(packEntry).use { input ->
            Files.newOutputStream(
                packTarget.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { output -> input.copyTo(output) }
        }
        processed += 1
        onProgress("导入整合包 ${processed}/${total}")

        modEntries.forEach { entry ->
            val filename = entry.name.substringAfter("mods/").trim()
            if (filename.isBlank()) return@forEach
            val target = DL_MOD_DIR.resolve(filename)
            target.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input ->
                Files.newOutputStream(
                    target.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).use { output -> input.copyTo(output) }
            }
            processed += 1
            onProgress("导入MOD ${processed}/${total}")
        }
    }

    val match = Regex("^([0-9a-fA-F]{24})_(.+)\\.zip$").find(packZipName)
        ?: throw IllegalStateException("整合包文件名无效：$packZipName")
    val (idStr, verName) = match.destructured
    val modpackId = org.bson.types.ObjectId(idStr)
    val modpackVo = server.makeRequest<Modpack.BriefVo>("modpack/${modpackId}/brief").data
        ?: throw IllegalStateException("未找到整合包信息")
    val version = server.makeRequest<Modpack.Version>("modpack/${modpackId}/version/${verName}").data
        ?: throw IllegalStateException("未找到整合包版本信息")
    version.startInstall(modpackVo.mcVer, modpackVo.modloader, modpackVo.name)
}

actual suspend fun exportLogsPack(packdir: ModpackLocalDir): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val sourceDirs = listOf("logs", "crash-reports")
            .map { packdir.dir.resolve(it) }
            .filter { it.exists() && it.isDirectory }
        if (sourceDirs.isEmpty()) {
            throw IllegalStateException("没有可导出的日志目录")
        }

        val chooser = JFileChooser().apply {
            dialogTitle = "选择日志保存位置"
            fileSelectionMode = JFileChooser.FILES_ONLY
            val safeName = packdir.vo.name.ifBlank { "modpack" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val defaultName = "${safeName}_${packdir.verName}_logs.zip"
            selectedFile = java.io.File(System.getProperty("user.home"), defaultName)
            fileFilter = FileNameExtensionFilter("ZIP 文件 (*.zip)", "zip")
        }
        val result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return@runCatching

        var outputFile = chooser.selectedFile
        if (!outputFile.name.endsWith(".zip", ignoreCase = true)) {
            outputFile = java.io.File(outputFile.parentFile, "${outputFile.name}.zip")
        }

        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            sourceDirs.forEach { dir ->
                val baseName = dir.name
                dir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relative = file.relativeTo(dir).invariantSeparatorsPath
                        val entryName = "$baseName/$relative"
                        zipOut.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
            }
        }
    }
}
