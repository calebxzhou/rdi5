package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.common.model.AllGameRules
import calebxzhou.rdi.common.model.GameRule
import calebxzhou.rdi.common.model.GameRuleValueType
import calebxzhou.rdi.ui.*
import icyllis.modernui.text.Typeface
import icyllis.modernui.widget.EditText
import icyllis.modernui.widget.LinearLayout

class GameRulesFragment(private val overrideRules: MutableMap<String,String>) : RFragment("游戏规则设定") {
    override var fragSize = FragmentSize.LARGE

    private val baseRuleById = AllGameRules.associateBy { it.id }

    init {
        contentViewInit = {
            renderRules()
        }
        titleViewInit = {
            quickOptions {
                "重置" with {
                    resetOverrides()
                }
            }
        }
    }

    private fun renderRules() {
        val grouped = AllGameRules.groupBy { it.category }.toSortedMap()
        contentView.apply {
            removeAllViews()
            scrollView {
                linearLayout {
                    orientation = LinearLayout.VERTICAL
                    paddingDp(8)
                    grouped.forEach { (category, rules) ->
                        textView(category) {
                            setTextColor(0xFFEEEEEE.toInt())
                            textSize = 16f
                            paddingDp(4, 8, 4, 4)
                        }
                        rules.sortedBy { it.name }.forEach { rule ->
                            renderRuleCard(rule)
                        }
                    }
                }
            }
        }
    }

    private fun LinearLayout.renderRuleCard(rule: GameRule) {
        val currentValue = overrideRules[rule.id] ?: rule.value
        linearLayout {
            orientation = LinearLayout.VERTICAL
            layoutParams = linearLayoutParam(PARENT, SELF) {
                topMargin = context.dp(6f)
            }
            paddingDp(12)
            background = drawable { canvas ->
                val b = bounds
                val paint = icyllis.modernui.graphics.Paint.obtain()
                paint.color = 0x33181818
                paint.style = icyllis.modernui.graphics.Paint.Style.FILL.ordinal
                canvas.drawRoundRect(
                    b.left.toFloat(),
                    b.top.toFloat(),
                    b.right.toFloat(),
                    b.bottom.toFloat(),
                    context.dp(10f).toFloat(),
                    paint
                )
                paint.recycle()
            }

            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = context.dp(4f)
                }
                when (rule.valueType) {
                    GameRuleValueType.BOOLEAN -> {
                        checkBox("", init = { isChecked = currentValue.equals("true", true) }) { _, isChecked ->
                            updateOverride(rule, isChecked.toString())
                        }
                    }
                    GameRuleValueType.INTEGER -> {
                        var latestValue = currentValue
                        val field = editText("输入数值") {
                            setText(currentValue)
                            layoutParams = linearLayoutParam(dp(80f), dp(32f)) {

                            }
                        } as EditText

                        fun commit() {
                            val textValue = field.text.toString().trim()
                            val parsed = textValue.toIntOrNull()
                            if (parsed == null) {
                                field.setText(latestValue)
                                field.toast("请输入数字")
                                return
                            }
                            latestValue = parsed.toString()
                            updateOverride(rule, latestValue)
                        }

                        field.setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus) commit()
                        }
                        field.setOnKeyListener { _, keyCode, event ->
                            if (keyCode == icyllis.modernui.view.KeyEvent.KEY_ENTER && event.action == icyllis.modernui.view.KeyEvent.ACTION_UP) {
                                commit(); true
                            } else false
                        }
                    }
                }
                textView(rule.name) {
                    //layoutParams = linearLayoutParam(0, SELF) { weight = 1f }
                    setTextColor(0xFFFFFFFF.toInt())
                    textStyle = Typeface.BOLD
                    textSize = 15f
                }
                textView("    ")
                textView(rule.description) {
                    textStyle = Typeface.ITALIC
                    setTextColor(0xFFCCCCCC.toInt())
                    textSize = 15f
                }
            }
            rule.effect?.takeIf { it.isNotBlank() }?.let {
                textView(it) {
                    setTextColor(0xFF9EA7B3.toInt())
                    textSize = 13f
                    layoutParams = linearLayoutParam(PARENT, SELF) {
                        topMargin = context.dp(2f)
                    }
                }
            }




        }
    }

    private fun updateOverride(rule: GameRule, newValue: String) {
        val baseRule = baseRuleById[rule.id] ?: return
        val normalized = newValue.trim()
        if (normalized.equals(baseRule.value, ignoreCase = true) || normalized.isEmpty()) {
            overrideRules.remove(rule.id)
        } else {
            overrideRules[rule.id] = normalized
        }
        title = if (overrideRules.isEmpty()) "游戏规则设定" else "游戏规则设定（已修改${overrideRules.size}项）"
    }

    private fun resetOverrides() {
        overrideRules.clear()
        title = "游戏规则设定"
        renderRules()
    }
}