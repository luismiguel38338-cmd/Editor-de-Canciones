# 🎵 Editor de Canciones (Android)

Aplicación nativa para Android desarrollada en **Kotlin** con **Jetpack Compose**, **Material 3** y arquitectura **MVVM**. Permite reproducir canciones de la biblioteca del dispositivo, modificar velocidad y tono (*pitch*) en tiempo real y exportar copias procesadas sin alterar los archivos originales.

---

## 🚀 Características Principales

- **Pantalla de Inicio con Animación Neón:**
  - Animación de entrada fluida con ondas sonoras en expansión, tocadiscos de vinilo giratorio y barras ecualizadoras pulsantes.
  - Transición suave hacia la biblioteca o acceso inmediato tocando "Entrar al Estudio".

- **Lectura y Exploración de Canciones (MediaStore):**
  - Solicitud de permisos en tiempo de ejecución (`READ_MEDIA_AUDIO` en Android 13+ y `READ_EXTERNAL_STORAGE` en versiones anteriores).
  - Consulta automática a `MediaStore` con título, artista, duración y carátula de álbum.
  - Pestañas organizadas: *Canciones* y *Mis Ediciones*.
  - Búsqueda en tiempo real por título o artista.
  - Selector directo de archivos de audio locales (`audio/*`) y pistas de prueba sintetizadas integradas.

- **Reproductor de Estudio (Media3 ExoPlayer):**
  - Reproductor con tocadiscos animado, visualizador de forma de onda (*WaveformVisualizer*), scrubber de posición, salto de ±5 segundos y modo de repetición en bucle.

- **Controles en Tiempo Real:**
  - **Velocidad:** Selector deslizante continuo de 0.50x a 2.00x con accesos directos (0.75x, 1.0x, 1.25x, 1.5x, 2.0x).
  - **Tono (Pitch):** Ajuste de -12 a +12 semitonos con botones paso a paso (+ / -) y presets (-5st, -2st, Original, +2st, +5st, +7st).
  - Modificación instantánea de `PlaybackParameters(speed, pitch)` sin interrumpir la reproducción.

- **Exportación No Destructiva:**
  - Procesamiento con `MediaExtractor`, decodificador `MediaCodec` y `SonicAudioProcessor` de Media3.
  - Genera una copia en formato WAV de 16 bits sin pérdidas guardada en la carpeta de Música (`Music/EditorDeCanciones`) vía `MediaStore`.
  - **El archivo original nunca es sobrescrito ni modificado.**

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 2.x
- **UI:** Jetpack Compose con Material Design 3 (Dark Studio Theme)
- **Audio Engine:** AndroidX Media3 (ExoPlayer & Sonic Audio Processor)
- **Arquitectura:** MVVM (Model-View-ViewModel) + StateFlow
- **Carga de Imágenes:** Coil Compose
- **Testing:** JUnit 4 + Robolectric

---

## 📦 Compilación y Ejecución

### Prerrequisitos
- Android Studio Ladybug / Meerkat o superior
- JDK 17 o superior
- Android SDK con `compileSdk 36` y `minSdk 24`

### Compilar desde la terminal con Gradle
```bash
# Dar permisos de ejecución al wrapper (Linux/macOS)
chmod +x gradlew

# Ejecutar pruebas unitarias
./gradlew testDebugUnitTest

# Compilar APK en modo Debug
./gradlew assembleDebug

# El archivo generado estará en:
# app/build/outputs/apk/debug/app-debug.apk
```

### Compilar en Windows
```cmd
gradlew.bat assembleDebug
```

---

## 🤖 Integración Continua (GitHub Actions)

El repositorio incluye un flujo de trabajo automatizado en `.github/workflows/android.yml`:
1. Configura el entorno con Java 17.
2. Ejecuta automáticamente los tests unitarios y de Robolectric.
3. Compila el APK `app-debug.apk`.
4. Sube el APK como artefacto descargable en las ejecuciones de GitHub Actions.
