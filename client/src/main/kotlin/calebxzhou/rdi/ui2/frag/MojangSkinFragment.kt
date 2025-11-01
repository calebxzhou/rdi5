package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.MojangApi
import calebxzhou.rdi.model.account
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioTask
import icyllis.modernui.widget.CheckBox
import icyllis.modernui.widget.EditText


class MojangSkinFragment : RFragment("导入正版皮肤") {
    private lateinit var nameInput: EditText
    private lateinit var skinCheckBox: CheckBox
    private lateinit var capeCheckBox: CheckBox
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}

    init {
        contentViewInit = {
            paddingDp(0,16,0,0)
            layoutParams = linearLayoutParam(dp(200f), SELF)
            vertical()
            nameInput = editText("正版玩家的昵称")


            linearLayout {
                padding8dp()
                center()
                layoutParams = linearLayoutParam(SELF, SELF)
                skinCheckBox = checkBox("导入皮肤", {
                    isChecked = true
                })
                capeCheckBox = checkBox("导入披风")
            }


        }
        bottomOptionsConfig = {
            "导入" colored MaterialColor.GREEN_900 with {
                importMojangSkin()
            }
        }
    }

    private fun importMojangSkin() {
        val name = nameInput.text.toString()
        val importSkin = skinCheckBox.isChecked
        val importCape = capeCheckBox.isChecked

        if (name.isEmpty()) {
            toast("请输入玩家名")
            return
        }

        if (!importSkin && !importCape) {
            toast("请选择皮肤或披风")
            return
        }

        ioTask {
            try {
                val uuid = MojangApi.getUuidFromName(name)
                if (uuid == null) {
                    alertErr("玩家${name}不存在")
                    return@ioTask
                }
                val cloth = MojangApi.getCloth(uuid)
                if (cloth == null) {
                    alertErr("没有读取到${name}的皮肤")
                    return@ioTask
                }
                val newCloth = account.cloth.copy()
                if (importSkin) {
                    newCloth.skin = cloth.skin
                    newCloth.isSlim = cloth.isSlim
                }
                if (importCape) {
                    newCloth.cape = cloth.cape
                }
                val params = mutableMapOf<String, Any>()
                params["isSlim"] = newCloth.isSlim.toString()
                params["skin"] = newCloth.skin
                newCloth.cape?.let {
                    params["cape"] = it
                }

                server.requestU("skin", params = params) {

                    account.updateCloth(newCloth)
                    alertOk("正版皮肤导入成功")

                }


            } catch (e: Exception) {
                e.printStackTrace()
                uiThread {
                    alertErr("导入失败: ${e.message}")
                }
            }
        }
    }
}