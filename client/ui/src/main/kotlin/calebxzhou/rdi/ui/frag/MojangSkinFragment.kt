package calebxzhou.rdi.ui.frag

import calebxzhou.mykotutils.mojang.MojangApi
import calebxzhou.mykotutils.mojang.MojangApi.textures
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.service.PlayerService
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.alertErr
import calebxzhou.rdi.ui.component.closeLoading
import calebxzhou.rdi.ui.component.showLoading
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
            paddingDp(0, 16, 0, 0)
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
        showLoading()
        ioTask {
            runCatching {
                val uuid = MojangApi.getUuidFromName(name).getOrThrow() ?: let {
                    throw RequestError("玩家${name}不存在")
                }
                val txts = MojangApi.getProfile(uuid).getOrThrow().textures
                val skin = txts["SKIN"] ?: let {
                    throw RequestError("玩家${name}没有设置过皮肤")
                }
                val cape = txts["CAPE"]

                val cloth = RAccount.Cloth(
                    isSlim = skin.metadata?.model.equals("slim", ignoreCase = true),
                    skin = skin.url,
                )
                if (importCape)
                    cape?.let { cloth.cape = it.url }
                PlayerService.setCloth(cloth)
            }.getOrElse {
                if(it !is RequestError)
                    it.printStackTrace()
                alertErr("导入失败: ${it.message}")
            }
            closeLoading()
        }
    }
}