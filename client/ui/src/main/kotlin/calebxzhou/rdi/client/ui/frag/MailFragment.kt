package calebxzhou.rdi.client.ui.frag

import calebxzhou.mykotutils.std.secondsToHumanDateTime
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.Mail
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui.*
import calebxzhou.rdi.client.ui.component.confirm
import icyllis.modernui.text.Typeface
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.CheckBox
import io.ktor.http.*
import org.bson.types.ObjectId
import kotlin.collections.isNotEmpty
import kotlin.collections.mapTo

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
                        textView("\uF415${mail.senderName} \uE38A${mail.id.timestamp.secondsToHumanDateTime}"){
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
                    title = "\uEB1C ${mail.title} ${mail._id.timestamp.secondsToHumanDateTime}"
                    textView(mail.content)
                }


            }
        }
    }
}