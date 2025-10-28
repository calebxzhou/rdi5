package calebxzhou.rdi

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * calebxzhou @ 2025-10-28 10:36
 */
val ioScope: CoroutineScope
    get() = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    )

fun ioTask(handler: suspend () -> Unit) = ioScope.launch { handler() }