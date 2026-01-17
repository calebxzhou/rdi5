package calebxzhou.rdi.common.model

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
