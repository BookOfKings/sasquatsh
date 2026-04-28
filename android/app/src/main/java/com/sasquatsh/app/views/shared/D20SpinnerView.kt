package com.sasquatsh.app.views.shared

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt

/**
 * A spinning D20 (icosahedron) with numbered faces, rendered with OpenGL ES 2.0.
 * Port of the iOS SceneKit-based D20SpinnerView with per-face number textures.
 */
@Composable
fun D20SpinnerView(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    color: Color = Color(0xFF6366F1),
    numberColor: Color = Color.White
) {
    val colorArgb = remember(color) { color.toArgb() }
    val numColorArgb = remember(numberColor) { numberColor.toArgb() }

    AndroidView(
        modifier = modifier.size(size),
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                setRenderer(D20Renderer(colorArgb, numColorArgb))
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }
    )
}

private class D20Renderer(
    private val faceColorArgb: Int,
    private val numberColorArgb: Int
) : GLSurfaceView.Renderer {

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var program = 0
    private var vertexBuffer: FloatBuffer? = null
    private var normalBuffer: FloatBuffer? = null
    private var uvBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var triangleCount = 0
    private var textureIds = IntArray(20)

    private var startTime = System.nanoTime()

    private val phi = (1f + sqrt(5f)) / 2f
    private val s = 0.75f

    private val verts = arrayOf(
        floatArrayOf(-1f * s, phi * s, 0f),
        floatArrayOf(1f * s, phi * s, 0f),
        floatArrayOf(-1f * s, -phi * s, 0f),
        floatArrayOf(1f * s, -phi * s, 0f),
        floatArrayOf(0f, -1f * s, phi * s),
        floatArrayOf(0f, 1f * s, phi * s),
        floatArrayOf(0f, -1f * s, -phi * s),
        floatArrayOf(0f, 1f * s, -phi * s),
        floatArrayOf(phi * s, 0f, -1f * s),
        floatArrayOf(phi * s, 0f, 1f * s),
        floatArrayOf(-phi * s, 0f, -1f * s),
        floatArrayOf(-phi * s, 0f, 1f * s)
    )

    private val faces = arrayOf(
        intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7),
        intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
        intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2),
        intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
        intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6),
        intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
        intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10),
        intArrayOf(8, 6, 7), intArrayOf(9, 8, 1)
    )

    // D20 face numbers matching iOS
    private val numbers = intArrayOf(20, 8, 14, 2, 17, 1, 13, 7, 19, 4, 16, 10, 6, 18, 12, 5, 11, 15, 3, 9)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        generateTextures()
        buildGeometry()
        buildShaderProgram()
        startTime = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3.2f, 0f, 0f, 0f, 0f, 1f, 0f)

        val elapsed = (System.nanoTime() - startTime) / 1_000_000_000f
        val angle = (elapsed / 3.5f) * 360f
        Matrix.setIdentityM(modelMatrix, 0)
        val ax = 0.3f; val ay = 1f; val az = 0.15f
        val len = sqrt(ax * ax + ay * ay + az * az)
        Matrix.setRotateM(modelMatrix, 0, angle, ax / len, ay / len, az / len)

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUseProgram(program)

        val posHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        val uvHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        val mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val modelHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val lightDirHandle = GLES20.glGetUniformLocation(program, "uLightDir")
        val lightDir2Handle = GLES20.glGetUniformLocation(program, "uLightDir2")
        val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)

        // Key light
        val lLen = sqrt(2f * 2f + 3f * 3f + 4f * 4f)
        GLES20.glUniform3f(lightDirHandle, 2f / lLen, 3f / lLen, 4f / lLen)
        // Fill light
        val l2Len = sqrt(3f * 3f + 1f * 1f + 2f * 2f)
        GLES20.glUniform3f(lightDir2Handle, -3f / l2Len, -1f / l2Len, 2f / l2Len)

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glEnableVertexAttribArray(uvHandle)

        vertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        normalBuffer?.position(0)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)
        uvBuffer?.position(0)
        GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer)

        // Draw each face with its own texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(textureHandle, 0)

        for (i in 0 until 20) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[i])
            indexBuffer?.position(i * 3)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        }

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(uvHandle)
    }

    private fun generateTextures() {
        GLES20.glGenTextures(20, textureIds, 0)

        for (i in 0 until 20) {
            val bitmap = renderNumberBitmap(numbers[i])
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[i])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()
        }
    }

    private fun renderNumberBitmap(number: Int): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill with face color
        val bgPaint = Paint().apply {
            color = faceColorArgb
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Draw number
        val textPaint = Paint().apply {
            color = numberColorArgb
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textSize = if (number >= 10) 38f else 44f
        }
        val x = size / 2f
        // Center vertically, offset down slightly into triangle center
        val y = size / 2f + textPaint.textSize / 3f + 10f
        canvas.drawText("$number", x, y, textPaint)

        return bitmap
    }

    private fun buildGeometry() {
        val vertexList = mutableListOf<Float>()
        val normalList = mutableListOf<Float>()
        val uvList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        var idx: Short = 0
        for ((_, face) in faces.withIndex()) {
            val v0 = verts[face[0]]
            val v1 = verts[face[1]]
            val v2 = verts[face[2]]

            // Face normal
            val e1x = v1[0] - v0[0]; val e1y = v1[1] - v0[1]; val e1z = v1[2] - v0[2]
            val e2x = v2[0] - v0[0]; val e2y = v2[1] - v0[1]; val e2z = v2[2] - v0[2]
            var nx = e1y * e2z - e1z * e2y
            var ny = e1z * e2x - e1x * e2z
            var nz = e1x * e2y - e1y * e2x
            val nLen = sqrt(nx * nx + ny * ny + nz * nz)
            if (nLen > 0f) { nx /= nLen; ny /= nLen; nz /= nLen }

            // Vertices
            for (vi in face) {
                vertexList.addAll(verts[vi].toList())
                normalList.addAll(listOf(nx, ny, nz))
            }

            // UVs: map triangle to texture (matching iOS)
            uvList.addAll(listOf(0.5f, 0f))  // top center
            uvList.addAll(listOf(0f, 1f))    // bottom left
            uvList.addAll(listOf(1f, 1f))    // bottom right

            indexList.addAll(listOf(idx, (idx + 1).toShort(), (idx + 2).toShort()))
            idx = (idx + 3).toShort()
        }

        triangleCount = faces.size

        vertexBuffer = ByteBuffer.allocateDirect(vertexList.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(vertexList.toFloatArray()); position(0)
            }
        normalBuffer = ByteBuffer.allocateDirect(normalList.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(normalList.toFloatArray()); position(0)
            }
        uvBuffer = ByteBuffer.allocateDirect(uvList.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(uvList.toFloatArray()); position(0)
            }
        indexBuffer = ByteBuffer.allocateDirect(indexList.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
                put(indexList.toShortArray()); position(0)
            }
    }

    private fun buildShaderProgram() {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aTexCoord;
            varying vec3 vNormal;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = mat3(uModelMatrix) * aNormal;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec3 uLightDir;
            uniform vec3 uLightDir2;
            uniform sampler2D uTexture;
            varying vec3 vNormal;
            varying vec2 vTexCoord;
            void main() {
                vec3 norm = normalize(vNormal);
                float diff1 = max(dot(norm, uLightDir), 0.0);
                float diff2 = max(dot(norm, uLightDir2), 0.0) * 0.4;
                float ambient = 0.35;
                float light = ambient + (1.0 - ambient) * (diff1 * 0.7 + diff2 * 0.3);
                vec4 texColor = texture2D(uTexture, vTexCoord);
                // Specular highlight
                vec3 halfVec = normalize(uLightDir + vec3(0.0, 0.0, 1.0));
                float spec = pow(max(dot(norm, halfVec), 0.0), 16.0) * 0.3;
                gl_FragColor = vec4(texColor.rgb * light + vec3(spec), texColor.a);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
