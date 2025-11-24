package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Mail
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.scrollView
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.util.humanDateTime
import org.bson.types.ObjectId

class MailFragment: RFragment("信箱") {
    override var fragSize = FragmentSize.FULL

    init {
        contentViewInit={
            server.request<List<Mail.Vo>>("mail"){
                load(it.data!!)
            }
        }
    }
    private fun load(mails: List<Mail.Vo>) {
        contentView.apply {
            mails.forEach { mail->
                textView(" \uEB1C ${mail.title}: ${mail.intro}..."){
                    setOnClickListener { Detail(mail.id).go() }
                }
            }
        }
    }
    class Detail(id: ObjectId) : RFragment("详细内容"){
        init {
            contentViewInit = {
                server.request<Mail>("mail/${id}"){
                    load(it.data!!)
                }
            }
        }
        private fun load(mail: Mail) {
            contentView.apply {
                scrollView {
                    textView("时间：${(mail._id.timestamp * 1000L).humanDateTime}")
                    textView("标题：${mail.title}")
                    textView("内容：\n${mail.content}")

                }
            }
        }
    }
}