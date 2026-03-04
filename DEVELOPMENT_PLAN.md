# Plan de desarrollo — QNotes

## Resumen de fases

| Fase | Alcance | Estado |
|---|---|---|
| 0 | Scaffolding del proyecto | Completada |
| 1 | Modelo de datos y almacenamiento | Pendiente |
| 2 | **Captura rápida + Quick Settings Tile** | Pendiente |
| 3 | Pantalla principal (Home) | Pendiente |
| 4 | Detalle y edición | Pendiente |
| 5 | Share Target | Pendiente |
| 6 | Internacionalización | Pendiente |
| 7 | Pulido y release | Pendiente |

---

## Fase 0 — Scaffolding del proyecto (COMPLETADA)

**Objetivo**: proyecto Android compilable con la estructura base lista.

**Resultado**:
- Proyecto configurado con AGP 9.1.0, Kotlin 2.2.10 (built-in en AGP 9), Compose BOM 2024.12.01, Material 3.
- Dependencias configuradas en `gradle/libs.versions.toml`: Hilt 2.59.2, Room 2.8.4, Navigation Compose 2.8.5, KSP 2.2.10-2.0.2, Coroutines 1.9.0.
- Estructura de paquetes creada: `data/`, `di/`, `service/`, `ui/`, `util/`.
- Hilt configurado: `@HiltAndroidApp` en `QNotesApplication`, `@AndroidEntryPoint` en `MainActivity`, `DatabaseModule` con providers para Room DB, DAOs.
- `NavGraph` con rutas funcionales: Home, Capture, Detail, Settings.
- Entidades Room placeholder: `VaultItem`, `Category`, `ItemType`.
- DAOs placeholder: `VaultItemDao`, `CategoryDao`.
- `ItemRepository` con inyección Hilt.
- `./gradlew assembleDebug` compila correctamente.

**Notas de compatibilidad AGP 9**:
- El plugin `kotlin-android` NO se aplica — AGP 9 tiene Kotlin integrado.
- `kotlinOptions` NO se usa — el JVM target se configura solo con `compileOptions`.
- Hilt 2.59.2+ es obligatorio (versiones anteriores no soportan AGP 9).
- Room 2.8.4+ es obligatorio (versiones anteriores fallan con KSP2: `unexpected jvm signature V`).
- `android.disallowKotlinSourceSets=false` en `gradle.properties` es necesario para KSP2 con AGP 9.

---

## Fase 1 — Modelo de datos y almacenamiento

**Objetivo**: capa de datos funcional y testeada.

**Tareas**:
- Completar entidades Room: añadir campos faltantes a `VaultItem` (`imagePath`, `tags`), a `Category` (`color`, `sortOrder`).
- Completar `VaultItemDao` y `CategoryDao` con queries de búsqueda, filtro por categoría y upsert.
- Implementar `FileStorage` para guardar/leer/eliminar imágenes en almacenamiento interno.
- Completar `ItemRepository` para coordinar DAO + FileStorage.
- Escribir tests instrumentados para el DAO (Room soporta testing con base de datos en memoria).

**Criterio de completado**: tests de DAO pasan. Se puede insertar, leer, buscar y eliminar items programáticamente.

---

## Fase 2 — Captura rápida + Quick Settings Tile

**Objetivo**: el flujo principal de la app funcionando de punta a punta. Bajar panel → tile → captura → guardar → cerrar.

> Esta fase combina captura + integración con el sistema porque el Tile ES el punto de entrada principal, no un añadido posterior.

### 2a — QuickCaptureActivity (actividad independiente)

- Crear `QuickCaptureActivity` como actividad separada con tema `Theme.Dialog` o transparente.
- Configurar en Manifest con `taskAffinity` y `launchMode` para que se abra como tarea nueva y se cierre limpiamente.
- Dentro, montar `QuickCaptureScreen` en Compose: tres botones grandes ("Captura de pantalla", "Nota rápida", "Desde portapapeles").
- **Nota rápida**: campo de texto minimal + botón guardar. Al guardar, `CaptureViewModel` persiste via repositorio y la actividad se cierra (`finish()`).
- **Desde portapapeles**: lee el clipboard, muestra preview del contenido, botón guardar. Misma lógica.
- Sin selector de categoría en este paso (se asigna "Inbox" por defecto, categorizar es tarea de organización posterior).
- Al guardar o cancelar (back), la actividad se cierra y el usuario vuelve a donde estaba.

### 2b — Quick Settings Tile

- Crear `QuickSettingsTileService` que extienda `TileService`.
- Registrar en `AndroidManifest.xml` con intent filter.
- `onClick()` lanza `QuickCaptureActivity` como nueva tarea.
- Icono provisional (vector drawable simple).

### 2c — Captura de pantalla (MediaProjection)

