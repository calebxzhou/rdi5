package calebxzhou.rdi.ui.component

import calebxzhou.rdi.util.isTextureReady
import calebxzhou.rdi.util.mc
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.PlayerModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import org.joml.Matrix4f
import org.joml.Quaternionf
import kotlin.math.atan2
import kotlin.math.sin


object RGuiEntityRenderer {
    fun drawEntity(
        matrices: PoseStack,
        x: Int,
        y: Int,
        size: Int,
        rotation: Float,
        mouseX: Double,
        mouseY: Double,
        isSlim: Boolean,
        skinRL: ResourceLocation,
        capeRL: ResourceLocation?=null
    ) {
        //400-转动幅度
        val yaw = (-atan2(mouseX, 60.0)).toFloat() // Invert the yaw calculation
        val pitch = (atan2(mouseY, 400.0)).toFloat() // Invert the pitch calculation

        val entityRotation = Quaternionf().rotateY(rotation * 0.025f)
        val pitchRotation = Quaternionf().rotateX(pitch * 10.0f * 0.017453292f)
        val yawRotation = Quaternionf().rotateY(yaw * 10.0f * 0.017453292f)
        entityRotation.mul(pitchRotation)
        entityRotation.mul(yawRotation)

        setupModelViewStack()
        setupMatrices(matrices, x, y, size, entityRotation)

        renderEntity(matrices, yaw, pitch, isSlim, skinRL, capeRL,mc.timer.gameTimeDeltaTicks)

        cleanupMatrices(matrices)
        cleanupModelViewStack()
    }

    private fun setupModelViewStack() {
        val modelViewStack  = RenderSystem.getModelViewStack()
        modelViewStack.pushMatrix()
        modelViewStack.translate(0.0f, 0.0f, 1000.0f)
        RenderSystem.applyModelViewMatrix()
    }

    private fun setupMatrices(matrices: PoseStack, x: Int, y: Int, size: Int, entityRotation: Quaternionf) {
        matrices.pushPose()
        matrices.translate(x.toDouble(), y.toDouble(), 100.0)
        matrices.mulPose(Matrix4f().scaling(size.toFloat(), size.toFloat(), -size.toFloat()))
        matrices.translate(0.0, -1.0, 0.0)
        matrices.mulPose(entityRotation)
        matrices.translate(0.0, -1.0, 0.0)
        Lighting.setupForEntityInInventory()
    }
    private fun renderEntity(
        matrices: PoseStack,
        yaw: Float,
        pitch: Float,
        isSlim: Boolean,
        skinRL: ResourceLocation,
        capeRL: ResourceLocation?,
        totalTickDelta: Float
    ) {

        val modelData = PlayerModel.createMesh(CubeDeformation.NONE, isSlim)
        val model =
            NoEntityPlayerModel(LayerDefinition.create(modelData, 64, 64).bakeRoot(), isSlim)

        model.swingArmsGently(totalTickDelta)
        model.setHeadPos(yaw, pitch)
        model.waveCapeGently(totalTickDelta)


        val vertexConsumers = mc.renderBuffers().bufferSource()
        model.renderToBuffer(
            matrices,
            vertexConsumers.getBuffer(RenderType.entityTranslucent(
                if(skinRL.isTextureReady)
                skinRL
                else DefaultPlayerSkin.getDefaultTexture()
            )),
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            0xFFFFFFFF.toInt()
        )

        capeRL?.let{ capeRL ->
            if(!capeRL.isTextureReady)
                return@let
            matrices.pushPose()
            matrices.scale(0.6f,0.6f,1f)
            matrices.translate(0.0f, 1.3f, 0.1f)

            matrices.mulPose(Quaternionf().rotateY(Math.PI.toFloat())) // Adjust rotation if needed
            //matrices.mulPose(Quaternionf().rotate(Math.PI.toFloat())) // Adjust rotation if needed
            model.renderCloak(
                matrices,
                vertexConsumers.getBuffer(
                    RenderType.entityTranslucent(
                        capeRL
                    )
                ),

                255,
                OverlayTexture.NO_OVERLAY
            );
            matrices.popPose()
        }
        vertexConsumers.endBatch()
        /*if ( mc.textureManager.getTexture(skinRL, MissingTextureAtlasSprite.getTexture()) == MissingTextureAtlasSprite.getTexture()) {
            textureReady=false
        }*/
    }

    private fun cleanupMatrices(matrices: PoseStack) {
        matrices.popPose()
        Lighting.setupFor3DItems()
    }

    private fun cleanupModelViewStack() {
        val modelViewStack = RenderSystem.getModelViewStack()
        modelViewStack.popMatrix()
        RenderSystem.applyModelViewMatrix()
    }

    class NoEntityPlayerModel(root: ModelPart, thinArms: Boolean) : PlayerModel<Player>(root, thinArms) {
        init {
            young=false
        }
        fun swingArmsGently(totalDeltaTick: Float) {
            val f: Float = sin(totalDeltaTick * 0.067f) * 0.05f
            this.rightArm.zRot = f + 0.06f
            this.rightSleeve.zRot = f + 0.06f
            this.leftArm.zRot = -f - 0.06f
            this.leftSleeve.zRot = -f - 0.06f
        }

        fun setHeadPos(headYaw: Float, headPitch: Float) {
            this.head.yRot = headYaw
            this.hat.yRot = headYaw
            this.head.xRot = headPitch
            this.hat.xRot = headPitch
        }

        fun waveCapeGently(totalDeltaTick: Float) {
            val f: Float = sin(totalDeltaTick * 0.067f) * 0.05f

            (this).cloak.xRot = f;
        }
    }
}