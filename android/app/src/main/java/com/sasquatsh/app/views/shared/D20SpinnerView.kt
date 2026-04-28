package com.sasquatsh.app.views.shared

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A spinning D20 (icosahedron) rendered with Compose Canvas.
 * Pure Compose — no GLSurfaceView, no transparency artifacts.
 */
@Composable
fun D20SpinnerView(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    color: Color = Color(0xFF6366F1),
    numberColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "d20spin")
    val rawAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 35000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "d20angle"
    )
    val angle = rawAngle % 360f

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val scale = this.size.minDimension / 3.2f

        drawD20(cx, cy, scale, angle, color, numberColor)
    }
}

private fun DrawScope.drawD20(
    cx: Float, cy: Float, scale: Float,
    angleDeg: Float,
    faceColor: Color, numColor: Color
) {
    val phi = (1f + sqrt(5f)) / 2f
    val s = 0.75f

    // Icosahedron vertices
    val rawVerts = arrayOf(
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

    val faces = arrayOf(
        intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7),
        intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
        intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2),
        intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
        intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6),
        intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
        intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10),
        intArrayOf(8, 6, 7), intArrayOf(9, 8, 1)
    )

    val numbers = intArrayOf(20, 8, 14, 2, 17, 1, 13, 7, 19, 4, 16, 10, 6, 18, 12, 5, 11, 15, 3, 9)

    // Rotation axis (matching iOS: 0.3, 1, 0.15 normalized)
    val ax = 0.3f; val ay = 1f; val az = 0.15f
    val axLen = sqrt(ax * ax + ay * ay + az * az)
    val ux = ax / axLen; val uy = ay / axLen; val uz = az / axLen

    val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
    val cosA = cos(rad); val sinA = sin(rad)

    // Rotate vertices using Rodrigues' rotation formula
    fun rotate(v: FloatArray): FloatArray {
        val dot = ux * v[0] + uy * v[1] + uz * v[2]
        val cx2 = uy * v[2] - uz * v[1]
        val cy2 = uz * v[0] - ux * v[2]
        val cz2 = ux * v[1] - uy * v[0]
        return floatArrayOf(
            v[0] * cosA + cx2 * sinA + ux * dot * (1 - cosA),
            v[1] * cosA + cy2 * sinA + uy * dot * (1 - cosA),
            v[2] * cosA + cz2 * sinA + uz * dot * (1 - cosA)
        )
    }

    val rotated = rawVerts.map { rotate(it) }

    // Light direction (normalized)
    val lx = 0.37f; val ly = 0.56f; val lz = 0.74f

    // Collect face data for depth sorting
    data class FaceData(
        val index: Int,
        val p0: FloatArray, val p1: FloatArray, val p2: FloatArray,
        val depth: Float, val brightness: Float
    )

    val faceList = mutableListOf<FaceData>()

    for ((fi, face) in faces.withIndex()) {
        val v0 = rotated[face[0]]
        val v1 = rotated[face[1]]
        val v2 = rotated[face[2]]

        // Face normal
        val e1x = v1[0] - v0[0]; val e1y = v1[1] - v0[1]; val e1z = v1[2] - v0[2]
        val e2x = v2[0] - v0[0]; val e2y = v2[1] - v0[1]; val e2z = v2[2] - v0[2]
        var nx = e1y * e2z - e1z * e2y
        var ny = e1z * e2x - e1x * e2z
        var nz = e1x * e2y - e1y * e2x
        val nLen = sqrt(nx * nx + ny * ny + nz * nz)
        if (nLen > 0f) { nx /= nLen; ny /= nLen; nz /= nLen }

        // Back-face culling (skip faces pointing away from camera)
        if (nz <= 0f) continue

        val avgZ = (v0[2] + v1[2] + v2[2]) / 3f
        val diffuse = maxOf(0f, nx * lx + ny * ly + nz * lz)
        val brightness = 0.35f + 0.65f * diffuse

        faceList.add(FaceData(fi, v0, v1, v2, avgZ, brightness))
    }

    // Depth sort (painter's algorithm — draw far faces first)
    faceList.sortBy { it.depth }

    val textPaint = android.graphics.Paint().apply {
        color = numColor.hashCode()
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    for (fd in faceList) {
        // Project 3D → 2D
        val x0 = cx + fd.p0[0] * scale; val y0 = cy - fd.p0[1] * scale
        val x1 = cx + fd.p1[0] * scale; val y1 = cy - fd.p1[1] * scale
        val x2 = cx + fd.p2[0] * scale; val y2 = cy - fd.p2[1] * scale

        // Shade face color by brightness
        val r = ((faceColor.red * fd.brightness * 255).toInt().coerceIn(0, 255))
        val g = ((faceColor.green * fd.brightness * 255).toInt().coerceIn(0, 255))
        val b = ((faceColor.blue * fd.brightness * 255).toInt().coerceIn(0, 255))
        val shadedColor = Color(r, g, b)

        val path = Path().apply {
            moveTo(x0, y0)
            lineTo(x1, y1)
            lineTo(x2, y2)
            close()
        }

        // Fill
        drawPath(path, shadedColor, style = Fill)
        // Edge
        drawPath(path, Color.White.copy(alpha = 0.15f), style = Stroke(width = 0.8f))

        // Number at centroid
        val centX = (x0 + x1 + x2) / 3f
        val centY = (y0 + y1 + y2) / 3f

        // Scale text to face size
        val faceSize = maxOf(
            sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)),
            sqrt((x2 - x0) * (x2 - x0) + (y2 - y0) * (y2 - y0))
        )
        val num = numbers[fd.index]
        textPaint.textSize = faceSize * (if (num >= 10) 0.28f else 0.35f)
        textPaint.color = android.graphics.Color.argb(
            (255 * fd.brightness).toInt().coerceIn(100, 255),
            (numColor.red * 255).toInt(),
            (numColor.green * 255).toInt(),
            (numColor.blue * 255).toInt()
        )

        drawContext.canvas.nativeCanvas.drawText(
            "$num",
            centX,
            centY + textPaint.textSize / 3f,
            textPaint
        )
    }
}
