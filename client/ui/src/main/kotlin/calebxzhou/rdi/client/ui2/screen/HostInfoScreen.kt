package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import calebxzhou.mykotutils.std.secondsToHumanDateTime
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.rdiRequest
import calebxzhou.rdi.client.net.rdiRequestU
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ModpackService.startPlay
import calebxzhou.rdi.client.ui2.BottomSnakebar
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.ConfirmDialog
import calebxzhou.rdi.client.ui2.ErrorText
import calebxzhou.rdi.client.ui2.MainBox
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.SimpleTooltip
import calebxzhou.rdi.client.ui2.Space8h
import calebxzhou.rdi.client.ui2.Space8w
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.asIconText
import calebxzhou.rdi.client.ui2.comp.Console
import calebxzhou.rdi.client.ui2.comp.ConsoleState
import calebxzhou.rdi.client.ui2.comp.GameRuleModal
import calebxzhou.rdi.client.ui2.comp.HeadButton
import calebxzhou.rdi.client.ui2.comp.ModpackCard
import calebxzhou.rdi.common.extension.isAdmin
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.model.Role
import io.ktor.client.plugins.sse.SSEBufferPolicy
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2026-01-15 19:38
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostInfoScreen(
    hostId: ObjectId,
    onBack: () -> Unit = {},
    onOpenModpackInfo: ((String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var okMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var hostDetail by remember { mutableStateOf<Host.DetailVo?>(null) }
    var modpackDetail by remember { mutableStateOf<Modpack.DetailVo?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showUpdateConfirm by remember { mutableStateOf(false) }
    var roleChangeConfirm by remember { mutableStateOf<RoleChange?>(null) }
    var transferConfirm by remember { mutableStateOf<ObjectId?>(null) }
    var kickConfirm by remember { mutableStateOf<ObjectId?>(null) }
    var stopConfirm by remember { mutableStateOf(false) }
    var restartConfirm by remember { mutableStateOf(false) }
    var forceStopConfirm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var consoleState by remember { mutableStateOf(ConsoleState()) }
    var logStreamJob by remember { mutableStateOf<Job?>(null) }
    var logStreamSseJob by remember { mutableStateOf<Job?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteQq by remember { mutableStateOf("") }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var optionDifficulty by remember { mutableStateOf(2) }
    var optionGameMode by remember { mutableStateOf(0) }
    var optionLevelType by remember { mutableStateOf("") }
    var optionWhitelist by remember { mutableStateOf(false) }
    var optionAllowCheats by remember { mutableStateOf(false) }
    var showGameRulesDialog by remember { mutableStateOf(false) }
    var gameRulesTouched by remember { mutableStateOf(false) }
    val gameRuleOverrides = remember { mutableStateMapOf<String, String>() }

    fun reload() {
        loading = true
        errorMessage = null
        scope.rdiRequest<Host.DetailVo>(
            path = "host/$hostId/detail",
            onOk = { response ->
                val detail = response.data
                if (detail == null) {
                    errorMessage = "无法加载地图信息"
                    loading = false
                    return@rdiRequest
                }
                hostDetail = detail
                scope.rdiRequest<Modpack.DetailVo>(
                    path = "modpack/${detail.modpack.id}",
                    onOk = { modpackResponse ->
                        modpackDetail = modpackResponse.data
                    },
                    onErr = {
                        errorMessage = "加载整合包信息失败: ${it.message}"
                        modpackDetail = null
                    },
                    onDone = { loading = false }
                )
            },
            onErr = {
                errorMessage = "加载地图信息失败: ${it.message}"
            },
            onDone = { loading = false }
        )
    }

    LaunchedEffect(hostId) {
        reload()
    }
    LaunchedEffect(okMessage) {
        okMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            okMessage = null
        }
    }

    val host = hostDetail
    val meAdmin = host?.isAdmin(loggedAccount) ?: false
    val meOwner = host?.ownerId == loggedAccount._id

    DisposableEffect(selectedTab, hostId) {
        if (selectedTab != 1) {
            onDispose { }
        } else {
            consoleState.clear()
            logStreamJob?.cancel()
            logStreamSseJob?.cancel()
            logStreamJob = scope.rdiRequest<String>(
                path = "host/$hostId/log/200",
                onOk = { response ->
                    response.data?.lineSequence()
                        ?.toMutableList()
                        ?.reversed()
                        ?.map { it.trimEnd('\r') }
                        ?.filter { it.isNotBlank() }
                        ?.forEach { consoleState.append(it) }
                },
                onErr = { errorMessage = "日志加载失败: ${it.message}" }
            )
            logStreamSseJob = server.sse(
                path = "host/$hostId/log/stream",
                bufferPolicy = SSEBufferPolicy.LastEvents(50),
                onEvent = { event ->
                    if (event.event == "heartbeat") return@sse
                    val payload = event.data?.ifBlank { null } ?: return@sse
                    scope.launch {
                        payload.lineSequence()
                            .map { it.trimEnd('\r') }
                            .filter { it.isNotBlank() }
                            .forEach { consoleState.append(it) }
                    }
                },
                onError = { throwable ->
                    errorMessage = "日志流错误: ${throwable.message}"
                }
            )
            onDispose {
                logStreamSseJob?.cancel()
                logStreamSseJob = null
                logStreamJob?.cancel()
                logStreamJob = null
            }
        }
    }

    MainBox {
        MainColumn {
            TitleRow(title = host?.name ?: "地图详情", onBack = onBack) {
                errorMessage?.let { ErrorText(it) }
                Space8w()
                host?.let { host ->
                    SimpleTooltip("地图的创建者") {
                        Text("\uEDEB ")
                        HeadButton(host.ownerId)
                    }
                    SimpleTooltip("在线玩家") {
                        Text(" · \uEC17 ${host.onlinePlayerIds.size} ")
                    }
                    Space8w()
                    host.onlinePlayerIds.forEach {
                        HeadButton(it, showName = false)
                    }
                    Space8w()
                    CircleIconButton(
                        icon = "\uF04B",
                        tooltip = "开始游玩",
                        bgColor = MaterialColor.GREEN_900.color
                    ) {
                        scope.rdiRequest<Host>(
                            path = "host/$hostId",
                            onOk = { response -> response.data?.startPlay() },
                            onErr = { errorMessage = "获取地图信息失败: ${it.message}" }
                        )
                    }
                    Space8w()
                    if (meAdmin) {
                        Space8w()
                    CircleIconButton(
                        icon = "\uF013",
                        tooltip = "设置"
                    ) {
                        optionDifficulty = host.difficulty
                        optionGameMode = host.gameMode
                        optionLevelType = host.levelType
                        optionWhitelist = host.whitelist
                        optionAllowCheats = host.allowCheats
                        showOptionsDialog = true
                    }
                        if (modpackDetail != null) {
                            Space8w()
                            CircleIconButton(
                                icon = "\uDB80\uDFD5",
                                tooltip = "更新"
                            ) { showUpdateConfirm = true }
                        }
                    }
                    if (meOwner) {
                        Space8w()
                        CircleIconButton(
                            icon = "\uEA81",
                            tooltip = "删除地图",
                            bgColor = MaterialColor.RED_900.color
                        ) { showDeleteConfirm = true }
                    }
                }
            }

            Space8h()

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                host == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage ?: "无法加载地图信息", color = MaterialColor.RED_900.color)
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("\uEB29 整合包 ${host.modpack.name} ${host.packVer}".asIconText)
                                Row{
                                    Text("\uF05F ${host.intro}".asIconText)
                                }
                                Row{
                                    Text("\uE384 ${host._id.timestamp.secondsToHumanDateTime}".asIconText)
                                }
                            }
                            modpackDetail?.let {
                                host.modpack.ModpackCard(
                                    modifier = Modifier.width(300.dp),
                                    onClick = { onOpenModpackInfo?.invoke(host.modpack.id.toHexString()) }
                                )
                            } ?: Text(
                                text = "找不到整合包",
                                color = MaterialColor.RED_900.color
                            )
                        }

                        val tabs = listOf("\uEF69 成员列表(${host.members.size}/10)", "\uDB80\uDD8D 后台", "\uE5FC 配置文件", "\uE615 基本信息")
                        TabRow(selectedTabIndex = selectedTab, backgroundColor = Color.White) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title.asIconText) }
                                )
                            }
                        }

                        when (selectedTab) {
                            0 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    host.members.forEach { member ->
                                        val memberColor = when (member.role) {
                                            Role.OWNER -> MaterialColor.YELLOW_900.color
                                            Role.ADMIN -> Color(0xFFC0C0C0)
                                            Role.MEMBER -> Color(0xFFCD7F32)
                                            else -> Color.White
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                HeadButton(
                                                    uid = member.id,
                                                    avatarSize = 28.dp,
                                                    showName = true
                                                )
                                                Space8w()
                                                Box(
                                                    modifier = Modifier
                                                        .background(memberColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = when (member.role) {
                                                            Role.OWNER -> "所有者"
                                                            Role.ADMIN -> "管理员"
                                                            Role.MEMBER -> "成员"
                                                            else -> ""
                                                        },
                                                        fontSize = 12.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (meOwner && member.role != Role.OWNER) {
                                                    TextButton(onClick = { transferConfirm = member.id }) {
                                                        Text("转让")
                                                    }
                                                }
                                                if (meOwner && member.role != Role.OWNER) {
                                                    val newRole = if (member.role == Role.ADMIN) Role.MEMBER else Role.ADMIN
                                                    TextButton(onClick = {
                                                        roleChangeConfirm = RoleChange(member.id, newRole)
                                                    }) {
                                                        Text(if (member.role == Role.ADMIN) "取消管理员" else "设为管理员")
                                                    }
                                                }
                                                if (meAdmin && member.role != Role.OWNER) {
                                                    TextButton(onClick = { kickConfirm = member.id }) {
                                                        Text("踢出", color = MaterialColor.RED_900.color)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (meAdmin) {
                                        TextButton(onClick = { showInviteDialog = true }) {
                                            Text("+ 邀请成员")
                                        }
                                    }
                                }
                            }
                            1 -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Console(state = consoleState, modifier = Modifier.fillMaxSize())
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        CircleIconButton(
                                            icon = "\uF04B",
                                            tooltip = "启动",
                                            bgColor = MaterialColor.GREEN_900.color
                                        ) {
                                            scope.rdiRequestU(
                                                path = "host/$hostId/start",
                                                method = HttpMethod.Post,
                                                onOk = { okMessage = "启动指令已发送" },
                                                onErr = { errorMessage = it.message ?: "启动失败" }
                                            )
                                        }
                                        CircleIconButton(
                                            icon = "\uF01E",
                                            tooltip = "重启",
                                            bgColor = MaterialColor.BLUE_800.color
                                        ) {
                                            restartConfirm = true
                                        }
                                        CircleIconButton(
                                            icon = "\uF04D",
                                            tooltip = "停止",
                                            bgColor = MaterialColor.RED_700.color
                                        ) {
                                            stopConfirm = true
                                        }
                                        CircleIconButton(
                                            icon = "\uF05E",
                                            tooltip = "强制停止",
                                            bgColor = MaterialColor.RED_900.color
                                        ) {
                                            forceStopConfirm = true
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Text("配置文件功能开发中", color = MaterialColor.GRAY_700.color)
                            }
                            else -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val difficultyText = when (host.difficulty) {
                                        0 -> "和平"
                                        1 -> "简单"
                                        2 -> "普通"
                                        3 -> "困难"
                                        else -> host.difficulty.toString()
                                    }
                                    val gameModeText = when (host.gameMode) {
                                        0 -> "生存"
                                        1 -> "创造"
                                        2 -> "冒险"
                                        3 -> "旁观"
                                        else -> host.gameMode.toString()
                                    }
                                    Text("难度：$difficultyText")
                                    Text("游戏模式：$gameModeText")
                                    Text("世界类型：${host.levelType}")
                                    Text("白名单：${if (host.whitelist) "开启" else "关闭"}")
                                    Text("允许作弊：${if (host.allowCheats) "开启" else "关闭"}")
                                    if (host.gameRules.isNotEmpty()) {
                                        Text("游戏规则覆盖：")
                                        host.gameRules.entries
                                            .sortedBy { it.key }
                                            .forEach { (rule, value) ->
                                                Text(" - $rule = $value")
                                            }
                                    } else {
                                        Text("游戏规则覆盖：无")
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

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "确认删除",
            message = "确认删除地图吗？\n（仅删除成员列表。\n存档数据不会被删除，可导出或重复利用）",
            onConfirm = {
                scope.rdiRequestU(
                    path = "host/$hostId",
                    method = HttpMethod.Delete,
                    onOk = {
                        okMessage = "已删除"
                        onBack()
                    },
                    onErr = { errorMessage = it.message ?: "删除失败" }
                )
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showUpdateConfirm && modpackDetail != null) {
        ConfirmDialog(
            title = "确认更新",
            message = "将更新地图当前的整合包《${modpackDetail!!.name}》到最新版本。",
            onConfirm = {
                scope.rdiRequestU(
                    path = "host/$hostId/update",
                    method = HttpMethod.Post,
                    onOk = {
                        okMessage = "已提交更新"
                        reload()
                    },
                    onErr = { errorMessage = it.message ?: "更新失败" }
                )
                showUpdateConfirm = false
            },
            onDismiss = { showUpdateConfirm = false }
        )
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("邀请成员") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请输入对方QQ号：")
                    OutlinedTextField(
                        value = inviteQq,
                        onValueChange = { inviteQq = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qq = inviteQq.trim()
                    if (qq.isBlank()) {
                        errorMessage = "QQ不能为空"
                        return@TextButton
                    }
                    scope.rdiRequestU(
                        path = "host/$hostId/member/$qq",
                        method = HttpMethod.Post,
                        onOk = {
                            okMessage = "已发送邀请"
                            reload()
                        },
                        onErr = { errorMessage = it.message ?: "邀请失败" }
                    )
                    showInviteDialog = false
                }) {
                    Text("邀请")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showOptionsDialog && host != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .zIndex(1f)
                .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease() }) }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.7f)
                    .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                TitleRow("地图设置", { showOptionsDialog = false }) {
                    CircleIconButton("\uF058", bgColor = MaterialColor.GREEN_900.color){
                        val body = serdesJson.encodeToString(
                            Host.OptionsDto(
                                difficulty = optionDifficulty,
                                gameMode = optionGameMode,
                                levelType = optionLevelType.ifBlank { null },
                                whitelist = optionWhitelist,
                                allowCheats = optionAllowCheats,
                                gameRules = if (gameRulesTouched) gameRuleOverrides.toMap() else null
                            )
                        )
                        scope.rdiRequestU(
                            path = "host/$hostId/options",
                            method = HttpMethod.Put,
                            body = body,
                            onOk = {
                                okMessage = "设置已保存"
                                reload()
                            },
                            onErr = { errorMessage = it.message ?: "保存失败" }
                        )
                        showOptionsDialog = false
                    }
                }
                Space8h()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp),verticalAlignment = Alignment.CenterVertically) {
                        Text("难度：")
                        listOf(
                            0 to "和平",
                            1 to "简单",
                            2 to "普通",
                            3 to "困难"
                        ).forEach { (value, label) ->
                                RadioButton(
                                    selected = optionDifficulty == value,
                                    onClick = { optionDifficulty = value }
                                )
                                Text(label)

                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp),verticalAlignment = Alignment.CenterVertically) {
                        Text("游戏模式：")
                        listOf(
                            0 to "生存",
                            1 to "创造",
                            2 to "冒险",
                            3 to "旁观"
                        ).forEach { (value, label) ->
                                RadioButton(
                                    selected = optionGameMode == value,
                                    onClick = { optionGameMode = value }
                                )
                                Text(label)

                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = optionWhitelist,
                            onCheckedChange = { optionWhitelist = it }
                        )
                        Text("白名单制 · 只有受邀成员允许游玩此地图")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = optionAllowCheats,
                            onCheckedChange = { optionAllowCheats = it }
                        )
                        Text("允许作弊")
                    }
                    TextButton(onClick = {
                        if (!gameRulesTouched) {
                            gameRuleOverrides.clear()
                            gameRuleOverrides.putAll(host.gameRules)
                            gameRulesTouched = true
                        }
                        showOptionsDialog = false
                        showGameRulesDialog = true
                    }) {
                        Text("游戏规则设定")
                    }
                }
            }
        }
    }

    GameRuleModal(
        show = showGameRulesDialog && host != null,
        overrideRules = gameRuleOverrides,
        onClose = { showGameRulesDialog = false },
        onBack = {
            showGameRulesDialog = false
            showOptionsDialog = true
        }
    )

    roleChangeConfirm?.let { change ->
        val msg = if (change.newRole == Role.ADMIN) "确定设置该成员为管理员？" else "确定取消管理员身份？"
        ConfirmDialog(
            title = "确认操作",
            message = msg,
            onConfirm = {
                scope.rdiRequestU(
                    path = "host/$hostId/member/${change.memberId}/role/${change.newRole.name}",
                    method = HttpMethod.Put,
                    onOk = {
                        okMessage = "已更新"
                        reload()
                    },
                    onErr = { errorMessage = it.message ?: "操作失败" }
                )
                roleChangeConfirm = null
            },
            onDismiss = { roleChangeConfirm = null }
        )
    }

    transferConfirm?.let { memberId ->
        ConfirmDialog(
            title = "确认转让",
            message = "确定将地图所有权转让给该成员吗？",
            onConfirm = {
                scope.rdiRequestU(
                    path = "host/$hostId/transfer/$memberId",
                    method = HttpMethod.Post,
                    onOk = {
                        okMessage = "已转让"
                        reload()
                    },
                    onErr = { errorMessage = it.message ?: "转让失败" }
                )
                transferConfirm = null
            },
            onDismiss = { transferConfirm = null }
        )
    }

    kickConfirm?.let { memberId ->
        ConfirmDialog(
            title = "确认踢出",
            message = "要踢出该成员吗？",
            onConfirm = {
                scope.rdiRequestU(
                    path = "host/$hostId/member/$memberId",
                    method = HttpMethod.Delete,
                    onOk = {
                        okMessage = "已踢出"
                        reload()
                    },
                    onErr = { errorMessage = it.message ?: "踢出失败" }
                )
                kickConfirm = null
            },
            onDismiss = { kickConfirm = null }
        )
    }

    if (restartConfirm) {
        ConfirmDialog(
            title = "确认重启",
            message = "确定重启该地图吗？",
            onConfirm = {
                scope.rdiRequestU(
                    path = "host/$hostId/restart",
                    method = HttpMethod.Post,
                    onOk = { okMessage = "重启指令已发送" },
                    onErr = { errorMessage = it.message ?: "重启失败" }
                )
                restartConfirm = false
            },
            onDismiss = { restartConfirm = false }
        )
    }

    if (stopConfirm) {
        ConfirmDialog(
            title = "确认停止",
            message = "确定停止该地图吗？",
            onConfirm = {
                scope.rdiRequestU(
                    path = "host/$hostId/stop",
                    method = HttpMethod.Post,
                    onOk = { okMessage = "停止指令已发送" },
                    onErr = { errorMessage = it.message ?: "停止失败" }
                )
                stopConfirm = false
            },
            onDismiss = { stopConfirm = false }
        )
    }

    if (forceStopConfirm) {
        ConfirmDialog(
            title = "确认强制停止",
            message = "确定强制停止该地图吗？",
            onConfirm = {
                scope.rdiRequestU(
                    path = "host/$hostId/force-stop",
                    method = HttpMethod.Post,
                    onOk = { okMessage = "强制停止指令已发送" },
                    onErr = { errorMessage = it.message ?: "强制停止失败" }
                )
                forceStopConfirm = false
            },
            onDismiss = { forceStopConfirm = false }
        )
    }
}

private data class RoleChange(
    val memberId: ObjectId,
    val newRole: Role
)
