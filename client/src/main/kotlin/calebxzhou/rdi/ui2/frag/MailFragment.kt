package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Mail
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.checkBox
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.frameLayout
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.scrollLinearLayout
import calebxzhou.rdi.ui2.scrollView
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.ui2.vertical
import calebxzhou.rdi.util.humanDateTime
import calebxzhou.rdi.util.json
import icyllis.modernui.text.Typeface
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.CheckBox
import io.ktor.http.HttpMethod
import org.bson.types.ObjectId

class MailFragment : RFragment("信箱") {
    override var fragSize = FragmentSize.FULL
    private val selectedIds = linkedSetOf<ObjectId>()
    private val checkboxRefs = mutableMapOf<ObjectId, CheckBox>()
    private var selectAllToggle: CheckBox? = null
    private var suppressSelectAllChange = false

    init {
        contentViewInit = {
            server.request<List<Mail.Vo>>("mail") {
                load(it.data!!)
            }
        }
    }

    private fun load(mails: List<Mail.Vo>) = uiThread {
        val visibleIds = mails.mapTo(mutableSetOf()) { it.id }
        selectedIds.retainAll(visibleIds)
        checkboxRefs.clear()

        contentView.removeAllViews()
        contentView.apply {
            if (mails.isEmpty()) {
                textView("什么都没有~")
                return@apply
            }
            scrollLinearLayout {
                vertical()
                mails.forEach { mail ->
                    frameLayout {
                        setOnClickListener { Detail(mail.id).go() }
                        linearLayout {
                            gravity = Gravity.START or Gravity.CENTER_VERTICAL
                            val cb = checkBox(
                                init = {
                                    isChecked = mail.id in selectedIds
                                }
                            ) { checkbox, checked ->
                                handleMailSelection(mail.id, checkbox, checked, mails.size)
                            }
                            checkboxRefs[mail.id] = cb
                            textView(" \uEB1C ${mail.title}")
                            textView(" ${mail.intro}...") {
                                paddingDp(24,0,0,0)
                                setTextColor(MaterialColor.GRAY_500.colorValue)
                                textSize = 14f
                                textStyle = Typeface.ITALIC
                            }
                        }
                        textView("\uF415${mail.senderName} \uE38A${mail.id.humanDateTime}"){
                            gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        }
                    }
                }
            }
        }
        titleView.apply {
            removeAllViews()
            selectAllToggle = null
            quickOptions {
                "全选" make checkbox init {
                    selectAllToggle = this
                    isEnabled = mails.isNotEmpty()
                    suppressSelectAllChange = true
                    isChecked = mails.isNotEmpty() && selectedIds.size == mails.size
                    suppressSelectAllChange = false
                } with { checked ->
                    if (suppressSelectAllChange) return@with
                    toggleAllSelection(checked, mails)
                }
                "\uF014 删除所选" colored MaterialColor.RED_900 with {
                    if (selectedIds.isEmpty()) {
                        toast("请选择至少一封邮件")
                        return@with
                    }
                    confirm("要删除所选的邮件吗？") {
                        val payload = selectedIds.toList().json
                        server.requestU("mail", HttpMethod.Delete, body = payload) {
                            toast("已删除")
                            selectedIds.clear()
                            reloadFragment()
                        }
                    }
                }
            }
        }
        updateSelectAllState(mails.size)
    }

    private fun handleMailSelection(mailId: ObjectId, checkBox: CheckBox, checked: Boolean, total: Int) {
        if (checked) selectedIds += mailId else selectedIds -= mailId
        checkboxRefs[mailId] = checkBox
        updateSelectAllState(total)
    }

    private fun toggleAllSelection(checked: Boolean, mails: List<Mail.Vo>) {
        if (mails.isEmpty()) return
        mails.forEach { mail ->
            checkboxRefs[mail.id]?.let { cb ->
                if (cb.isChecked != checked) {
                    cb.isChecked = checked
                }
            }
        }
    }

    private fun updateSelectAllState(total: Int) {
        val toggle = selectAllToggle ?: return
        toggle.isEnabled = total > 0
        val shouldCheckAll = total > 0 && selectedIds.size == total
        if (toggle.isChecked == shouldCheckAll) return
        suppressSelectAllChange = true
        toggle.isChecked = shouldCheckAll
        suppressSelectAllChange = false
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
                        confirm("要删除这封邮件吗？") {
                            server.requestU("mail/${id}", HttpMethod.Delete) {
                                toast("已删除")
                                close()
                            }
                        }
                    }
                }
            }
        }

        private fun load(mail: Mail) = uiThread {
            this.contentView.scrollView {
                linearLayout {
                    vertical()
                    layoutParams = linearLayoutParam(PARENT, PARENT)
                    title = "\uEB1C ${mail.title} ${(mail._id.timestamp * 1000L).humanDateTime}"
                    textView(mail.content)
                }


            }
        }
    }
}