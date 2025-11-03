package calebxzhou.rdi.ihq

import org.bson.types.ObjectId

/**
 * calebxzhou @ 2025-07-07 23:07
 */
val REGEX_RESLOCA = Regex("^(?:[a-z0-9_\\-.]+:)?[a-z0-9_\\-.\\/]+\$")
val DEFAULT_MODPACK_ID = ObjectId("abcdefabcdefabcdefabcdef")
//系统邮件用
val SYSTEM_SENDER_ID = DEFAULT_MODPACK_ID