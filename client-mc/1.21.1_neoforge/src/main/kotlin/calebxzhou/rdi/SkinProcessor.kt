package calebxzhou.rdi

import com.mojang.blaze3d.platform.NativeImage

object SkinProcessor {
    @JvmStatic
      fun processLegacySkin(pImage: NativeImage): NativeImage {
        var pImage = pImage
        val height = pImage.height
        val width = pImage.width
        lgr.info("处理皮肤中:${width}x${height}")
        if (width == 64 && (height == 32 || height == 64)) {
            val legacySkin = height == 32
            if (legacySkin) {
                val nativeimage = NativeImage(64, 64, true)
                nativeimage.copyFrom(pImage)
                pImage.close()
                pImage = nativeimage
                nativeimage.fillRect(0, 32, 64, 32, 0)
                nativeimage.copyRect(4, 16, 16, 32, 4, 4, true, false)
                nativeimage.copyRect(8, 16, 16, 32, 4, 4, true, false)
                nativeimage.copyRect(0, 20, 24, 32, 4, 12, true, false)
                nativeimage.copyRect(4, 20, 16, 32, 4, 12, true, false)
                nativeimage.copyRect(8, 20, 8, 32, 4, 12, true, false)
                nativeimage.copyRect(12, 20, 16, 32, 4, 12, true, false)
                nativeimage.copyRect(44, 16, -8, 32, 4, 4, true, false)
                nativeimage.copyRect(48, 16, -8, 32, 4, 4, true, false)
                nativeimage.copyRect(40, 20, 0, 32, 4, 12, true, false)
                nativeimage.copyRect(44, 20, -8, 32, 4, 12, true, false)
                nativeimage.copyRect(48, 20, -16, 32, 4, 12, true, false)
                nativeimage.copyRect(52, 20, -8, 32, 4, 12, true, false)
            }

            setNoAlpha(pImage, 0, 0, 32, 16)
            if (legacySkin) {
                doNotchTransparencyHack(pImage, 32, 0, 64, 32)
            }

            setNoAlpha(pImage, 0, 16, 64, 32)
            setNoAlpha(pImage, 16, 48, 48, 64)
            return pImage
        } else {
            //如果超过64x32尺寸的皮肤,不close 不return null
            return pImage
        }
    }

    private fun doNotchTransparencyHack(pImage: NativeImage?, pX: Int, pY: Int, pWidth: Int, pHeight: Int) {
        for (i in pX..<pWidth) {
            for (j in pY..<pHeight) {
                val k = pImage!!.getPixelRGBA(i, j)
                if ((k shr 24 and 255) < 128) {
                    return
                }
            }
        }

        for (l in pX..<pWidth) {
            for (i1 in pY..<pHeight) {
                pImage!!.setPixelRGBA(l, i1, pImage.getPixelRGBA(l, i1) and 16777215)
            }
        }
    }

    private fun setNoAlpha(pImage: NativeImage?, pX: Int, pY: Int, pWidth: Int, pHeight: Int) {
        for (i in pX..<pWidth) {
            for (j in pY..<pHeight) {
                pImage!!.setPixelRGBA(i, j, pImage.getPixelRGBA(i, j) or -16777216)
            }
        }
    }
}