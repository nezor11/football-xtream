# Football Xtream

Reproductor IPTV libre para **Android TV / Google TV, Fire TV y móvil/tablet Android**, con un
enfoque *TV-first* (mando a distancia, foco D-pad) y centrado en **deporte / fútbol en directo**.
Soporta tanto el protocolo **Xtream Codes** como **listas M3U/M3U-plus**.

> **Estado:** funcional. Login (Xtream o M3U) → navegación de canales de deporte → reproducción,
> con audio Dolby por software, buscador, carpetas con logos y "continuar viendo". Probado en un
> Chromecast con Google TV. Ver [Roadmap](#roadmap).

## Características

- **Dos formas de conectar**: perfil **Xtream** (servidor + usuario + contraseña) o **lista M3U**
  (pega la URL). Varios perfiles guardados que se eligen desde un selector; se pueden **renombrar
  y editar** (mantener pulsado un perfil → *Editar* / *Eliminar*).
- **Solo deporte**: filtra automáticamente los canales de deporte/fútbol (palabras clave
  multiidioma) y **descarta el resto, el VOD (películas/series) y canales generales mal
  categorizados**.
- **Carpetas por marca**: agrupa las versiones de un canal (p. ej. *beIN Sports 1/2/3…*) en una
  carpeta con su logo. Los logos que la lista no trae se buscan en la base de datos pública
  **iptv-org** y se cachean en disco.
- **Deduplicación por calidad**: las variantes del mismo canal (4K/2K/FHD/HD/SD) se unen en un
  canal lógico, incluso cuando la calidad va pegada al nombre (`…TVHD`).
- **Audio por software (FFmpeg)**: decodifica **AC-3 / E-AC-3 (Dolby), MP2 y DTS**, para que
  canales que el dispositivo no sabe decodificar (típico de DAZN/Movistar) **tengan sonido**.
- **Buscador** para localizar canales por nombre al instante.
- **Continuar viendo**: recuerda y reanuda el último canal visto.
- **Reproductor TV**:
  - **Zapping con el mando** (flechas arriba/abajo/izq/der = cambiar de canal).
  - **Selector de calidad** con OK (Auto · 4K · 2K · FHD · HD · SD del canal).
  - **Calidad automática** según el ancho de banda medido, con bajada automática si hay cortes; si
    una variante falla (stream muerto), prueba otra y, si todas fallan, avisa.
  - **Overlay discreto** (canal · calidad · Mbps · resolución) abajo-izquierda para no tapar el
    marcador; tasa de transferencia que se refresca a menudo.
  - **EPG "Ahora / Luego"**: vía la API en perfiles **Xtream**, y vía **XMLTV** en listas **M3U**
    que declaren su guía (`x-tvg-url` + `tvg-id`), descargada y parseada en segundo plano.
  - Pre-buffering ampliado para suavizar la reproducción IPTV.
- **Favoritos** (mantener pulsado una tarjeta).
- UI con **Jetpack Compose for TV**.

## Stack

- **Kotlin** + **Jetpack Compose for TV** (`androidx.tv:tv-material`)
- **Media3 / ExoPlayer** (+ HLS) con **extensión FFmpeg** (`io.github.anilbeesetti:nextlib-media3ext`)
  para los códecs de audio de IPTV (AC-3/E-AC-3/MP2/DTS)
- **Retrofit + OkHttp + kotlinx.serialization** para la API Xtream; parser M3U propio
- **Room** (perfiles y favoritos, con migraciones para no perder datos al actualizar) y
  **DataStore** (ajustes: calidad, último canal)
- **Coil** para imágenes; logos vía la base de datos pública **iptv-org**
- Arquitectura **MVVM** con inyección manual de dependencias (`AppContainer`)

## Cómo añadir tu lista

En **Añadir** elige el modo:

- **Xtream**: `URL del servidor` (`http://host:puerto`), `usuario` y `contraseña`.
- **Lista M3U**: pega la URL de tu lista (`…/get.php?...&type=m3u_plus` o similar).

> Nota: algunos proveedores sirven la API Xtream pero bloquean su CDN de `/live/` para clientes
> genéricos; en ese caso usa el modo **M3U**, que suele funcionar.

## Requisitos para compilar

Se compila con **Android Studio** (recomendado, trae JDK 17 y el SDK):

1. Instala [Android Studio](https://developer.android.com/studio) y abre la carpeta del proyecto.
2. Android Studio descargará el SDK (compileSdk 35) y sincronizará Gradle.
3. Ejecuta en un **Android TV / Fire TV / móvil** real por ADB (recomendado) o un emulador.

Por línea de comandos (requiere JDK 17 y el Android SDK con `local.properties`):

```bash
./gradlew assembleDebug
```

El APK se divide **por ABI** para reducir su tamaño (FFmpeg pesa por arquitectura), así que se
generan `app-arm64-v8a-debug.apk` y `app-armeabi-v7a-debug.apk` (sin APK universal). La mayoría de
Android TV / Fire TV usan **arm64-v8a**.

> minSdk 24 (Android 7.0). Los Fire TV muy antiguos con Fire OS 5 (Android 5.1) no están soportados.

## Roadmap

- [ ] **Catch-up / timeshift** (`has_archive`).
- [ ] **Cifrado de credenciales en reposo.**
- [ ] **Chromecast opcional** (desacoplado para no depender de Google Play Services en Fire TV).
- [ ] (Opcional) VOD / Películas y Series, si se amplía más allá del deporte.

Hecho recientemente: migraciones de Room (sin borrado destructivo), EPG vía XMLTV para listas M3U,
edición/renombrado de perfiles.

## Aviso legal

Football Xtream es un **reproductor cliente neutro** (como VLC): no incluye, aloja ni distribuye
ningún contenido ni listas. El usuario es el único responsable de los servidores y credenciales
que configure y del contenido al que acceda, así como de cumplir la legislación aplicable.

## Licencia

Distribuido bajo **GPL-3.0**. Ver [LICENSE](LICENSE).
