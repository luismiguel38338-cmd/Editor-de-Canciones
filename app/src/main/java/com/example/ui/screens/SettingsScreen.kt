package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentColorChoice
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ControlSizeChoice
import com.example.ui.theme.MeloTurquoise
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    themeMode: AppThemeMode,
    accentColorChoice: AccentColorChoice,
    controlSizeChoice: ControlSizeChoice = ControlSizeChoice.NORMAL,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onAccentColorChanged: (AccentColorChoice) -> Unit,
    onControlSizeChanged: (ControlSizeChoice) -> Unit = {},
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Título limpio
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN: APARIENCIA
        SectionHeader("Apariencia y Tema")

        // Modo de tema
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppThemeMode.values().forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChanged(mode) },
                    label = { Text(mode.displayName, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeloTurquoise.copy(alpha = 0.2f),
                        selectedLabelColor = MeloTurquoise,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color de acento
        Text(
            text = "Color de Acento",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentColorChoice.values().forEach { choice ->
                val isSelected = accentColorChoice == choice
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onAccentColorChanged(choice) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(choice.color)
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = choice.displayName.substringBefore(" "),
                        fontSize = 10.sp,
                        color = if (isSelected) MeloTurquoise else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN: REPRODUCCIÓN Y SONIDO
        SectionHeader("Reproducción y Sonido")

        CleanSettingItem(
            icon = Icons.Default.Equalizer,
            title = "Ecualizador Biquad IIR",
            subtitle = "Ajuste de graves, medios, agudos y refuerzo activo"
        )

        CleanSettingItem(
            icon = Icons.Default.Headphones,
            title = "Pausa con Auriculares",
            subtitle = "Pausa la reproducción automáticamente al desconectar"
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN: ALMACENAMIENTO Y BIBLIOTECA
        SectionHeader("Biblioteca y Almacenamiento")

        CleanSettingItem(
            icon = Icons.Default.Security,
            title = "Archivos de Audio Intactos",
            subtitle = "MeloTune lee los archivos sin alterar tus grabaciones originales"
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onClearCache,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("clear_cache_button"),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioSurfaceBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextPrimary,
                containerColor = StudioSurface
            )
        ) {
            Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MeloTurquoise
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Optimizar memoria y caché",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN: INFORMACIÓN
        SectionHeader("Información")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("MeloTune", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text("Reproductor musical para Android", color = TextSecondary, fontSize = 12.sp)
            }
            Surface(
                color = StudioSurfaceHighlight,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "V2 Oficial",
                    color = MeloTurquoise,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            color = MeloTurquoise,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        ),
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun CleanSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(StudioSurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MeloTurquoise,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
