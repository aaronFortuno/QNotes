# QNotes

**Captura rápida de ideas, notas y capturas de pantalla en Android. En 1-2 toques, desde cualquier pantalla.**

QNotes es una aplicación Android nativa que permite guardar contenido (notas, capturas de pantalla, enlaces, texto del portapapeles) con el mínimo número de toques posible, directamente desde cualquier pantalla del dispositivo.

## Motivación

Guardar una idea o referencia interesante no debería requerir abrir una app, navegar menús, crear un documento y pegar contenido. QNotes reduce ese proceso a 1-2 toques mediante integración directa con el sistema Android.

## Características principales

- **Quick Settings Tile**: botón en el panel de ajustes rápidos para lanzar la captura instantánea.
- **Share Target**: recibe contenido compartido desde cualquier aplicación (imágenes, texto, URLs).
- **Captura desde portapapeles**: pega automáticamente lo que tengas copiado.
- **Notas rápidas**: campo de texto para anotar ideas al vuelo.
- **Organización**: categorías, etiquetas y búsqueda sobre todo el contenido guardado.
- **Almacenamiento 100% local**: base de datos Room, sin dependencias de servicios externos.
- **Multiidioma (i18n)**: detección automática del idioma del dispositivo con opción de selección manual independiente del idioma del sistema.

## Stack técnico

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin (built-in AGP 9) | 2.2.10 |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Build system | AGP | 9.1.0 |
| Arquitectura | MVVM (ViewModel + Repository) | — |
| Base de datos | Room | 2.8.4 |
| Inyección de dependencias | Hilt (Dagger) | 2.59.2 |
| Procesador de anotaciones | KSP 2 | 2.2.10-2.0.2 |
| Navegación | Navigation Compose | 2.8.5 |
| Coroutines | kotlinx-coroutines | 1.9.0 |
| Almacenamiento de imágenes | Ficheros internos de la app | — |
| i18n | Android Resources + `AppCompatDelegate` | — |

## Requisitos

- Android 8.0+ (API 26)
- Android Studio Meerkat (2025.1) o superior
- JDK 17
- Gradle 9.3.1+

## Estructura del proyecto

```
app/src/main/java/com/aaronfortuno/studio/qnotes/
├── QNotesApplication.kt          # @HiltAndroidApp
├── MainActivity.kt               # @AndroidEntryPoint + NavHost
├── data/
│   ├── local/
│   │   ├── VaultDatabase.kt      # @Database (VaultItem, Category)
│   │   ├── VaultItemDao.kt       # @Dao — CRUD para VaultItem
│   │   └── CategoryDao.kt        # @Dao — CRUD para Category
│   ├── model/
│   │   ├── VaultItem.kt          # @Entity — item principal
│   │   ├── Category.kt           # @Entity — categoría
│   │   └── ItemType.kt           # enum: NOTE, PASSWORD, FILE
│   ├── repository/
│   │   └── ItemRepository.kt     # Repositorio con inyección Hilt
│   └── storage/
│       └── FileStorage.kt        # Almacenamiento de ficheros
├── di/
│   └── DatabaseModule.kt         # @Module Hilt — DB, DAOs
├── service/                       # QuickSettingsTileService (Fase 2)
├── ui/
│   ├── capture/
│   │   └── CaptureScreen.kt      # Pantalla de captura rápida
│   ├── home/
│   │   └── HomeScreen.kt         # Pantalla principal (lista)
│   ├── detail/
│   │   └── DetailScreen.kt       # Detalle/edición de item
│   ├── settings/
│   │   └── SettingsScreen.kt     # Configuración
│   ├── navigation/
│   │   └── NavGraph.kt           # NavHost con rutas
│   └── theme/                     # Theme Material 3
└── util/                          # Extensiones, helpers (Fase 6+)
```

## Compilar

```bash
./gradlew assembleDebug
```

## Licencia

MIT

## Estado

En desarrollo — Fase 0 (scaffolding) completada.
