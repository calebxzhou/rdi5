package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.SYSTEM_SENDER_ID
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Mail
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

object MailService {
    private const val TITLE_MAX_LEN = 120
    private const val CONTENT_MAX_LEN = 4000

    private val mailCol = DB.getCollection<Mail>("mail")

    suspend fun getInbox(uid: ObjectId): List<Mail> {
        ensurePlayerExists(uid)
        return mailCol.find(eq("receiverId", uid))
            .sort(Sorts.descending("_id"))
            .toList()
    }

    suspend fun sendMail(
        senderId: ObjectId,
        receiverId: ObjectId,
        title: String,
        content: String,
    ): Mail {
        ensurePlayerExists(senderId)
        ensurePlayerExists(receiverId)
        val normalizedTitle = normalizeTitle(title)
        val normalizedContent = normalizeContent(content)
        val mail = Mail(
            senderId = senderId,
            receiverId = receiverId,
            title = normalizedTitle,
            content = normalizedContent,
        )
        mailCol.insertOne(mail)
        return mail
    }

    suspend fun sendSystemMail(
        receiverId: ObjectId,
        title: String,
        content: String,
    ): Mail {
        ensurePlayerExists(receiverId)
        val normalizedTitle = normalizeTitle(title)
        val normalizedContent = normalizeContent(content)
        val mail = Mail(
            senderId = SYSTEM_SENDER_ID,
            receiverId = receiverId,
            title = normalizedTitle,
            content = normalizedContent,
        )
        mailCol.insertOne(mail)
        return mail
    }

    suspend fun markAsRead(mailId: ObjectId, ownerId: ObjectId) {
        val updateResult = mailCol.updateOne(
            and(eq("_id", mailId), eq("receiverId", ownerId)),
            Updates.set("unread", false)
        )
        if (updateResult.matchedCount == 0L) {
            throw RequestError("未找到对应邮件")
        }
    }

    private suspend fun ensurePlayerExists(uid: ObjectId) {
        if (uid == SYSTEM_SENDER_ID) return
        PlayerService.getById(uid) ?: throw RequestError("玩家不存在")
    }

    private fun normalizeTitle(title: String): String {
        val normalized = title.trim()
        if (normalized.isEmpty()) throw ParamError("标题不能为空")
        if (normalized.length > TITLE_MAX_LEN) throw ParamError("标题过长")
        return normalized
    }

    private fun normalizeContent(content: String): String {
        val normalized = content.trim()
        if (normalized.isEmpty()) throw ParamError("内容不能为空")
        if (normalized.length > CONTENT_MAX_LEN) throw ParamError("内容过长")
        return normalized
    }
}