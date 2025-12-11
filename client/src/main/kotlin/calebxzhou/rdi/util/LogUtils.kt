package calebxzhou.rdi.util

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * calebxzhou @ 2025-11-19 10:20
 */
object Loggers {
    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>
    ): ReadOnlyProperty<Any?, KLogger> {
        val owner = thisRef
        val logger = when (owner) {
            null -> KotlinLogging.logger(prop.name)
            is Class<*> -> KotlinLogging.logger(owner.name)
            else -> KotlinLogging.logger(owner::class.java.name)
        }
        return ReadOnlyProperty { _, _ -> logger }
    }
}

fun markerFor(target: Any, fallback: String = "Anonymous"): Marker =
    MarkerFactory.getMarker(target::class.simpleName ?: fallback)