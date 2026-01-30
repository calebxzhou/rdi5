package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.DEFAULT_MODPACK_ICON
import calebxzhou.rdi.client.ui2.MaterialColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * calebxzhou @ 2026-01-27 21:24
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpackManageCard(
    packdir: ModpackService.LocalDir,
    isRunning: Boolean = false,
    onDelete: () -> Unit,
    onReinstall: () -> Unit,
    onOpenFolder: () -> Unit,
    onExportLogs: () -> Unit,
    onOpenMcPlay: (() -> Unit)? = null
) {

    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val glowModifier = if (isRunning) {
        Modifier
            .background(Color(0xFFE9D5FF), cardShape)
            .padding(2.dp)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(glowModifier)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF9F9FB),
            shape = cardShape,
            elevation = 1.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialColor.GRAY_200.color, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val iconUrl = packdir.vo.icon?.takeIf { it.isNotBlank() }
                    if (iconUrl != null) {
                        HttpImage(
                            imgUrl = iconUrl,
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = "Modpack Icon"
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            bitmap = DEFAULT_MODPACK_ICON,
                            contentDescription = "Modpack Icon",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = packdir.vo.name,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = packdir.verName,
                            style = MaterialTheme.typography.body2,
                            color = MaterialColor.GRAY_700.color
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val size = 32
                        CircleIconButton(
                            "\uEA81",
                            "删除",
                            size=size,
                            bgColor = MaterialColor.RED_900.color,
                            longPressDelay = 5000L
                        ) {
                            onDelete()
                        }
                        CircleIconButton(
                            "\uDB81\uDC53",
                            "重装",
                            size=size,
                            bgColor = MaterialColor.PINK_900.color,
                        ) {
                            onReinstall()
                        }
                        CircleIconButton(
                            "\uEAED",
                            "打开文件夹",
                            size=size,

                        ) {
                            onOpenFolder ()
                        }
                        CircleIconButton(
                            "\uEF11",
                            "导出日志",
                            size=size,
                            bgColor = MaterialColor.GRAY_900.color,
                        ) {
                            onExportLogs()
                        }
                        if (isRunning && onOpenMcPlay != null) {
                            CircleIconButton(
                                "\uDB80\uDD8D",
                                "控制台",
                                size=size,
                                bgColor = MaterialColor.BLUE_800.color,
                            ) {
                                onOpenMcPlay()
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun exportLogsPack(packdir: ModpackService.LocalDir): Result<Unit> = withContext(Dispatchers.IO) {
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
