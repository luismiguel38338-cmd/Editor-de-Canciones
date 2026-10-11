# 🎛️ Editor de Canciones (Android)

Aplicación nativa para Android desarrollada en **Kotlin** con **Jetpack Compose**, **Material 3** y arquitectura **MVVM**. Ofrece un entorno de producción musical sobrio y profesional para reproducir, editar velocidad y tono en tiempo real, aplicar ecualización paramétrica de 3 bandas con refuerzo de graves y exportar copias procesadas sin alterar los archivos de origen.

---

## 🏛️ Arquitectura y Secciones de la Aplicación

La aplicación se estructura en 4 áreas principales accesibles mediante una barra de navegación ergonómica y un **Mini-Reproductor Persistente**:

1. **Biblioteca:**
   - Exploración de la colección musical del dispositivo vía `MediaStore`.
   - Búsqueda en tiempo real por título y artista.
   - Ordenamiento avanzado por Título (A-Z), Artista o Duración.
   - Importación directa de archivos de audio locales (`audio/*`) y pistas de prueba sintetizadas para demostración inmediata.

2. **Estudio / Editor & Reproductor:**
   - **Reproductor y Transporte:** Scrubber de línea de tiempo con lectura precisa en milisegundos, control de volumen con atenuación de pico, salto de ±5s y repetición en bucle.
   - **Pestaña de Tono y Velocidad:** Fader continuo de velocidad (0.50x a 2.00x) y afinador de semitonos (-12st a +12st) con botones de paso fino y presets.
   - **Pestaña de Ecualización Paramétrica & Graves:**
     - Curva visual de respuesta en frecuencia paramétrica en tiempo real.
     - 3 bandas de ecualización en escala de decibelios (-12 dB a +12 dB): Graves (< 250 Hz), Medios (~1 kHz) y Agudos (> 4 kHz).
     - Refuerzo de subgraves (*Bass Boost* de 0% a 100%).
     - Restablecimiento instantáneo a curva plana (0 dB).
   - **Exportación Procesada:** Renderizado en segundo plano con indicador porcentual sin congelar la interfaz.

3. **Exportaciones:**
   - Historial y gestor de todas las pistas procesadas en `Música / EditorDeCanciones`.
   - Fichas técnicas con tamaño exacto en MB, duración y fecha/hora de renderizado.
   - Reproducción directa, opción de compartir archivo mediante la hoja de Android (`Intent.ACTION_SEND`) y eliminación con confirmación.

4. **Ajustes:**
   - Información técnica del pipeline DSP (WAV 16-bit PCM, Sonic, Filtros IIR Biquad Direct Form II).
   - Verificación de la integridad de los archivos originales (estrictamente de solo lectura).
   - Herramienta de optimización y limpieza de caché temporal de renderizado.

---

## 🎧 Procesamiento Digital de Audio (DSP) Real

A diferencia de interfaces meramente decorativas, el procesamiento es **real**:
- **Reproducción en vivo:** `ExoPlayer` aplica `PlaybackParameters` para la velocidad y el tono con el motor Sonic, y se acopla a `android.media.audiofx.Equalizer` y `BassBoost` para la ecualización analógica en hardware.
- **Exportación sin pérdidas:** Decodificación a PCM 16-bit mediante `MediaExtractor` y `MediaCodec`. Los buffers pasan por `SonicAudioProcessor` y por filtros digitales IIR Biquad (`LowShelf`, `Peaking`, `HighShelf`) antes de empaquetarse en un contenedor WAV RIFF estándar.

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 2.x
- **UI Toolkit:** Jetpack Compose (Material 3 Dark Graphite & Charcoal)
- **Audio Core:** AndroidX Media3 (ExoPlayer 1.5.1 + Sonic DSP)
- **Filtros DSP:** Biquad IIR Direct Form II implementado en PCM
- **Arquitectura:** MVVM (Model-View-ViewModel) con `StateFlow` reactivo
- **Pruebas:** JUnit 4 + Robolectric JVM

---

## 📦 Compilación y Pruebas

```bash
# Otorgar permisos de ejecución
chmod +x gradlew

# Ejecutar pruebas unitarias y de DSP
./gradlew testDebugUnitTest

# Compilar APK de depuración
./gradlew assembleDebug
```
El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.
