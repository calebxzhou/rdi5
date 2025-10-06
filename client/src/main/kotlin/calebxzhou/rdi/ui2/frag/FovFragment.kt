package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import calebxzhou.rdi.util.toFixed
import icyllis.modernui.mc.BlurHandler
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.SeekBar
import icyllis.modernui.widget.TextView
import kotlin.math.tan

class FovFragment : RFragment("视野") {
    val fovOption
        get() = mc.options.fov()
    private lateinit var bar: SeekBar
    private lateinit var focusDisp: TextView

    init {
        renderThread {

            BlurHandler.INSTANCE.closeEffect()
        }
    }

    init {
        contentLayoutInit = {
            bar = SeekBar(context).apply {
                max = 120
                progress = fovOption.get()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        fovOption.set(progress)
                        updateFocusDisp()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })
            }.also { this += it }
            focusDisp = textView{
                layoutParams = linearLayoutParam {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = linearLayoutParam {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                button("超超广角") { bar.progress = 120 }
                button("超广角") { bar.progress = 103 }
                button("广角") { bar.progress = 81 }
                button("标准") { bar.progress = 54 }
                button("中焦") { bar.progress = 40 }
                button("长焦") { bar.progress = 24 }
                button("超长焦") { bar.progress = 6 }
                button("超超长焦") { bar.progress = 2 }
            }
            updateFocusDisp()
        }
    }

    fun updateFocusDisp() {
        focusDisp.text = "视野角度：${fovOption.get()}° 等效相机焦距: ${
            calculateFocalLength(
                fovOption.get().toDouble()
            ).toFixed(1)
        }mm"
    }

    fun calculateFocalLength(fovAngle: Double): Double {
        // Convert the FOV angle from degrees to radians
        val fovRadians = Math.toRadians(fovAngle)
        // Calculate the focal length using the formula: focal length = (sensor width / 2) / tan(fov / 2)
        val sensorWidth = 36 // 35mm full-frame sensor width in millimeters
        return (sensorWidth / 2) / tan(fovRadians / 2)
    }
}