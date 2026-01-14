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
import calebxzhou.rdi.client.Const
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.AllGameRules
import calebxzhou.rdi.common.model.GameRule
import calebxzhou.rdi.common.model.GameRuleValueType
import calebxzhou.rdi.common.model.World
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    arg: HostCreate
) {
    val scope = rememberCoroutineScope()
    val overrideRules = remember { mutableStateMapOf<String, String>() }

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

    LaunchedEffect(arg.modpackId, arg.packVer) {
        loading = true
        errorMessage = null
        val response = withContext(Dispatchers.IO) {
            runCatching { server.makeRequest<List<World>>("world") }.getOrNull()
        }
        if (response == null) {
            errorMessage = "加载存档失败"
        } else if (!response.ok) {
            errorMessage = response.msg
        } else {
            worlds = response.data ?: emptyList()
        }
        loading = false
    }

    val worldOptions = remember(worlds) {
        buildList {
            worlds.forEachIndexed { index, world ->
                val label = "${world.name} (${(world._id.timestamp * 1000L).millisToHumanDateTime})"
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

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("创建新地图", style = MaterialTheme.typography.h5)
        Text("整合包：${arg.modpackName} V${arg.packVer}", style = MaterialTheme.typography.body1)

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
                    submitting = true
                    statusMessage = null
                    scope.launch {
                        val selectedWorld = worldOptions.firstOrNull { it.id == selectedWorldId }?.world
                        val params = mutableMapOf<String, Any>(
                            "modpackId" to arg.modpackId,
                            "packVer" to arg.packVer,
                            "difficulty" to difficulty,
                            "gameMode" to gameMode,
                            "levelType" to levelType,
                            "gameRules" to overrideRules.toMap().json
                        )
                        if (selectedWorld != null) {
                            params += "useWorld" to selectedWorld._id
                            params += "worldOpr" to "use"
                        } else if (selectedWorldId == ID_CREATE_NEW_SAVE) {
                            params += "worldOpr" to "create"
                        } else if (selectedWorldId == ID_NO_SAVE) {
                            params += "worldOpr" to "no"
                        }

                        val response = withContext(Dispatchers.IO) {
                            runCatching {
                                server.makeRequest<Unit>("host", HttpMethod.Post, params)
                            }.getOrNull()
                        }

                        submitting = false
                        if (response == null) {
                            statusMessage = "创建失败，请检查网络"
                        } else if (!response.ok) {
                            statusMessage = response.msg
                        } else {
                            showResult = "已提交创建请求 完成后信箱通知你"
                        }
                    }
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

@Composable
private fun GameRulesDialog(
    overrideRules: MutableMap<String, String>,
    onDismiss: () -> Unit
) {
    val grouped = remember { AllGameRules.groupBy { it.category }.toSortedMap() }
    val items = remember {
        val list = mutableListOf<Pair<String?, GameRule?>>()
        grouped.forEach { (category, rules) ->
            list += category to null
            rules.sortedBy { it.name }.forEach { rule ->
                list += null to rule
            }
        }
        list
    }

    Dialog(onCloseRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
            color = MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("游戏规则设定", style = MaterialTheme.typography.h6)
                    TextButton(onClick = { overrideRules.clear() }) {
                        Text("重置")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { entry ->
                        val category = entry.first
                        val rule = entry.second
                        if (category != null) {
                            Text(category, style = MaterialTheme.typography.subtitle1)
                        } else if (rule != null) {
                            GameRuleItem(rule, overrideRules)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun GameRuleItem(
    rule: GameRule,
    overrideRules: MutableMap<String, String>
) {
    val currentValue = overrideRules[rule.id] ?: rule.value
    var textValue by remember(rule.id) { mutableStateOf(currentValue) }

    LaunchedEffect(currentValue) {
        if (textValue != currentValue) {
            textValue = currentValue
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (rule.valueType) {
                    GameRuleValueType.BOOLEAN -> {
                        Checkbox(
                            checked = currentValue.equals("true", ignoreCase = true),
                            onCheckedChange = { checked ->
                                updateOverride(overrideRules, rule, checked.toString())
                            }
                        )
                    }
                    GameRuleValueType.INTEGER -> {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { newValue ->
                                val sanitized = newValue.filter { it.isDigit() }
                                textValue = sanitized
                                if (sanitized.isBlank()) {
                                    overrideRules.remove(rule.id)
                                } else {
                                    updateOverride(overrideRules, rule, sanitized)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.name, style = MaterialTheme.typography.subtitle1)
                    Text(rule.description, style = MaterialTheme.typography.body2)
                }
            }
            rule.effect?.takeIf { it.isNotBlank() }?.let { effect ->
                Text(effect, style = MaterialTheme.typography.caption)
            }
        }
    }
}

private fun updateOverride(
    overrideRules: MutableMap<String, String>,
    rule: GameRule,
    newValue: String
) {
    val normalized = newValue.trim()
    if (normalized.isEmpty() || normalized.equals(rule.value, ignoreCase = true)) {
        overrideRules.remove(rule.id)
    } else {
        overrideRules[rule.id] = normalized
    }
}
