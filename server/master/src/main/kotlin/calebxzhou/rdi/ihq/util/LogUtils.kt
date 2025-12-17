package calebxzhou.rdi.ihq.util

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Calebrdi @ 2025-11-19 originally rolled a marker-aware proxy.
 * We can lean on kotlin-logging so every class just declares `private val log by Loggers`.
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