package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.common.util.ioScope
import calebxzhou.rdi.lgr
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.Console
import icyllis.modernui.view.Gravity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TaskFragment(
	val name: String,
	private val task: suspend TaskFragment.() -> Unit
) : RFragment(name) {

	private lateinit var console: Console
	private var taskJob: Job? = null

	init {
		closable = false
		showCloseButton = false

		contentViewInit = {
			gravity = Gravity.CENTER
			console = Console(fctx)
			console.layoutParams = linearLayoutParam(PARENT, PARENT)
			addView(console)
			startTaskOnce()
		}
	}

	private fun startTaskOnce() {
		if (taskJob != null) return
		taskJob = ioScope.launch {
			runCatching { task() }
				.onSuccess {
					log("任务完成")
					uiThread { closeSafely() }
				}
				.onFailure { error ->
					lgr.error(error) { "Task $name failed" }
					log("任务失败: ${error::class.qualifiedName + error.message }")
					uiThread { closeSafely() }
				}
		}
	}

	fun log(line: String)=uiThread {
		console.append(line)
	}

	private fun closeSafely() {
		closable = true
        title=("任务[$name]完成，按ESC键退出")
	}
}