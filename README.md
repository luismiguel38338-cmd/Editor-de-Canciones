# 🎵 MeloTune — Reproductor Musical Profesional & Editor de Audio (Android)

**MeloTune** es un reproductor de música moderno y profesional para Android desarrollado en **Kotlin**, **Jetpack Compose**, **Material 3** y **AndroidX Media3**. Diseñado con una identidad visual inspirada en estudios musicales de alta fidelidad (negro grafito, superficies carbón y acentos turquesa), combina la velocidad y ergonomía de reproductores de referencia como *Poweramp*, *Musicolet*, *VLC*, *AIMP* y *Oto Music* con un potente motor de edición de audio y procesamiento digital en tiempo real.

---

## ✨ Características Principales

### 1. 🎶 Reproducción Continua y Cola Inteligente
- **Cola Dinámica:** Añadir canciones, reordenar pistas, remover elementos o seleccionar pistas al vuelo sin detener la reproducción.
- **Reproducción Continua:** Transición automática a la siguiente canción al concluir la pista.
- **Modos de Reproducción:** Repetición desactivada, repetir una canción (`Repeat 1`), repetir toda la cola (`Repeat All`) y modo aleatorio (`Shuffle`).
- **Temporizador de Apagado (Sleep Timer):** Apagado gradual automático programable (15, 30, 45, 60 minutos o tiempo personalizado).

### 2. 📱 Pantalla de Reproducción Inmersiva
- Carátula de alta resolución con esquinas redondeadas.
- Metadatos limpios: título, artista, álbum y duración.
- Barra de progreso interactiva con lectura de tiempo transcurrido y restante.
- Botones ergonómicos diseñados para uso fluido con una sola mano.
- Mini-reproductor persistente en la parte inferior durante la navegación entre pestañas.

### 3. 🔔 Integración Nativa Android (Media3 & MediaSession)
- **Servicio en segundo plano:** `PlaybackService` basado en `androidx.media3.session.MediaSessionService`.
- **Controles del Sistema:** Integración con la barra de notificaciones de Android, pantalla de bloqueo y dispositivos conectados (auriculares Bluetooth y controles multimedia del coche).

### 4. 🎨 Identidad Visual y Personalización (MeloTune)
- **Superficies Elegantes:** Negro grafito profundo (`#0B0D13`), superficies carbón y acento turquesa distintivo (`#00E5BF`).
- **Selector de Temas:** Modo Oscuro (predeterminado), Modo Claro y Modo Automático (según sistema).
- **Colores de Acento:** Turquesa MeloTune, Azul Océano, Violeta Neón, Esmeralda y Ámbar.
- **Personalización de Metadatos:** Edición de títulos y artistas mostrados guardados en preferencias sin alterar el archivo original.

### 5. 🎚️ Estudio de Edición & DSP Real
- **Control de Velocidad:** Modificación continua de 0.50x a 2.00x mediante Sonic time-stretching preservando la afinación.
- **Afinador de Tono (Pitch Shifting):** Desplazamiento armónico de ±12 semitonos.
- **Ecualizador Paramétrico de 3 Bandas:** Graves (< 250 Hz), Medios (1 kHz) y Agudos (> 4 kHz) de -12 dB a +12 dB.
- **Refuerzo de Graves (Bass Boost):** Ajuste porcentual de 0% a 100%.
- **Exportación WAV 16-bit:** Procesamiento offline sin compresión guardado en la carpeta de Música, manteniendo 100% intacto el archivo original.

---

## 🏛️ Estructura del Proyecto

```
app/src/main/java/com/example/
├── MainActivity.kt               # Contenedor principal Compose y barra de navegación
├── audio/
│   ├── AudioExporter.kt          # Exportador DSP a WAV PCM 16-bit
│   ├── LiveAudioEffects.kt       # Efectos de audio en hardware
│   └── MusicPlayerManager.kt     # Singleton de ExoPlayer y gestión de estado
├── data/
│   ├── model/                    # Modelos de datos (Song, ExportedTrack)
│   └── repository/               # Repositorios (SongRepository, ExportsRepository, Preferences)
├── service/
│   └── PlaybackService.kt        # Servicio MediaSessionService para Android Media3
└── ui/
    ├── components/               # MiniPlayerBar, SongItem, QueueBottomSheet
    ├── screens/                  # LibraryScreen, PlayerScreen, SongEditorScreen, ExportsScreen, SettingsScreen
    └── theme/                    # Color, Theme, Type con paleta MeloTune
```

---

## 🔒 Nota sobre el Identificador de Paquete
El `applicationId` configurado en `app/build.gradle.kts` es `com.aistudio.musicstudio.zqvlme` con namespace `com.example`. Se mantiene sin cambios para preservar la compatibilidad binaria, firma criptográfica del keystore, continuidad en la instalación del dispositivo y persistencia de las preferencias locales del usuario.
