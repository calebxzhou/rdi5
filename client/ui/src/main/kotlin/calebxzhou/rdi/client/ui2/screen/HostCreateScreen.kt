package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.secondsToHumanDateTime
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.rdiRequest
import calebxzhou.rdi.client.net.rdiRequestU
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.GameRuleModal
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
    var worlds by remember { mutableStateOf<List<World>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRules by remember { mutableStateOf(false) }
    var showNoSaveWarn by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var editHostId by remember { mutableStateOf<ObjectId?>(null) }
    var editWorldId by remember { mutableStateOf<ObjectId?>(null) }
    var editPreferNoSave by remember { mutableStateOf(false) }

    var selectedWorldId by remember { mutableStateOf(0) }
    var difficulty by remember { mutableStateOf(2) }
    var gameMode by remember { mutableStateOf(0) }
    var levelType by remember { mutableStateOf(if (arg.skyblock) "skyblockbuilder:skyblock" else "minecraft:normal") }
    var levelChoice by remember { mutableStateOf(if (arg.skyblock) 2 else 0) }
    var whitelist by remember { mutableStateOf(false) }
    var allowCheats by remember { mutableStateOf(false) }

    LaunchedEffect(arg.hostId) {
        val rawHostId = arg.hostId?.trim()
        if (rawHostId.isNullOrBlank() || !ObjectId.isValid(rawHostId)) return@LaunchedEffect
        editHostId = ObjectId(rawHostId)
        loading = true
        scope.rdiRequest<Host.DetailVo>(
            path = "host/$rawHostId/detail",
            onOk = { resp ->
                val detail = resp.data ?: return@rdiRequest
                title = "编辑地图 ${detail.name}"
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
        scope.rdiRequest<List<World>>(
            "world",
            onErr = { "无法载入存档数据: ${it.message}" },
            onOk = { resp ->
                worlds = resp.data ?: emptyList()
            },
            onDone = { loading = false }
        )
    }

    val worldOptions = remember(worlds) {
        buildList {
            worlds.forEachIndexed { index, world ->
                val label = "${world.name} (${(world._id.timestamp).secondsToHumanDateTime})"
                add(HostWorldOption(index, label, world))
            }
            if (worlds.size < 5) {
                add(HostWorldOption(HOST_CREATE_NEW_SAVE_ID, "创建新存档", null))
            }
            add(HostWorldOption(HOST_NO_SAVE_ID, "不存档", null))
        }
    }

    LaunchedEffect(worldOptions, editWorldId, editPreferNoSave) {
        if (worldOptions.isNotEmpty() && worldOptions.none { it.id == selectedWorldId }) {
            selectedWorldId = worldOptions.first().id
        }
        val targetWorldId = editWorldId
        if (targetWorldId != null) {
            selectedWorldId = worldOptions.firstOrNull { it.world?._id == targetWorldId }?.id
                ?: HOST_NO_SAVE_ID
            editWorldId = null
        }
        if (editPreferNoSave) {
            selectedWorldId = HOST_NO_SAVE_ID
            editPreferNoSave = false
        }
    }
    fun submit(){
        if (submitting) return
        statusMessage = null
        val trimmedName = hostName.trim()
        if (trimmedName.isEmpty()) {
            statusMessage = "请输入主机名称"
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
        val selectedWorld = worldOptions.firstOrNull { it.id == selectedWorldId }?.world
        val saveWorld = selectedWorldId != HOST_NO_SAVE_ID
        val worldId = when {
            selectedWorld != null -> selectedWorld._id
            selectedWorldId == HOST_CREATE_NEW_SAVE_ID -> null
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
        TitleRow(title,onBack){
            CircleIconButton("\uDB82\uDE50", bgColor = MaterialColor.GREEN_900.color){
                submit()
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                label = {Text("主机名称")},
                value = hostName,
                onValueChange = { hostName = it },
                singleLine = true,
                modifier = 300.wM
            )
        }
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
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(0.5f)) {
                Text("选择存档数据")
                worldOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option.id == selectedWorldId,
                            onClick = {
                                selectedWorldId = option.id
                                if (option.id == HOST_NO_SAVE_ID) {
                                    showNoSaveWarn = true
                                }
                            }
                        )
                        Text(option.label)
                    }
                }
                if (showNoSaveWarn) {
                    Text(
                        "不保留任何存档数据，仅供测试使用！",
                        color = MaterialColor.YELLOW_900.color
                    )
                }
            }
            Column(modifier = Modifier.weight(0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("难度")
                    DifficultyOption("和平", 0, difficulty) { difficulty = it }
                    DifficultyOption("简单", 1, difficulty) { difficulty = it }
                    DifficultyOption("普通", 2, difficulty) { difficulty = it }
                    DifficultyOption("困难", 3, difficulty) { difficulty = it }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("模式")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DifficultyOption("生存", 0, gameMode) { gameMode = it }
                        DifficultyOption("创造", 1, gameMode) { gameMode = it }
                        DifficultyOption("冒险", 2, gameMode) { gameMode = it }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("地形")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (arg.skyblock) {
                            DifficultyOption("空岛", 2, levelChoice) {
                                levelChoice = it
                                levelType = "skyblockbuilder:skyblock"
                            }
                        } else {
                            DifficultyOption("普通", 0, levelChoice) {
                                levelChoice = it
                                levelType = "minecraft:normal"
                            }
                            DifficultyOption("超平坦", 1, levelChoice) {
                                levelChoice = it
                                levelType = "minecraft:flat"
                            }
                        }
                    }
                }
                //whitelist
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(whitelist, { whitelist = it })
                    Text("白名单 · 只有受邀成员能够游玩")
                }

                //cheat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(allowCheats, { allowCheats = it })
                    Text("允许作弊")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showRules = true }) {
                        Text("设置游戏规则")
                    }
                    if (overrideRules.isNotEmpty()) {
                        Text("已修改 ${overrideRules.size} 项", style = MaterialTheme.typography.caption)
                    }
                }
            }
        }




        Spacer(modifier = Modifier.weight(1f))

        statusMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
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

@Composable
private fun DifficultyOption(
    label: String,
    value: Int,
    selectedValue: Int,
    onSelected: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = value == selectedValue,
            onClick = { onSelected(value) }
        )
        Text(label)
    }
}

const val HOST_CREATE_NEW_SAVE_ID = 100
const val HOST_NO_SAVE_ID = 101

data class HostWorldOption(
    val id: Int,
    val label: String,
    val world: World?
)

