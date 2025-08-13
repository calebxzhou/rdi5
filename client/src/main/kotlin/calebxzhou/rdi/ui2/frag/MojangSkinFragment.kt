package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.MojangApi
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.uiThread
import icyllis.modernui.material.MaterialCheckBox
import icyllis.modernui.widget.CheckBox
import icyllis.modernui.widget.LinearLayout
import kotlinx.coroutines.launch


class MojangSkinFragment : RFragment("导入正版皮肤") {
    private lateinit var nameInput: REditText
    private lateinit var skinCheckBox: CheckBox
    private lateinit var capeCheckBox: CheckBox

    override fun initContent() {
        contentLayout.apply {
            orientation = LinearLayout.VERTICAL

            textView {
                text = "正版玩家名"
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(8f)
                }
            }

            nameInput = editText("输入正版玩家名", 200f) {
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(16f)
                }
            }

            skinCheckBox = MaterialCheckBox(fctx).apply {
                text = "导入皮肤"
                isChecked = true
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(8f)
                }
            }
            this += skinCheckBox

            capeCheckBox = MaterialCheckBox(fctx).apply {
                text = "导入披风"
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    bottomMargin = dp(16f)
                }
            }
            this += capeCheckBox

            textButton("导入") {
                importMojangSkin()
            }
        }
    }

    private fun importMojangSkin() {
        val name = nameInput.text.toString().trim()
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

        ioScope.launch {
            try {
                val uuid = MojangApi.getUuidFromName(name)
                if (uuid != null) {
                    val cloth = MojangApi.getCloth(uuid)
                    if (cloth != null) {
                        val account = RAccount.now ?: return@launch
                        val server = RServer.now ?: return@launch

                        val newCloth = account.cloth.copy()
                        if (importSkin) {
                            newCloth.skin = cloth.skin
                            newCloth.isSlim = cloth.isSlim
                        }
                        if (importCape) {
                            newCloth.cape = cloth.cape
                        }

                        val params = mutableListOf<Pair<String, Any>>()
                        params += "isSlim" to newCloth.isSlim.toString()
                        params += "skin" to newCloth.skin
                        newCloth.cape?.let {
                            params += "cape" to it
                        }

                        server.hqRequest(true, "skin", params = params) { response ->
                            if (response.success) {
                                account.updateCloth(newCloth)
                                uiThread {
                                    close()
                                    alertOk("正版皮肤导入成功")
                                }
                            } else {
                                uiThread {
                                    close()
                                    alertOk("皮肤设置失败")
                                }
                            }
                        }
                    } else {
                        uiThread {
                            alertErr("没有读取到${name}的皮肤")
                        }
                    }
                } else {
                    uiThread {
                        alertErr("玩家${name}不存在")
                    }
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