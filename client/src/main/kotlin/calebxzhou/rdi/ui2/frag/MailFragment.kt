package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Mail
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.scrollView
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.ui2.vertical
import calebxzhou.rdi.util.humanDateTime
import io.ktor.http.HttpMethod
import org.bson.types.ObjectId

class MailFragment : RFragment("信箱") {
    override var fragSize = FragmentSize.FULL

    init {
        contentViewInit = {
            server.request<List<Mail.Vo>>("mail") {
                load(it.data!!)
            }
        }
    }

    private fun load(mails: List<Mail.Vo>) = uiThread {
        contentView.apply {
            if (mails.isEmpty()) {
                textView("什么都没有~")
                return@apply
            }
            mails.forEach { mail ->
                textView(" \uEB1C ${mail.title}: ${mail.intro}...") {
                    setOnClickListener { Detail(mail.id).go() }
                }
            }
        }
    }

    class Detail(id: ObjectId) : RFragment("详细内容") {
        init {
            this.contentViewInit = {
                server.request<Mail>("mail/${id}") {
                    load(it.data!!)
                }
            }
            titleViewInit = {
                quickOptions {
                    "\uF014 删除" colored MaterialColor.RED_900 with {
                        confirm("要删除这封邮件吗？"){
                            server.requestU("mail/${id}", HttpMethod.Delete) {
                                toast("已删除")
                                close()
                            }
                        }
                    }
                }
            }
        }

        private fun load(mail: Mail) = uiThread{
            this.contentView.scrollView {
                linearLayout {
                    vertical()
                    layoutParams = linearLayoutParam(PARENT,PARENT)
                    title = "\uEB1C ${mail.title} ${(mail._id.timestamp * 1000L).humanDateTime}"
                    textView(mail.content)
                }


            }
        }
    }
}