# Football Xtream

Reproductor IPTV libre para **Android TV / Google TV, Fire TV y móvil/tablet Android**, que se
conecta a servidores mediante el protocolo **Xtream Codes**. Pensado para una experiencia
*TV-first* (mando a distancia, foco D-pad) con un enfoque inicial en contenido deportivo.

> **Estado:** MVP en construcción. Funciona el flujo **login → TV en directo → reproducción**,
> con favoritos. Ver [Roadmap](#roadmap).

## Características

Disponible en el MVP actual:
- Login Xtream (servidor, usuario, contraseña) con sesión persistente.
- Listado de **TV en directo** por categorías, con orden "deportes primero".
- **Favoritos** de canales (mantener pulsado en una tarjeta).
- Reproducción con **Media3 / ExoPlayer**.
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
- [ ] **VOD / Películas** y **Series** (con "continuar viendo").
- [ ] Historial y "añadido recientemente".
- [ ] Búsqueda y sección/atajos de deportes.
- [ ] Soporte de **catch-up / timeshift**.
- [ ] Decodificación ampliada con la **extensión FFmpeg de Media3** (códecs MPEG-TS poco comunes;
      requiere compilar la extensión con el NDK).
- [ ] Chromecast opcional (desacoplado para no depender de Google Play Services en Fire TV).
- [ ] Múltiples perfiles y cifrado de credenciales en reposo.

## Aviso legal

Football Xtream es un **reproductor cliente neutro** (como VLC): no incluye, aloja ni distribuye
ningún contenido ni listas. El usuario es el único responsable de los servidores y credenciales
que configure y del contenido al que acceda, así como de cumplir la legislación aplicable.

## Licencia

Distribuido bajo **GPL-3.0**. Ver [LICENSE](LICENSE).