- Implementar flujo de MediaProjection: la primera vez que el usuario pulsa "Captura de pantalla", se solicita permiso.
- Una vez concedido, tomar screenshot, mostrar preview en `QuickCaptureScreen`, botón guardar.
- Guardar la imagen via `FileStorage` + crear `VaultItem` de tipo `IMAGE`.
- En Android 14+, MediaProjection requiere foreground service con tipo `mediaProjection`. Configurar en Manifest.

**Criterio de completado**: se puede añadir el tile al panel, pulsarlo, tomar una nota rápida o screenshot, guardar y volver a la app anterior. Todo el flujo en ≤3 toques.

---

## Fase 3 — Pantalla principal (Home)

**Objetivo**: ver y gestionar todo el contenido guardado.

**Tareas**:
- Crear `HomeViewModel` con `StateFlow` de items, estado de filtros y búsqueda.
- Crear `HomeScreen` en Compose:
  - Lista vertical de items (LazyColumn) con preview del contenido (texto truncado o thumbnail de imagen).
  - Barra de búsqueda.
  - Chips de filtro por categoría.
  - FAB "+" que navega a `QuickCaptureScreen`.
  - Menú con acceso a Settings.
- Implementar swipe-to-delete con confirmación.
- Implementar ordenación (más recientes primero, configurable).

**Criterio de completado**: la lista muestra items guardados en fase 2, se puede buscar por texto, filtrar por categoría y eliminar items.

---

## Fase 4 — Detalle y edición

**Objetivo**: poder ver y modificar items existentes.

**Tareas**:
- Crear `DetailViewModel` con carga por ID, edición y borrado.
- Crear `DetailScreen` en Compose:
  - Vista completa del item (texto formateado, imagen a tamaño completo).
  - Modo edición: modificar título, contenido, categoría, tags.
  - Botón eliminar con confirmación.
  - Mostrar metadatos (fecha creación, última modificación, tipo).
- Poder cambiar la imagen asociada o eliminarla.

**Criterio de completado**: se puede navegar desde Home a un item, editarlo, guardar cambios y volver. Los cambios persisten.

---

## Fase 5 — Share Target

**Objetivo**: recibir contenido compartido desde otras apps.

**Tareas**:
- Registrar intent filters en el Manifest para `ACTION_SEND`:
  - `text/plain`: capturar texto/URLs compartidos.
  - `image/*`: capturar imágenes compartidas.
- Reutilizar `QuickCaptureActivity` como receptor: parsear el intent y pre-cargar el contenido en `QuickCaptureScreen`.
- Detectar automáticamente si el texto compartido es una URL y marcar el tipo como `LINK`.
- Manejar `ACTION_SEND_MULTIPLE` para múltiples imágenes.

**Criterio de completado**: al compartir contenido desde otra app (Chrome, WhatsApp, galería...), QNotes aparece como opción y pre-carga el contenido listo para guardar con un solo toque.

---

## Fase 6 — Internacionalización

**Objetivo**: app disponible en múltiples idiomas con selección manual.

**Tareas**:
- Crear `strings.xml` para en, es, ca (extraer todos los strings hardcodeados).
- Implementar `LocaleHelper` con lógica de resolución y cadenas de fallback.
- En `SettingsScreen`:
  - Selector de idioma con nombres nativos ("English", "Español", "Català").
  - Opción "Automático (idioma del dispositivo)".
- Usar `AppCompatDelegate.setApplicationLocales()` para aplicar el cambio.
- Configurar `android:localeConfig` en el Manifest (requerido desde Android 13 para el selector de idioma del sistema).
- Verificar que el fallback funciona: dispositivo en catalán → app en catalán; dispositivo en gallego → app en español.

**Criterio de completado**: se puede cambiar el idioma de la app desde Settings sin reiniciar. Los fallbacks funcionan correctamente para idiomas no soportados directamente.

---

## Fase 7 — Pulido y release

**Objetivo**: app lista para uso diario y publicación en GitHub.

**Tareas**:
- Icono de la app y del Quick Settings Tile (vector drawable).
- Splash screen con la API de Android 12+.
- Animaciones de transición entre pantallas.
- Manejo de errores y estados vacíos en todas las pantallas.
- Tests unitarios para ViewModels y Repository.
- Tests de UI básicos con Compose testing.
- Revisión de accesibilidad (content descriptions, contraste).
- Configurar ProGuard/R8 para release.
- Documentar en README cómo compilar y contribuir.
- Generar APK firmado para distribución directa (sin Play Store inicialmente).

**Criterio de completado**: APK release instalable, tests pasan, documentación actualizada.

---

## Fases futuras (fuera de v1)

- **Overlay flotante**: botón flotante accesible desde cualquier app (requiere `SYSTEM_ALERT_WINDOW`).
- **Captura de pantalla integrada**: usar MediaProjection API para capturar screenshots directamente desde el tile.
- **Exportación/backup**: exportar datos a JSON/ZIP para backup manual.
- **Widget de escritorio**: widget de Android para captura rápida desde la pantalla de inicio.
- **Markdown en notas**: soporte básico de formato markdown en el campo de texto.
- **Más idiomas**: añadir traducciones según demanda.
