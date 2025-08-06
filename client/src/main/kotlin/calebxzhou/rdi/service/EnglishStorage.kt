package calebxzhou.rdi.service

import net.minecraft.client.resources.language.ClientLanguage

object EnglishStorage {
    lateinit var lang : ClientLanguage
    operator fun get(key: String): String = lang.getOrDefault(key)
}