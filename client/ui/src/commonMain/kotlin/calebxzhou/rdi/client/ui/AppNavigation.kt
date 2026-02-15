package calebxzhou.rdi.client.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import calebxzhou.rdi.client.UIFontFamily
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.ui.screen.*
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.util.toUUID
import org.bson.types.ObjectId

/**
 * Common Typography using the cross-platform UIFontFamily.
 */
val AppTypography: Typography
    @Composable get() = Typography(defaultFontFamily = UIFontFamily)

/**
 * Common navigation graph shared between desktop and Android.
 * Desktop-only routes (McPlayView, ModpackUpload) are gated behind isDesktop.
 *
 * @param startDestination the initial route (default: Login)
 * @param onInstallDropTask optional callback for desktop drag-and-drop import (desktop sets this from Main.kt)
 */
@Composable
fun AppNavigation(
    startDestination: Any = Login,
) {
    MaterialTheme(typography = AppTypography) {
        val navController = rememberNavController()
        val openTaskView: (calebxzhou.rdi.common.model.Task, Boolean, (() -> Unit)?) -> Unit = { task, autoClose, onDone ->
            TaskStore.current = task
            TaskStore.autoClose = autoClose
            TaskStore.onDone = onDone
            navController.navigate(TaskView)
        }

        // Android FCL launch dialog
        val showFclLaunchDialog = remember { mutableStateOf(false) }
        val fclLaunchArgs = remember { mutableStateOf<McPlayArgs?>(null) }
        val handleOpenMcPlay: (McPlayArgs) -> Unit = { args ->
            if (isDesktop) {
                McPlayStore.current = args
                navController.navigate(McPlayView)
            } else {
                fclLaunchArgs.value = args
                showFclLaunchDialog.value = true
            }
        }
        if (showFclLaunchDialog.value) {
            val args = fclLaunchArgs.value
            if (args != null) {
                val uuid = runCatching {
                    loggedAccount._id.toUUID().toString().replace("-", "")
                }.getOrDefault("unknown")
                AlertDialog(
                    onDismissRequest = { showFclLaunchDialog.value = false },
                    title = { Text("在FCL中启动游戏") },
                    text = {
                        Column {
                            Text("请在Fold Craft Launcher中配置：")
                            Spacer(Modifier.height(8.dp))
                            Text("版本: ${args.versionId}")
                            Spacer(Modifier.height(4.dp))
                            Text("UUID: $uuid")
                            if (args.jvmArgs.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text("JVM参数: ${args.jvmArgs.joinToString(" ")}")
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("已点击 复制参数 后可粘贴到FCL的JVM参数中。")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val copyText = buildString {
                                append("版本: ${args.versionId}\n")
                                append("UUID: $uuid\n")
                                if (args.jvmArgs.isNotEmpty()) {
                                    append("JVM参数: ${args.jvmArgs.joinToString(" ")}\n")
                                }
                            }
                            copyToClipboard(copyText)
                            openGameLauncher()
                            showFclLaunchDialog.value = false
                        }) {
                            Text("复制参数并打开FCL")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFclLaunchDialog.value = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }

        NavHost(navController = navController, startDestination = startDestination) {
            composable<Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(HostList) {
                            popUpTo(Login) { inclusive = true }
                        }
                    },
                    onOpenRegister = { msa -> navController.navigate(Register(msa)) }
                )
            }
            composable<Register> {
                val route = it.toRoute<Register>()
                RegisterScreen(
                    route.msa,
                    onBack = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.popBackStack()
                    }
                )
            }
            composable<Wardrobe> { WardrobeScreen(onBack = { navController.navigate(HostList) }) }
            composable<Mail> { MailScreen(onBack = { navController.navigate(HostList) }) }
            composable<HostList> {
                HostListScreen(
                    onBack = { navController.navigate(Login) },
                    onOpenWorldList = { navController.navigate(WorldList) },
                    onOpenHostInfo = { hostId ->
                        navController.navigate(HostInfo(hostId))
                    },
                    onOpenModpackList = { navController.navigate(ModpackList) },
                    onOpenMcPlay = handleOpenMcPlay,
                    onOpenMcVersions = { mcVer ->
                        navController.navigate(RMcVersion(mcVer?.mcVer))
                    },
                    onOpenTask = { task ->
                        openTaskView(task, false, null)
                    },
                    onOpenWardrobe = { navController.navigate(Wardrobe) },
                    onOpenMail = { navController.navigate(Mail) },
                    onOpenSettings = { navController.navigate(Setting) }
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
                    onOpenMcPlay = handleOpenMcPlay,
                    onOpenMcVersions = { mcVer ->
                        navController.navigate(RMcVersion(mcVer?.mcVer))
                    },
                    onOpenTask = { task ->
                        openTaskView(task, false, null)
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
                    onNavigateProfile = { navController.navigate(HostList) }
                )
            }
            composable<TaskView> {
                val task = TaskStore.current
                if (task != null) {
                    TaskScreen(
                        task = task,
                        autoClose = TaskStore.autoClose,
                        onBack = { navController.popBackStack() },
                        onDone = {
                            TaskStore.onDone?.invoke()
                            TaskStore.onDone = null
                            TaskStore.autoClose = false
                        }
                    )
                } else {
                    Text("没有可显示的任务")
                }
            }
            // Desktop-only routes (McPlayView, ModpackUpload) are added via expect/actual
            addDesktopOnlyRoutes(navController)
            composable<Setting> {
                SettingScreen(
                    onBack = { navController.navigate(HostList) },
                )
            }
            composable<ModpackList> {
                ModpackListScreen(
                    onBack = { navController.navigate(HostList) },
                    onOpenUpload = { navController.navigate(ModpackUpload) },
                    onOpenTask = { task, autoClose, onDone ->
                        openTaskView(task, autoClose, onDone)
                    },
                    onOpenMcVersions = { navController.navigate(RMcVersion(null)) },
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
                    onOpenUpload = { modpackId, modpackName ->
                        navController.navigate(ModpackUpload)
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
                        openTaskView(task, false, null)
                    }
                )
            }

            composable<RMcVersion> {
                val route = it.toRoute<RMcVersion>()
                val required = route.mcVer?.let { ver -> McVersion.from(ver) }
                McVersionScreen(
                    requiredMcVer = required,
                    onBack = { navController.navigate(ModpackList) },
                    onOpenTask = { task ->
                        openTaskView(task, false, null)
                    },
                    onOpenPlay = handleOpenMcPlay
                )
            }
        }
    }
}
