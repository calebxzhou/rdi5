package calebxzhou.rdi.util

import org.slf4j.Logger

fun Logger.error(throwable: Throwable) {
    error(throwable.message ?: throwable.javaClass.name, throwable)
}
