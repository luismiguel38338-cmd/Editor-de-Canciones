package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.audio.EqualizerConfig
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import kotlin.math.exp
import kotlin.math.ln

@Composable
fun ParametricEqVisualizer(
    config: EqualizerConfig,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurface)
            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(10.dp))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxDb = 12f
        val dbScale = (height * 0.4f) / maxDb

        // Dibujar líneas de referencia horizontal (0 dB, +6 dB, -6 dB)
        val gridColor = Color(0xFF262A38)
        drawLine(
            color = gridColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor.copy(alpha = 0.5f),
            start = Offset(0f, centerY - 6f * dbScale),
            end = Offset(width, centerY - 6f * dbScale),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor.copy(alpha = 0.5f),
            start = Offset(0f, centerY + 6f * dbScale),
            end = Offset(width, centerY + 6f * dbScale),
            strokeWidth = 1f
        )

        // Calcular curva paramétrica continua
        val path = Path()
        val fillPath = Path()
        val pointsCount = 60

        val totalBass = config.bassGainDb + (config.bassBoostPercent * 0.12f)
        val mid = config.midGainDb
        val treble = config.trebleGainDb

        for (i in 0..pointsCount) {
            val normX = i.toFloat() / pointsCount
            val x = normX * width

            // Aproximación de respuesta espectral:
            // Graves dominan en normX < 0.33
            val bassFactor = (1.0f - (normX / 0.38f)).coerceIn(0f, 1f)
            // Medios dominan en normX ~ 0.5
            val midDist = (normX - 0.5f) / 0.22f
            val midFactor = exp(-(midDist * midDist).toDouble()).toFloat()
            // Agudos dominan en normX > 0.65
            val trebleFactor = ((normX - 0.62f) / 0.38f).coerceIn(0f, 1f)

            val netDb = (totalBass * bassFactor + mid * midFactor + treble * trebleFactor).coerceIn(-14f, 14f)
            val y = centerY - (netDb * dbScale)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, centerY)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (i == pointsCount) {
                fillPath.lineTo(x, centerY)
                fillPath.close()
            }
        }

        // Relleno degradado sutil bajo la curva
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    StudioCobalt.copy(alpha = 0.25f),
                    StudioIndigo.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        // Trazo de la curva de EQ
        drawPath(
            path = path,
            color = StudioCobalt,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
