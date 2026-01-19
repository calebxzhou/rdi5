package calebxzhou.rdi.common.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

data class TaskProgress(
    val message: String,
    val fraction: Float? = null
)

class TaskContext(
    val emitProgress: (TaskProgress) -> Unit,
    val isCancelled: () -> Boolean = { false }
)

sealed class Task {
    abstract val name: String

    data class Leaf(
        override val name: String,
        val execute: suspend (TaskContext) -> Unit
    ) : Task()

    data class Group(
        override val name: String,
        val subTasks: List<Task> = emptyList(),
        val parallelism: Int = Runtime.getRuntime().availableProcessors()
    ) : Task()

    data class Sequence(
        override val name: String,
        val subTasks: List<Task> = emptyList()
    ) : Task()
}

suspend fun Task.execute( ctx: TaskContext) {
    val task = this
    when (task) {
        is Task.Leaf -> task.execute(ctx)
        is Task.Group -> {
            val total = task.subTasks.size.coerceAtLeast(1)
            val completed = AtomicInteger(0)
            val semaphore = Semaphore(task.parallelism.coerceAtLeast(1))
            coroutineScope {
                task.subTasks.map { subTask ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            subTask.execute(ctx)
                        }
                        val done = completed.incrementAndGet()
                        ctx.emitProgress(TaskProgress("已完成 $done/$total", done.toFloat() / total))
                    }
                }.forEach { it.await() }
            }
        }
        is Task.Sequence -> {
            val total = task.subTasks.size.coerceAtLeast(1)
            task.subTasks.forEachIndexed { index, subTask ->
                subTask.execute(ctx)
                val done = index + 1
                ctx.emitProgress(TaskProgress("已完成 $done/$total", done.toFloat() / total))
            }
        }
    }
}
