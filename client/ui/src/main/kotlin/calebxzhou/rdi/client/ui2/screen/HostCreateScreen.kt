package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.secondsToHumanDateTime
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.rdiRequest
import calebxzhou.rdi.client.net.rdiRequestU
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.GameRuleModal
import calebxzhou.rdi.client.ui2.comp.ImageCard
import calebxzhou.rdi.client.ui2.comp.WorldCard
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.common.model.World
import calebxzhou.rdi.common.serdesJson
import io.ktor.http.*
import org.bson.types.ObjectId
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostCreateScreen(
    arg: HostCreate,
    //host: Host.DetailVo, // if options mode
    onBack: () -> Unit,
    onNavigateProfile: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val overrideRules = remember { mutableStateMapOf<String, String>() }
    var title by remember { mutableStateOf("使用整合包 ${arg.modpackName} ${arg.packVer} 创建新地图") }
    var hostName by remember { mutableStateOf("${loggedAccount.name}的世界${Random.nextInt(1000)}") }
    var modpackIdText by remember { mutableStateOf(arg.modpackId) }
    var packVerText by remember { mutableStateOf(arg.packVer) }
    var worlds by remember { mutableStateOf<List<World.Vo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRules by remember { mutableStateOf(false) }
    var noSave by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var editHostId by remember { mutableStateOf<ObjectId?>(null) }
    var editWorldId by remember { mutableStateOf<ObjectId?>(null) }
    var editPreferNoSave by remember { mutableStateOf(false) }

    var selectedWorldId by remember { mutableStateOf<ObjectId?>(null) }
    var difficulty by remember { mutableStateOf(2) }
    var gameMode by remember { mutableStateOf(0) }
    var levelType by remember { mutableStateOf(if (arg.skyblock) "skyblockbuilder:skyblock" else "minecraft:normal") }
    var levelChoice by remember { mutableStateOf(if (arg.skyblock) 2 else 0) }
    var whitelist by remember { mutableStateOf(false) }
    var allowCheats by remember { mutableStateOf(false) }
    fun isEditMode() = editHostId != null
    LaunchedEffect(arg.hostId) {
        val rawHostId = arg.hostId?.trim()
        if (rawHostId.isNullOrBlank() || !ObjectId.isValid(rawHostId)) return@LaunchedEffect
        editHostId = ObjectId(rawHostId)
        loading = true
        scope.rdiRequest<Host.DetailVo>(
            path = "host/$rawHostId/detail",
            onOk = { resp ->
                val detail = resp.data ?: return@rdiRequest
                title = "地图设置 · ${detail.name}"
                hostName = detail.name
                modpackIdText = detail.modpack.id.toHexString()
                packVerText = detail.packVer
                difficulty = detail.difficulty
                gameMode = detail.gameMode
                levelType = detail.levelType
                whitelist = detail.whitelist
                allowCheats = detail.allowCheats
                if (detail.worldId == null) {
                    editPreferNoSave = true
                } else {
                    editWorldId = detail.worldId
                }
                overrideRules.clear()
                overrideRules.putAll(detail.gameRules)
            },
            onErr = { errorMessage = "无法加载地图信息: ${it.message}" },
            onDone = { loading = false }
        )
    }
    LaunchedEffect(Unit) {
        loading = true
        scope.rdiRequest<List<World.Vo>>(
            "world",
            onErr = { "无法载入区块数据: ${it.message}" },
            onOk = { resp ->
                worlds = resp.data ?: emptyList()
            },
            onDone = { loading = false }
        )
    }

    LaunchedEffect(worlds, editWorldId, editPreferNoSave) {
        val targetWorldId = editWorldId
        if (targetWorldId != null) {
            selectedWorldId = worlds.firstOrNull { it.id == targetWorldId }?.id
            editWorldId = null
        }
        if (editPreferNoSave) {
            noSave = true
            selectedWorldId = null
            editPreferNoSave = false
        }
    }
    fun submit() {
        if (submitting) return
        statusMessage = null
        val trimmedName = hostName.trim()
        if (trimmedName.isEmpty()) {
            statusMessage = "请输入地图名称"
            return
        }
        val trimmedModpackId = modpackIdText.trim()
        val trimmedPackVer = packVerText.trim()
        if (editHostId == null) {
            if (!ObjectId.isValid(trimmedModpackId)) {
                statusMessage = "整合包 ID 格式不正确"
                return
            }
            if (trimmedPackVer.isEmpty()) {
                statusMessage = "请输入整合包版本"
                return
            }

        }
        submitting = true
        val selectedWorld = selectedWorldId?.let { id -> worlds.firstOrNull { it.id == id } }
        val saveWorld = !noSave
        val worldId = when {
            !noSave && selectedWorld != null -> selectedWorld.id
            else -> null
        }
        val hostId = editHostId
        if (hostId != null) {
            val optionsDto = Host.OptionsDto(
                name = trimmedName,
                saveWorld = saveWorld,
                worldId = worldId,
                difficulty = difficulty,
                gameMode = gameMode,
                levelType = levelType,
                allowCheats = allowCheats,
                whitelist = whitelist,
                gameRules = overrideRules.toMutableMap()
            )
            val body = serdesJson.encodeToString(optionsDto)
            scope.rdiRequestU(
                path = "host/${hostId.toHexString()}/options",
                method = HttpMethod.Put,
                body = body,
                onErr = { statusMessage = "保存失败: ${it.message}" },
                onOk = { showResult = "设置已保存" },
                onDone = { submitting = false }
            )
        } else {
            val createDto = Host.CreateDto(
                name = trimmedName,
                modpackId = ObjectId(trimmedModpackId),
                packVer = trimmedPackVer,
                saveWorld = saveWorld,
                worldId = worldId,
                difficulty = difficulty,
                gameMode = gameMode,
                levelType = levelType,
                allowCheats = allowCheats,
                whitelist = whitelist,
                gameRules = overrideRules.toMutableMap()
            )
            val body = serdesJson.encodeToString(createDto)
            scope.rdiRequestU(
                path = "host/v2",
                method = HttpMethod.Post,
                body = body,
                onErr = { statusMessage = "创建失败: ${it.message}" },
                onOk = { showResult = "已提交创建请求 完成后信箱通知你" },
                onDone = { submitting = false }
            )
        }
    }
    MainColumn {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TitleRow(title, onBack) {
                statusMessage?.let {
                    Text(it, color = MaterialTheme.colors.error)
                }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colors.error)
                }
                if (loading) {
                    CircularProgressIndicator()
                }
                CircleIconButton("\uDB82\uDE50", bgColor = MaterialColor.GREEN_900.color) {
                    submit()
                }
            }
            Space8h()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                Row {
                    Column {
                        Row {
                            Text("地图名称", fontWeight = FontWeight.Bold)
                            Space24w()
                            Text("将会在地图大厅界面显示。立刻生效", color = MaterialColor.GRAY_500.color)
                        }
                        Space8h()
                        OutlinedTextField(
                            label = { Text("地图名称") },
                            value = hostName,
                            onValueChange = { hostName = it },
                            singleLine = true,
                        )
                    }
                    Space24w()
                    Space24w()
                    Column {
                        Row {
                            Text("其他设置", fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(whitelist, { whitelist = it })
                            Text("白名单制")
                            Space24w()
                            Text("只有受邀玩家允许游玩", color = MaterialColor.GRAY_500.color)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(allowCheats, { allowCheats = it })
                            Text("允许作弊")
                            Space24w()
                            Button(onClick = { showRules = true }) {
                                Text("设置游戏规则  (已改${overrideRules.size}项)")
                            }
                        }
                    }
                }

                Space8h()
                Row {
                    Column {
                        Text("难度", fontWeight = FontWeight.Bold)
                        Space8h()
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            ImageCard(
                                title = "和平",
                                desc = "无敌对生物",
                                iconPath = "assets/icons/difficulty_peaceful.png",
                                selected = difficulty == 0,
                                onClick = { difficulty = 0 }
                            )
                            ImageCard(
                                title = "简单",
                                desc = "有敌对生物 伤害较低",
                                iconPath = "assets/icons/difficulty_easy.png",
                                selected = difficulty == 1,
                                onClick = { difficulty = 1 }
                            )
                            ImageCard(
                                title = "普通",
                                desc = "有敌对生物 中等伤害",
                                iconPath = "assets/icons/difficulty_normal.png",
                                selected = difficulty == 2,
                                onClick = { difficulty = 2 }
                            )
                            ImageCard(
                                title = "困难",
                                desc = "有敌对生物 更高伤害",
                                iconPath = "assets/icons/difficulty_hard.png",
                                selected = difficulty == 3,
                                onClick = { difficulty = 3 }
                            )
                        }
                    }
                    Column {
                        Text("模式", fontWeight = FontWeight.Bold)
                        Space8h()
                        Row() {
                            ImageCard(
                                title = "生存模式",
                                desc = "探索一个神秘的世界",
                                iconPath = "assets/icons/gamemode_survival.png",
                                selected = gameMode == 0,
                                onClick = { gameMode = 0 }
                            )
                            ImageCard(
                                title = "创造模式",
                                desc = "无限制地建造和探索",
                                iconPath = "assets/icons/gamemode_creative.png",
                                selected = gameMode == 1,
                                onClick = { gameMode = 1 }
                            )
                        }
                    }
                    Column {
                        Text("地形", fontWeight = FontWeight.Bold)
                        Space8h()
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (arg.skyblock) {
                                ImageCard(
                                    title = "空岛",
                                    desc = "漂浮小岛 开局资源稀少",
                                    iconPath = "assets/icons/worldtype_skyblock.jpg",
                                    selected = levelChoice == 2,
                                    onClick = {
                                        levelChoice = 2
                                        levelType = "skyblockbuilder:skyblock"
                                    }
                                )
                            } else {
                                ImageCard(
                                    title = "普通",
                                    desc = "最多生物群系的维度",
                                    iconPath = "assets/icons/worldtype_normal.png",
                                    selected = levelChoice == 0,
                                    onClick = {
                                        levelChoice = 0
                                        levelType = "minecraft:normal"
                                    }
                                )
                                ImageCard(
                                    title = "超平坦",
                                    desc = "完全平坦的表面",
                                    iconPath = "assets/icons/worldtype_flat.png",
                                    selected = levelChoice == 1,
                                    onClick = {
                                        levelChoice = 1
                                        levelType = "minecraft:flat"
                                    }
                                )
                            }
                        }
                    }
                }

                Row {
                    Column(modifier = Modifier.weight(0.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("选择要使用的区块数据", fontWeight = FontWeight.Bold)
                            Space24w()
                            if (worlds.size < 5) {
                                RadioButton(
                                    selected = selectedWorldId == null && !noSave,
                                    onClick = {
                                        noSave = false
                                        selectedWorldId = null
                                    }
                                )
                                Text("创建一份新的区块数据")
                            }
                            RadioButton(
                                selected = selectedWorldId == null && noSave,
                                onClick = {
                                    selectedWorldId = null
                                    noSave = true
                                }
                            )
                            Text("不保存任何区块数据")
                            Space24w()
                            if (noSave) {
                                Text("仅限测试整合包使用 谨慎选择", color = MaterialColor.RED_900.color)
                            }
                        }
                        Space8h()
                        val worldItems = worlds
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 260.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 520.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(worldItems) { world ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (world.id == selectedWorldId) 2.dp else 1.dp,
                                            color = if (world.id == selectedWorldId) {
                                                MaterialColor.PURPLE_500.color
                                            } else {
                                                MaterialColor.GRAY_200.color
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .padding(2.dp)
                                ) {
                                    world.WorldCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            selectedWorldId = world.id
                                            noSave = false
                                        }
                                    )
                                }
                            }
                        }

                    }

                }
            }
            Space8h()

        }
    }

    if (showRules) {
        GameRuleModal(
            show = true,
            overrideRules = overrideRules,
            onClose = { showRules = false }
        )
    }

    showResult?.let { message ->
        AlertDialog(
            onDismissRequest = {
                showResult = null
            },
            title = { Text(if (editHostId != null) "设置已保存" else "创建请求已提交") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    showResult = null
                    if (editHostId != null) {
                        onBack()
                    } else {
                        onNavigateProfile()
                    }
                }) {
                    Text("确定")
                }
            }
        )
    }

}
