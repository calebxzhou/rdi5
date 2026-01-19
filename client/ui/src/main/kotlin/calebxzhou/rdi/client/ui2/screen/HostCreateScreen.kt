package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import calebxzhou.mykotutils.std.millisToHumanDateTime
import calebxzhou.mykotutils.std.secondsToHumanDateTime
import calebxzhou.rdi.client.Const
import calebxzhou.rdi.client.net.rdiRequest
import calebxzhou.rdi.client.net.rdiRequestU
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.common.model.AllGameRules
import calebxzhou.rdi.common.model.GameRule
import calebxzhou.rdi.common.model.GameRuleValueType
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.common.model.World
import calebxzhou.rdi.common.serdesJson
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

private const val ID_CREATE_NEW_SAVE = 100
private const val ID_NO_SAVE = 101

private data class WorldOption(
    val id: Int,
    val label: String,
    val world: World?
)

@Composable
fun HostCreateScreen(
    arg: HostCreate,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val overrideRules = remember { mutableStateMapOf<String, String>() }

    var hostName by remember { mutableStateOf("") }
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

    var selectedWorldId by remember { mutableStateOf(0) }
    var difficulty by remember { mutableStateOf(2) }
    var gameMode by remember { mutableStateOf(0) }
    var levelType by remember { mutableStateOf(if (arg.skyblock) "skyblockbuilder:skyblock" else "minecraft:normal") }
    var levelChoice by remember { mutableStateOf(if (arg.skyblock) 2 else 0) }

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
                add(WorldOption(index, label, world))
            }
            if (worlds.size < 5) {
                add(WorldOption(ID_CREATE_NEW_SAVE, "创建新存档", null))
            }
            add(WorldOption(ID_NO_SAVE, "不存档", null))
        }
    }

    LaunchedEffect(worldOptions) {
        if (worldOptions.isNotEmpty() && worldOptions.none { it.id == selectedWorldId }) {
            selectedWorldId = worldOptions.first().id
        }
    }

    MainColumn {
        TitleRow("创建新地图",onBack){

        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("主机名称")
            OutlinedTextField(
                value = hostName,
                onValueChange = { hostName = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("整合包：${arg.modpackName} ${arg.packVer}")
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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("选择存档数据")
            worldOptions.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = option.id == selectedWorldId,
                        onClick = {
                            selectedWorldId = option.id
                            if (option.id == ID_NO_SAVE && !Const.DEBUG) {
                                showNoSaveWarn = true
                            }
                        }
                    )
                    Text(option.label)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("难度")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DifficultyOption("和平", 0, difficulty) { difficulty = it }
                DifficultyOption("简单", 1, difficulty) { difficulty = it }
                DifficultyOption("普通", 2, difficulty) { difficulty = it }
                DifficultyOption("困难", 3, difficulty) { difficulty = it }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("模式")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DifficultyOption("生存", 0, gameMode) { gameMode = it }
                DifficultyOption("创造", 1, gameMode) { gameMode = it }
                DifficultyOption("冒险", 2, gameMode) { gameMode = it }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showRules = true }) {
                Text("设置游戏规则")
            }
            if (overrideRules.isNotEmpty()) {
                Text("已修改 ${overrideRules.size} 项", style = MaterialTheme.typography.caption)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        statusMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                enabled = !submitting,
                onClick = {
                    if (submitting) return@Button
                    statusMessage = null
                    val trimmedName = hostName.trim()
                    if (trimmedName.isEmpty()) {
                        statusMessage = "请输入主机名称"
                        return@Button
                    }
                    val trimmedModpackId = modpackIdText.trim()
                    if (!ObjectId.isValid(trimmedModpackId)) {
                        statusMessage = "整合包 ID 格式不正确"
                        return@Button
                    }
                    val trimmedPackVer = packVerText.trim()
                    if (trimmedPackVer.isEmpty()) {
                        statusMessage = "请输入整合包版本"
                        return@Button
                    }
                    submitting = true
                    val selectedWorld = worldOptions.firstOrNull { it.id == selectedWorldId }?.world
                    val saveWorld = selectedWorldId != ID_NO_SAVE
                    val worldId = when {
                        selectedWorld != null -> selectedWorld._id
                        selectedWorldId == ID_CREATE_NEW_SAVE -> null
                        else -> null
                    }
                    val createDto = Host.CreateDto(
                        name = trimmedName,
                        modpackId = ObjectId(trimmedModpackId),
                        packVer = trimmedPackVer,
                        saveWorld = saveWorld,
                        worldId = worldId,
                        difficulty = difficulty,
                        gameMode = gameMode,
                        levelType = levelType,
                        allowCheats = false,
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
            ) {
                Text(if (submitting) "创建中..." else "创建")
            }
        }
    }

    if (showRules) {
        GameRulesDialog(
            overrideRules = overrideRules,
            onDismiss = { showRules = false }
        )
    }

    if (showNoSaveWarn) {
        AlertDialog(
            onDismissRequest = { showNoSaveWarn = false },
            title = { Text("注意") },
            text = { Text("如果不用存档，所有的地图、背包内容都不保存，\n关服后自动删除，仅供测试使用。\n谨慎选择！") },
            confirmButton = {
                TextButton(onClick = { showNoSaveWarn = false }) {
                    Text("明白")
                }
            }
        )
    }

    showResult?.let { message ->
        AlertDialog(
            onDismissRequest = {
                showResult = null
            },
            title = { Text("创建请求已提交") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    showResult = null
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

