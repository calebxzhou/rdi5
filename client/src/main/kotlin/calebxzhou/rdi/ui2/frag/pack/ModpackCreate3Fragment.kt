package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.plusAssign

class ModpackCreate3Fragment(val name: String, val mods: List<Mod>) : RFragment("制作整合包3 配置与脚本") {
    //读取config
    override var fragSize = FragmentSize.SMALL

    companion object {
        fun readConfKjs(): Pair<Map<String, ByteArray>, Map<String, ByteArray>> {
            val confs = hashMapOf<String, ByteArray>()
            val kjs = hashMapOf<String, ByteArray>()
            val configRoot = File("config")
            if (configRoot.exists()) {
                configRoot.walk().forEach { file ->
                    if (file.isFile) {
                        val relative = file.relativeTo(configRoot).invariantSeparatorsPath
                        confs += relative to file.readBytes()
                    }
                }
            }

            val kubeRoot = File("kubejs")
            if (kubeRoot.exists()) {
                kubeRoot.walk().forEach { file ->
                    if (file.isFile) {
                        val relative = file.relativeTo(kubeRoot).invariantSeparatorsPath
                        kjs += relative to file.readBytes()
                    }
                }
            }
            return confs to kjs
        }
    }

    init {
        contentLayoutInit = {
            textView("正在读取配置文件与KubeJS脚本。需要3秒左右")
            ioScope.launch {
                readConfKjs().let { (conf, kjs) ->
                    val confSize = conf.values.sumOf { it.size }
                    val kjsSize = kjs.values.sumOf { it.size }
                    uiThread {
                        contentLayout.apply {
                            textView("读取完成！")
                            textView("配置文件${conf.size} 个，总大小：${confSize.toLong().humanSize} ")
                            textView("KubeJS脚本${kjs.size} 个，总大小：${kjsSize.toLong().humanSize} ")
                            bottomOptions{
                                "下一步" colored MaterialColor.GREEN_900 with {
                                    ModpackCreate4Fragment(name, mods, conf, kjs).go()

                                }
                            }
                        }

                    }
                }
            }
        }

    }
}