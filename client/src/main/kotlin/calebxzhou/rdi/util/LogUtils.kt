package calebxzhou.rdi.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * calebxzhou @ 2025-11-19 10:20
 */
class LoggerWithMarker(private val delegate: Logger, private val marker: Marker) {
    fun trace(msg: String, vararg args: Any?) = delegate.trace(marker, msg, *args)
    fun debug(msg: String, vararg args: Any?) = delegate.debug(marker, msg, *args)
    fun info(msg: String, vararg args: Any?) = delegate.info(marker, msg, *args)
    fun warn(msg: String, vararg args: Any?) = delegate.warn(marker, msg, *args)
    fun error(msg: String, vararg args: Any?) = delegate.error(marker, msg, *args)
    fun error(t: Throwable, msg: String, vararg args: Any?) = delegate.error(marker, msg, *args, t)
}
//The actual delegate provider
object Loggers {
    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>
    ): ReadOnlyProperty<Any?, LoggerWithMarker> {
        val logger = LoggerFactory.getLogger(thisRef!!::class.java)
        val marker = MarkerFactory.getMarker(thisRef::class.simpleName ?: "Anonymous")
        return ReadOnlyProperty { _, _ -> LoggerWithMarker(logger, marker) }
    }
}