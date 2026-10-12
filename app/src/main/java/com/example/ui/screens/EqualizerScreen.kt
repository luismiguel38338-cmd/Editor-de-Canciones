package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.EqualizerConfig
import com.example.ui.components.ParametricEqVisualizer
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class EqPreset(val label: String, val bass: Float, val mid: Float, val treble: Float, val boost: Int) {
    FLAT("Plano", 0f, 0f, 0f, 0),
    BASS_BOOST("Graves +", 6f, 0f, -1f, 40),
    VOCAL("Vocal", -2f, 5f, 2f, 0),
    ROCK("Rock", 4f, -2f, 4f, 15),
    ELECTRONIC("Electrónica", 6f, 1f, 5f, 35),
    ACOUSTIC("Acústico", 2f, 2f, 3f, 0)
}

@Composable
fun EqualizerScreen(
    equalizerConfig: EqualizerConfig,
    onBassGainChanged: (Float) -> Unit,
    onMidGainChanged: (Float) -> Unit,
    onTrebleGainChanged: (Float) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onResetEqualizer: () -> Unit,
    onApplyPreset: (Float, Float, Float, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBg)
    ) {
        // Encabezado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Ecualizador Profesional",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Filtros IIR Biquad Direct Form II en tiempo real",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary
                    )
                )
            }

            OutlinedButton(
                onClick = onResetEqualizer,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StudioSurfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary,
                    containerColor = StudioSurface
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("reset_equalizer_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Plano", fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Visualizador Gráfico de Respuesta de Frecuencia
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                ParametricEqVisualizer(
                    config = equalizerConfig,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Presets predefinidos
            Text(
                text = "Ajustes Predefinidos",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EqPreset.values().take(4).forEach { preset ->
                    val isSelected = equalizerConfig.bassGainDb == preset.bass &&
                            equalizerConfig.midGainDb == preset.mid &&
                            equalizerConfig.trebleGainDb == preset.treble &&
                            equalizerConfig.bassBoostPercent == preset.boost

                    FilterChip(
                        selected = isSelected,
                        onClick = { onApplyPreset(preset.bass, preset.mid, preset.treble, preset.boost) },
                        label = { Text(preset.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = StudioSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = StudioSurfaceBorder,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Banda 1: Graves (Low Shelf < 250 Hz)
            EqBandCard(
                title = "Graves (Low Shelf < 250 Hz)",
                gainDb = equalizerConfig.bassGainDb,
                onGainChanged = onBassGainChanged,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Banda 2: Medios (Peaking 1 kHz)
            EqBandCard(
                title = "Medios (Peaking ~1 kHz)",
                gainDb = equalizerConfig.midGainDb,
                onGainChanged = onMidGainChanged,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Banda 3: Agudos (High Shelf > 4 kHz)
            EqBandCard(
                title = "Agudos (High Shelf > 4 kHz)",
                gainDb = equalizerConfig.trebleGainDb,
                onGainChanged = onTrebleGainChanged,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Control de Refuerzo de Graves (Bass Boost)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SurroundSound,
                                contentDescription = null,
                                tint = StudioIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Refuerzo Dinámico de Graves (Bass Boost)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            color = StudioIndigo.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${equalizerConfig.bassBoostPercent}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Slider(
                        value = equalizerConfig.bassBoostPercent.toFloat(),
                        onValueChange = { onBassBoostChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioIndigo,
                            activeTrackColor = StudioIndigo,
                            inactiveTrackColor = StudioSurfaceBorder
                        ),
                        modifier = Modifier.testTag("bass_boost_slider")
                    )

                    Text(
                        text = "Potencia armónica en bajas frecuencias sin saturación digital.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun EqBandCard(
    title: String,
    gainDb: Float,
    onGainChanged: (Float) -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurface)
            .border(1.dp, StudioSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    val sign = if (gainDb > 0) "+" else ""
                    Text(
                        text = "$sign${gainDb.toInt()} dB",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Slider(
                value = gainDb,
                onValueChange = onGainChanged,
                valueRange = -12f..12f,
                steps = 23,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = StudioSurfaceBorder
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-12 dB", fontSize = 10.sp, color = TextMuted)
                Text("0 dB", fontSize = 10.sp, color = TextMuted)
                Text("+12 dB", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}
