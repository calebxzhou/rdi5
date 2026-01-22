package calebxzhou.rdi.client

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import calebxzhou.mykotutils.std.decodeBase64
import calebxzhou.mykotutils.std.ioScope
import calebxzhou.mykotutils.std.jarResource
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.service.GameService
import calebxzhou.rdi.client.service.PlayerService
import calebxzhou.rdi.client.ui2.McPlayStore
import calebxzhou.rdi.client.ui2.TaskStore
import calebxzhou.rdi.client.ui2.screen.*
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.common.model.Task
import calebxzhou.rdi.common.model.TaskProgress
import calebxzhou.rdi.common.serdesJson
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import java.awt.Toolkit
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.datatransfer.DataFlavor
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipFile
import javax.swing.SwingUtilities

lateinit var UIFontFamily: FontFamily
lateinit var ArtFontFamily: FontFamily
lateinit var CodeFontFamily: FontFamily
lateinit var IconFontFamily: FontFamily
lateinit var ScreenSize: Pair<Dp, Dp>
fun main() = application {
    val windowIcon = remember {
        jarResource("icon.png").use { stream ->
            BitmapPainter(stream.readAllBytes().decodeToImageBitmap())
        }
    }
    val fontDir = RDIClient.DIR.resolve("run").resolve("fonts").takeIf { it.exists() }
        ?: RDIClient.DIR.resolve("fonts")
    val uiFont = fontDir.resolve("1oppo.ttf")
    val artFont = fontDir.resolve("smiley-sans.otf")
    val codeFont = fontDir.resolve("jetbrainsmono.ttf")
    val iconFont = fontDir.resolve("symbolsnerdfont.ttf")

    UIFontFamily = remember {
        FontFamily(listOf(uiFont, iconFont).map { Font(it) })
    }
    CodeFontFamily = remember {
        FontFamily(listOf(codeFont, uiFont, iconFont).map { Font(it) })
    }
    ArtFontFamily = remember {
        FontFamily(listOf(artFont, iconFont).map { Font(it) })
    }
    IconFontFamily = remember {
        FontFamily(listOf(iconFont).map { Font(it) })
    }
    //账号信息
    System.getProperty("rdi.account")?.let {
        loggedAccount = serdesJson.decodeFromString(it.decodeBase64)
    }
    //JWT
    System.getProperty("rdi.jwt")?.let {
        loggedAccount.jwt = it
    }
    if (loggedAccount.jwt == null && loggedAccount != RAccount.DEFAULT) {
        ioScope.launch {
            val jwt = PlayerService.getJwt(loggedAccount.qq, loggedAccount.pwd)
            loggedAccount.jwt = jwt
        }
    }
    // 设置窗口初始大小为屏幕的2/3，并居中显示
    val screen = remember { Toolkit.getDefaultToolkit().screenSize }
    ScreenSize = (screen.width * 2 / 3).dp to  (screen.height * 2 / 3).dp
    val windowState = rememberWindowState(
        width = ScreenSize.first,
        height = ScreenSize.second,
        position = WindowPosition(Alignment.Center)
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "RDI ${Const.VERSION_NUMBER}",
        icon = windowIcon,
        state = windowState
    ) {
        MaterialTheme(typography = Typography(defaultFontFamily = UIFontFamily)) {
            val navController = rememberNavController()
            val dropWindow = window
            DisposableEffect(dropWindow, navController) {
                installPackDropTarget(dropWindow) { task ->
                    TaskStore.current = task
                    SwingUtilities.invokeLater {
                        navController.navigate(TaskView)
                    }
                }
                onDispose { dropWindow.dropTarget = null }
            }
            val initScreenName = System.getProperty("rdi.init.screen")?.trim()
            val startDestination = when (initScreenName) {
                "pf" -> Profile
                "wd" -> Wardrobe
                "mail" -> Mail
                "hl" -> HostList
                "wl" -> WorldList
                "ml" -> ModpackList
                "mm" -> ModpackManage
                else -> Login
            }
            NavHost(navController = navController, startDestination = startDestination) {
                composable<Login> {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Profile) {
                                popUpTo(Login) { inclusive = true }
                            }
                        }
                    )
                }
                composable<Wardrobe> { WardrobeScreen(onBack = { navController.navigate(Profile) }) }
                composable<Mail> { MailScreen(onBack = { navController.navigate(Profile) }) }
                composable<HostList> {
                    HostListScreen(
                        onBack = { navController.navigate(Profile) },
                        onOpenWorldList = { navController.navigate(WorldList) },
                        onOpenHostInfo = { hostId ->
                            navController.navigate(HostInfo(hostId))
                        },
                        onOpenModpackList = { navController.navigate(ModpackList) },
                        onOpenMcPlay = { args ->
                            McPlayStore.current = args
                            navController.navigate(McPlayView)
                        }
                    )
                }
                composable<HostInfo> {
                    val route = it.toRoute<HostInfo>()
                    HostInfoScreen(
                        hostId = ObjectId(route.hostId),
                        onBack = { navController.navigate(HostList) },
                        onOpenModpackInfo = { modpackId ->
                            navController.navigate(ModpackInfo(modpackId, fromHostId = route.hostId))
                        },
                        onOpenMcPlay = { args ->
                            McPlayStore.current = args
                            navController.navigate(McPlayView)
                        },
                        onOpenHostEdit = { host ->
                            navController.navigate(
                                HostCreate(
                                    modpackId = host.modpack.id.toHexString(),
                                    modpackName = host.modpack.name,
                                    packVer = host.packVer,
                                    skyblock = host.levelType.contains("skyblock", true),
                                    hostId = host._id.toHexString()
                                )
                            )
                        }
                    )
                }
                composable<WorldList> {
                    WorldListScreen(onBack = { navController.navigate(HostList) })
                }
                composable<HostCreate> {
                    HostCreateScreen(
                        it.toRoute(),
                        onBack = { navController.popBackStack() },
                        onNavigateProfile = { navController.navigate(Profile) }
                    )
                }
                composable<TaskView> {
                    val task = TaskStore.current
                    if (task != null) {
                        TaskScreen(task, onBack = { navController.popBackStack() })
                    } else {
                        Text("没有可显示的任务")
                    }
                }
                composable<McPlayView> {
                    val args = McPlayStore.current
                    if (args != null) {
                        McPlayScreen(
                            title = args.title,
                            mcVer = args.mcVer,
                            versionId = args.versionId,
                            jvmArgs = args.jvmArgs.toTypedArray(),
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        Text("没有可显示的游戏")
                    }
                }
                composable<Profile> {
                    ProfileScreen(
                        onLogout = {
                            navController.navigate(Login) {
                                popUpTo(Profile) { inclusive = true }
                            }
                        },
                        onOpenWardrobe = { navController.navigate(Wardrobe) },
                        onOpenHostList = { navController.navigate(HostList) },
                        onOpenMail = { navController.navigate(Mail) },
                        onOpenModpackManage = { navController.navigate(ModpackManage) }
                    )
                }
                composable<ModpackList> {
                    ModpackListScreen(
                        onBack = { navController.navigate(ModpackManage) },
                        onOpenUpload = { navController.navigate(ModpackUpload) },
                        onOpenInfo = { modpackId ->
                            navController.navigate(ModpackInfo(modpackId))
                        }
                    )
                }
                composable<ModpackInfo> {
                    val route = it.toRoute<ModpackInfo>()
                    ModpackInfoScreen(
                        modpackId = route.modpackId,
                        onBack = {
                            if (route.fromHostId != null) {
                                navController.navigate(HostInfo(route.fromHostId))
                            } else {
                                navController.navigate(ModpackList)
                            }
                        },
                        onCreateHost = { modpackId, modpackName, packVer, skyblock ->
                            navController.navigate(
                                HostCreate(
                                    modpackId = modpackId,
                                    modpackName = modpackName,
                                    packVer = packVer,
                                    skyblock = skyblock
                                )
                            )
                        },
                        onOpenTask = { task ->
                            TaskStore.current = task
                            navController.navigate(TaskView)
                        }
                    )
                }
                composable<ModpackManage> {
                    ModpackManageScreen(
                        onBack = { navController.navigate(Profile) },
                        onOpenModpackList = { navController.navigate(ModpackList) },
                        onOpenTask = { task ->
                            TaskStore.current = task
                            navController.navigate(TaskView)
                        },
                        onOpenMcPlay = { args ->
                            McPlayStore.current = args
                            navController.navigate(McPlayView)
                        }
                    )
                }
                composable<ModpackUpload> {
                    ModpackUploadScreen(onBack = { navController.navigate(ModpackList) })
                }
            }
        }
    }
}

