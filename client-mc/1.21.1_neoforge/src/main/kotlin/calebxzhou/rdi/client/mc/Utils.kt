package calebxzhou.rdi.client.mc

import java.util.UUID

/**
 * calebxzhou @ 2025-12-15 12:03
 */
fun String.camelToSnakeCase(): String =
    replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace("-", "_")
        .lowercase()
fun isClassLoaded(className: String, classLoader: ClassLoader = ClassLoader.getSystemClassLoader()): Boolean {
    return try {
        Class.forName(className, false, classLoader) // false = do not initialize
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}