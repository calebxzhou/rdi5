package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.Space8h
import calebxzhou.rdi.client.ui2.Space8w
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.common.model.AllGameRules
import calebxzhou.rdi.common.model.GameRuleValueType
import java.text.Collator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRuleModal(
    show: Boolean,
    overrideRules: MutableMap<String, String>,
    onClose: () -> Unit,
    onBack: (() -> Unit)? = null,
    zIndex: Float = 2f
) {
    if (!show) return

    val groupedRules = remember {
        val collator = Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
        AllGameRules.groupBy { it.category }
            .toSortedMap(compareBy(collator) { it })
    }
    val baseRuleById = remember { AllGameRules.associateBy { it.id } }
    val changedCount = overrideRules.size

    fun updateOverride(ruleId: String, newValue: String) {
        val baseRule = baseRuleById[ruleId] ?: return
        val normalized = newValue.trim()
        if (normalized.equals(baseRule.value, ignoreCase = true) || normalized.isEmpty()) {
            overrideRules.remove(ruleId)
        } else {
            overrideRules[ruleId] = normalized
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .zIndex(zIndex)
            .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease() }) }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .height(560.dp)
                .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            val title = if (changedCount > 0) {
                "游戏规则设定（${changedCount}项已更改）"
            } else {
                "游戏规则设定"
            }
            TitleRow(title, { (onBack ?: onClose).invoke() }) {
                CircleIconButton("\uDB81\uDC50", "重置") { overrideRules.clear() }
            }
            Space8h()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(320.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                groupedRules.forEach { (category, rules) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(category, fontSize = 15.sp)
                    }
                    items(rules.sortedBy { it.name }, key = { it.id }) { rule ->
                        val currentValue = overrideRules[rule.id] ?: rule.value
                        val isChanged = overrideRules.containsKey(rule.id)
                        val cardColor = if (isChanged) Color(0xFFEDEDED) else Color.White
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = cardColor,
                            elevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (rule.valueType) {
                                        GameRuleValueType.BOOLEAN -> {
                                            Checkbox(
                                                checked = currentValue.equals("true", true),
                                                onCheckedChange = { checked ->
                                                    updateOverride(rule.id, checked.toString())
                                                }
                                            )
                                        }
                                        GameRuleValueType.INTEGER -> {
                                            var text by remember(rule.id, currentValue) {
                                                mutableStateOf(currentValue)
                                            }
                                            OutlinedTextField(
                                                value = text,
                                                onValueChange = { input ->
                                                    text = input
                                                    val parsed = input.trim().toIntOrNull()
                                                    if (parsed != null) {
                                                        updateOverride(rule.id, parsed.toString())
                                                    } else if (input.isBlank()) {
                                                        updateOverride(rule.id, "")
                                                    }
                                                },
                                                label = { Text("数值") },
                                                singleLine = true,
                                                modifier = Modifier.width(96.dp)
                                            )
                                        }
                                    }
                                    Space8w()
                                    Text(rule.name, fontSize = 14.sp)
                                }
                                Text(
                                    rule.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                )
                                rule.effect?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, fontSize = 12.sp, color = MaterialColor.GRAY_700.color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