private fun installPackDropTarget(
    window: java.awt.Window,
    onOpenTask: (Task) -> Unit
) {
    val listener = object : DropTargetAdapter() {
        override fun dragEnter(dtde: DropTargetDragEvent) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
            } else {
                dtde.rejectDrag()
            }
        }

        override fun dragOver(dtde: DropTargetDragEvent) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
            } else {
                dtde.rejectDrag()
            }
        }

        override fun drop(dtde: DropTargetDropEvent) {
            if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return
            dtde.acceptDrop(DnDConstants.ACTION_COPY)
            val files = dtde.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                ?: return
            val packFiles = files.filterIsInstance<java.io.File>()
                .filter { it.name.endsWith(".mc.rdipack", ignoreCase = true) }
            if (packFiles.isEmpty()) return
            val task = if (packFiles.size == 1) {
                buildImportPackTask(packFiles.first())
            } else {
                Task.Sequence(
                    name = "导入整合包",
                    subTasks = packFiles.map { buildImportPackTask(it) }
                )
            }
            onOpenTask(task)
        }
    }
    window.dropTarget = DropTarget(window, listener)
    (window as? java.awt.Container)?.let { container ->
        attachDropTargetRecursively(container, listener)
    }
    (window as? javax.swing.RootPaneContainer)?.let { root ->
        attachDropTargetRecursively(root.contentPane, listener)
        attachDropTargetRecursively(root.glassPane as? java.awt.Container, listener)
    }
}

private fun attachDropTargetRecursively(
    container: java.awt.Container?,
    listener: DropTargetAdapter
) {
    if (container == null) return
    container.dropTarget = DropTarget(container, listener)
    container.components.forEach { component ->
        component.dropTarget = DropTarget(component, listener)
        if (component is java.awt.Container) {
            attachDropTargetRecursively(component, listener)
        }
    }
}

private fun buildImportPackTask(zipFile: java.io.File): Task {
    return Task.Leaf("导入 ${zipFile.name}") { ctx ->
        val targetRoot = GameService.DIR.canonicalFile
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
                        ).use { output ->
                            input.copyTo(output)
                        }
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
