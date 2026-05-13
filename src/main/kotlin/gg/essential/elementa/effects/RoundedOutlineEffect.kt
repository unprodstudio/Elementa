package gg.essential.elementa.effects

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.MappedState
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.readElementaShaderSource
import gg.essential.elementa.utils.readFromLegacyShader
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.shader.Float4Uniform
import gg.essential.universal.shader.FloatUniform
import gg.essential.universal.shader.UShader
import gg.essential.universal.vertex.UBufferBuilder
import java.awt.Color

class RoundedOutlineEffect(
    thickness: State<Float>, radius: State<Float>, color: State<Color>,
    var drawAfterChildren: Boolean = false,
) : Effect() {
    @JvmOverloads constructor(
        thickness: Float, radius: Float, color: Color,
        drawAfterChildren: Boolean = false,
    ) : this(BasicState(thickness), BasicState(radius), BasicState(color), drawAfterChildren)

    private val colorState: MappedState<Color, Color> = color.map { it }
    private val thicknessState: MappedState<Float, Float> = thickness.map { it }
    private val radiusState: MappedState<Float, Float> = radius.map { it }

    var color: Color
        get() = colorState.get()
        set(value) {
            colorState.set(value)
        }

    var thickness: Float
        get() = thicknessState.get()
        set(value) {
            thicknessState.set(value)
        }

    var radius: Float
        get() = radiusState.get()
        set(value) {
            radiusState.set(value)
        }

    fun bindColor(state: State<Color>) = apply {
        colorState.rebind(state)
    }

    fun bindThickness(state: State<Float>) = apply {
        thicknessState.rebind(state)
    }

    fun bindRadius(state: State<Float>) = apply {
        radiusState.rebind(state)
    }

    override fun beforeChildrenDraw(matrixStack: UMatrixStack) {
        if (!drawAfterChildren)
            drawOutline(matrixStack)
    }

    override fun afterDraw(matrixStack: UMatrixStack) {
        if (drawAfterChildren)
            drawOutline(matrixStack)
    }

    private fun drawOutline(matrixStack: UMatrixStack) {
        val left = boundComponent.getLeft()
        val right = boundComponent.getRight()
        val top = boundComponent.getTop()
        val bottom = boundComponent.getBottom()

        val thickness = thickness

        if (color.alpha != 0)
            drawRoundedOutline(matrixStack, left, top, right, bottom, radius, color, thickness)
    }

    companion object {
        private lateinit var shader: UShader
        private lateinit var shaderRadiusUniform: FloatUniform
        private lateinit var shaderThicknessUniform: FloatUniform
        private lateinit var shaderInnerRectUniform: Float4Uniform

        private val PIPELINE = URenderPipeline.builderWithLegacyShader(
            "elementa:rounded_outline",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_COLOR,
            readElementaShaderSource("rect", "vsh"),
            readElementaShaderSource("rounded_outline", "fsh"),
        ).apply {
            @Suppress("DEPRECATION")
            blendState = BlendState.NORMAL
            depthTest = URenderPipeline.DepthTest.Always // see UIBlock.PIPELINE
        }.build()

        private val PIPELINE2 = URenderPipeline.builderWithLegacyShader(
            "elementa:rounded_outline",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_COLOR,
            readElementaShaderSource("rect", "vsh"),
            readElementaShaderSource("rounded_outline", "fsh"),
        ).apply {
            blendState = BlendState.ALPHA
            depthTest = URenderPipeline.DepthTest.Always // see UIBlock.PIPELINE
        }.build()

        fun initShaders() {
            if (URenderPipeline.isRequired) return
            if (::shader.isInitialized)
                return

            @Suppress("DEPRECATION")
            shader = UShader.readFromLegacyShader("rect", "rounded_outline", BlendState.NORMAL)
            if (!shader.usable) {
                println("Failed to load Elementa UIRoundedOutline shader")
                return
            }
            shaderRadiusUniform = shader.getFloatUniform("u_Radius")
            shaderInnerRectUniform = shader.getFloat4Uniform("u_InnerRect")
            shaderThicknessUniform = shader.getFloatUniform("u_Thickness")
        }

        @Deprecated(
            UMatrixStack.Compat.DEPRECATED,
            ReplaceWith("drawRoundedRectangle(matrixStack, left, top, right, bottom, radius, color)"),
        )
        fun drawRoundedOutline(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Color, thickness: Float) =
            drawRoundedOutline(UMatrixStack(), left, top, right, bottom, radius, color, thickness)

        /**
         * Draws a rounded rectangle
         */
        fun drawRoundedOutline(matrixStack: UMatrixStack, left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Color, thickness: Float) {
            if (!URenderPipeline.isRequired && !ElementaVersion.atLeastV9Active) {
                @Suppress("DEPRECATION")
                return drawRoundedRectangleLegacy(matrixStack, left, top, right, bottom, radius, color, thickness)
            }

            val bufferBuilder = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR)
            UIBlock.drawBlock(bufferBuilder, matrixStack, color, left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
            bufferBuilder.build()?.drawAndClose(if (ElementaVersion.atLeastV10Active) PIPELINE2 else PIPELINE) {
                uniform("u_Radius", radius)
                uniform("u_InnerRect", left + radius, top + radius, right - radius, bottom - radius)
                uniform("u_Thickness", thickness)
            }
        }

        @Deprecated("Stops working in 1.21.5")
        @Suppress("DEPRECATION")
        private fun drawRoundedRectangleLegacy(matrixStack: UMatrixStack, left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Color, thickness: Float) {
            if (!::shader.isInitialized || !shader.usable)
                return

            shader.bind()
            shaderRadiusUniform.setValue(radius)
            shaderInnerRectUniform.setValue(left + radius, top + radius, right - radius, bottom - radius)
            shaderThicknessUniform.setValue(thickness)

            UIBlock.drawBlockWithActiveShader(matrixStack, color, left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

            shader.unbind()
        }
    }
}
