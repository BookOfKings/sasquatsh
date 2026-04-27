package com.sasquatsh.app.views.shared

import android.opengl.GLES20
import android.opengl.GLSurfaceView
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
 * A spinning D20 (icosahedron) rendered with OpenGL ES 2.0.
 * Port of the iOS SceneKit-based D20SpinnerView.
 */
@Composable
fun D20SpinnerView(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    color: Color = Color(0xFF6366F1),
    numberColor: Color = Color.White
) {
    val colorArgb = remember(color) { color.toArgb() }

    AndroidView(
        modifier = modifier.size(size),
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
                setZOrderOnTop(true)
                setRenderer(D20Renderer(colorArgb))
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }
    )
}

private class D20Renderer(private val faceColorArgb: Int) : GLSurfaceView.Renderer {

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var program = 0
    private var vertexBuffer: FloatBuffer? = null
    private var normalBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var colorBuffer: FloatBuffer? = null
    private var triangleCount = 0

    private var startTime = System.nanoTime()

    // Golden ratio
    private val phi = (1f + sqrt(5f)) / 2f
    private val s = 0.75f

    // Icosahedron vertices
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

    // 20 triangular faces
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

    // Slightly different shades per face for visual depth
    private fun faceShade(faceIndex: Int): Float {
        // Vary brightness between 0.7 and 1.0 across 20 faces
        return 0.7f + 0.3f * (faceIndex % 5) / 4f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

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

        // Camera
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3.2f, 0f, 0f, 0f, 0f, 1f, 0f)

        // Rotation: spin around axis (0.3, 1, 0.15), 3.5 second period
        val elapsed = (System.nanoTime() - startTime) / 1_000_000_000f
        val angle = (elapsed / 3.5f) * 360f
        Matrix.setIdentityM(modelMatrix, 0)
        // Normalize axis
        val ax = 0.3f; val ay = 1f; val az = 0.15f
        val len = sqrt(ax * ax + ay * ay + az * az)
        Matrix.setRotateM(modelMatrix, 0, angle, ax / len, ay / len, az / len)

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        // Draw
        GLES20.glUseProgram(program)

        val posHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        val mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val modelHandle = GLES20.glGetUniformLocation(program, "uModelMatrix")
        val lightDirHandle = GLES20.glGetUniformLocation(program, "uLightDir")

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
        // Light direction (normalized)
        val lLen = sqrt(2f * 2f + 3f * 3f + 4f * 4f)
        GLES20.glUniform3f(lightDirHandle, 2f / lLen, 3f / lLen, 4f / lLen)

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glEnableVertexAttribArray(colorHandle)

        vertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        normalBuffer?.position(0)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)
        colorBuffer?.position(0)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)

        indexBuffer?.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, triangleCount * 3, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun buildGeometry() {
        val vertexList = mutableListOf<Float>()
        val normalList = mutableListOf<Float>()
        val colorList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        val r = ((faceColorArgb shr 16) and 0xFF) / 255f
        val g = ((faceColorArgb shr 8) and 0xFF) / 255f
        val b = (faceColorArgb and 0xFF) / 255f

        var idx: Short = 0
        for ((fi, face) in faces.withIndex()) {
            val v0 = verts[face[0]]
            val v1 = verts[face[1]]
            val v2 = verts[face[2]]

            // Compute face normal
            val e1x = v1[0] - v0[0]; val e1y = v1[1] - v0[1]; val e1z = v1[2] - v0[2]
            val e2x = v2[0] - v0[0]; val e2y = v2[1] - v0[1]; val e2z = v2[2] - v0[2]
            var nx = e1y * e2z - e1z * e2y
            var ny = e1z * e2x - e1x * e2z
            var nz = e1x * e2y - e1y * e2x
            val nLen = sqrt(nx * nx + ny * ny + nz * nz)
            if (nLen > 0f) { nx /= nLen; ny /= nLen; nz /= nLen }

            val shade = faceShade(fi)
            val cr = r * shade
            val cg = g * shade
            val cb = b * shade

            for (vi in face) {
                vertexList.addAll(verts[vi].toList())
                normalList.addAll(listOf(nx, ny, nz))
                colorList.addAll(listOf(cr, cg, cb, 1f))
            }
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
        colorBuffer = ByteBuffer.allocateDirect(colorList.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(colorList.toFloatArray()); position(0)
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
            attribute vec4 aColor;
            varying vec3 vNormal;
            varying vec4 vColor;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = mat3(uModelMatrix) * aNormal;
                vColor = aColor;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec3 uLightDir;
            varying vec3 vNormal;
            varying vec4 vColor;
            void main() {
                vec3 norm = normalize(vNormal);
                float diff = max(dot(norm, uLightDir), 0.0);
                float ambient = 0.4;
                float light = ambient + (1.0 - ambient) * diff;
                gl_FragColor = vec4(vColor.rgb * light, vColor.a);
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
