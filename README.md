# Football Xtream

Reproductor IPTV libre para **Android TV / Google TV, Fire TV y móvil/tablet Android**, que se
conecta a servidores mediante el protocolo **Xtream Codes**. Pensado para una experiencia
*TV-first* (mando a distancia, foco D-pad) con un enfoque inicial en contenido deportivo.

> **Estado:** MVP en construcción. Funciona el flujo **login → TV en directo → reproducción**,
> con favoritos. Ver [Roadmap](#roadmap).

## Características

Disponible en el MVP actual:
- **Selector de perfiles** sencillo (varios servidores Xtream guardados; elegir = entrar).
- Pantalla **solo deporte**: filtra automáticamente los canales de deporte/fútbol y oculta el resto.
- **Deduplicación por calidad**: las múltiples versiones del mismo canal (4K/FHD/HD/SD) se agrupan en un único canal lógico.
- **Filtro de calidad** con chips `Auto · 4K · FHD · HD · SD · Todas`.
- **Calidad automática según la red** (modo Auto): elige la mejor variante que cabe en tu ancho de banda y baja de calidad sola si detecta cortes.
- **Indicador de transferencia** en el reproductor (Mbps en vivo + resolución) para confirmar que está descargando.
- **Pre-buffering** ampliado para suavizar la reproducción IPTV.
- **Favoritos** (mantener pulsado una tarjeta).
- UI con **Jetpack Compose for TV** y navegación por mando.

## Stack

- **Kotlin** + **Jetpack Compose for TV** (`androidx.tv:tv-material`)
- **Media3 / ExoPlayer** (+ HLS) para reproducción
- **Retrofit + OkHttp + kotlinx.serialization** para la API Xtream
- **Room** (favoritos) y **DataStore** (perfil)
- Arquitectura **MVVM** con inyección manual de dependencias (`AppContainer`)
- **Coil** para imágenes

## Requisitos para compilar

Este proyecto se compila con **Android Studio** (recomendado, trae JDK 17 y el SDK):

1. Instala [Android Studio](https://developer.android.com/studio).
2. Abre la carpeta del proyecto. Android Studio descargará el SDK de Android necesario
   (compileSdk 35) y sincronizará Gradle automáticamente.
3. Ejecuta sobre:
   - un **emulador de Android TV** (Device Manager → Android TV 1080p), o
   - un dispositivo real (Android TV, Fire TV o móvil) por ADB.

Por línea de comandos (requiere JDK 17 y el Android SDK con `local.properties` apuntando a él):

```bash
./gradlew assembleDebug
```

> minSdk 24 (Android 7.0). Los Fire TV muy antiguos con Fire OS 5 (Android 5.1) no están
> soportados en esta versión.

## Roadmap

- [ ] **EPG / Guía** de programación (`get_short_epg`, `get_simple_data_table`).
- [ ] Búsqueda de canales y mejoras en la detección de deportes/fútbol.
- [ ] Recordar la última calidad por canal y mejorar la lógica de auto-bajada.
- [ ] Soporte de **catch-up / timeshift**.
- [ ] Decodificación ampliada con la **extensión FFmpeg de Media3** (códecs MPEG-TS poco comunes;
      requiere compilar la extensión con el NDK).
- [ ] Chromecast opcional (desacoplado para no depender de Google Play Services en Fire TV).
- [ ] Cifrado de credenciales en reposo.
- [ ] (Opcional) VOD / Películas y Series, si se decide ampliar más allá del deporte.

## Aviso legal

Football Xtream es un **reproductor cliente neutro** (como VLC): no incluye, aloja ni distribuye
ningún contenido ni listas. El usuario es el único responsable de los servidores y credenciales
que configure y del contenido al que acceda, así como de cumplir la legislación aplicable.

## Licencia

Distribuido bajo **GPL-3.0**. Ver [LICENSE](LICENSE).
