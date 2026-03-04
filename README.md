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

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (ViewModel + Repository) |
| Base de datos | Room |
| Inyección de dependencias | Hilt |
| Navegación | Navigation Compose |
| Almacenamiento de imágenes | Ficheros internos de la app |
| i18n | Android Resources con `AppCompatDelegate.setApplicationLocales()` |

## Requisitos

- Android 8.0+ (API 26)
- Android Studio Ladybug o superior
- JDK 17

## Estructura del proyecto

```
app/src/main/
├── java/com/qnotes/
│   ├── data/           # Room DB, DAOs, entities, repository
│   ├── di/             # Módulos Hilt
│   ├── service/        # QuickSettingsTileService
│   ├── ui/
│   │   ├── capture/    # Pantalla de captura rápida
│   │   ├── home/       # Pantalla principal (lista de items)
│   │   ├── detail/     # Detalle/edición de un item
│   │   ├── settings/   # Configuración (idioma, categorías)
│   │   └── theme/      # Theme Material 3
│   └── util/           # Extensiones, helpers i18n
├── res/
│   ├── values/           # strings.xml (inglés - fallback)
│   ├── values-es/        # strings.xml (español)
│   ├── values-ca/        # strings.xml (catalán)
│   └── ...
└── AndroidManifest.xml
```

## Licencia

MIT

## Estado

🚧 En desarrollo
