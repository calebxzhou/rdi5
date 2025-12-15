package calebxzhou.rdi.client.mc

/**
 * calebxzhou @ 2025-12-15 12:03
 */
fun String.camelToSnakeCase(): String =
    replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace("-", "_")
        .lowercase()