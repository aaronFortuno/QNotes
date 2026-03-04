# Arquitectura — QNotes

## Visión general

QNotes sigue una arquitectura MVVM (Model-View-ViewModel) con una capa de repositorio que abstrae el acceso a datos. La inyección de dependencias se gestiona con Hilt. La UI se construye íntegramente con Jetpack Compose y Material 3.

**Paquete base**: `com.aaronfortuno.studio.qnotes`

### Stack actual

| Componente | Versión |
|---|---|
| AGP | 9.1.0 |
| Kotlin | 2.2.10 (built-in AGP 9) |
| Compose BOM | 2024.12.01 |
| Hilt | 2.59.2 |
| Room | 2.8.4 |
| KSP 2 | 2.2.10-2.0.2 |
| Navigation Compose | 2.8.5 |
| Coroutines | 1.9.0 |
| minSdk | 26 |
| targetSdk | 36 |
| Java | 17 |

### Notas AGP 9

AGP 9.1.0 incluye Kotlin de forma nativa. Esto implica:
- No se aplica el plugin `kotlin-android` — ya está integrado en AGP.
- No existe `kotlinOptions` — el JVM target se configura solo con `compileOptions`.
- `android.disallowKotlinSourceSets=false` es necesario en `gradle.properties` para compatibilidad con KSP2.
- Hilt 2.59.2+ es obligatorio (versiones anteriores no encuentran `BaseExtension`).
- Room 2.8.4+ es obligatorio (versiones anteriores fallan con KSP2).

### Principio de diseño fundamental

**El uso principal de QNotes ocurre fuera de la app.** El usuario baja el panel de ajustes rápidos, pulsa el tile, captura el contenido y vuelve a lo que estaba haciendo. Todo el flujo debe completarse en 1-2 toques y menos de 3 segundos. La app interna (lista de items, edición, categorías) es una herramienta secundaria de organización para usar cuando convenga, no el punto de entrada habitual.

Esto tiene implicaciones directas en la arquitectura:
- `QuickCaptureActivity` es una actividad independiente, no parte del grafo de navegación de la app. Se lanza como nueva tarea, se cierra al guardar.
- La pantalla de captura rápida debe ser extremadamente ligera: sin animaciones de carga, sin navegación interna, sin estados intermedios.
- El guardado debe poder ser "fire and forget": un toque en guardar cierra la actividad inmediatamente y persiste en background.
- La captura de pantalla (screenshot) debe ser un flujo de un solo paso: tile → screenshot automático → confirmación mínima → guardado.

```
┌─────────────────────────────────────────────────────────┐
│              FLUJO PRINCIPAL (fuera de la app)           │
│                                                         │
│  Quick Settings Tile ──→ QuickCaptureActivity           │
│  Share Intent ─────────→ (actividad independiente,      │
│                           tema dialog/transparente,     │
│                           se cierra al guardar)         │
│                                  │                      │
│                                  ▼                      │
│                          CaptureViewModel               │
│                                  │                      │
│              FLUJO SECUNDARIO (dentro de la app)        │
│                                                         │
│  App Launcher ──→ HomeScreen ──→ DetailScreen           │
│                       │              │                  │
│                       ▼              ▼                  │
│                  HomeViewModel   DetailViewModel         │
│                                                         │
│              CAPA COMPARTIDA                            │
│                                                         │
│                        ItemRepository                   │
│                          │          │                   │
│                          ▼          ▼                   │
│                     Room DB    File Storage              │
│                   (metadata)   (imágenes)               │
└─────────────────────────────────────────────────────────┘
```

## Componentes principales

### 1. Puntos de entrada al sistema

#### Quick Settings Tile (`QuickSettingsTileService`) — Punto de entrada principal

Servicio que registra un tile en el panel de ajustes rápidos de Android. Es el mecanismo principal de uso de la app. Al pulsarlo, lanza `QuickCaptureActivity` como actividad independiente (nueva tarea, tema diálogo translúcido), permitiendo capturar contenido sin salir del contexto actual.

El flujo implementado desde el tile:
1. El usuario ve algo interesante en pantalla.
2. Baja el panel de ajustes rápidos y pulsa el tile "Quick Capture".
3. Se abre un diálogo flotante (`QuickCaptureActivity` con `Theme.QNotes.QuickCapture`) con 3 opciones: Screenshot, Quick Note, From Clipboard.
4. **Screenshot**: lanza `MediaProjection` → `ScreenshotService` captura la pantalla → guarda como `VaultItem(type=IMAGE)` → notificación "Screenshot saved".
5. **Quick Note**: `OutlinedTextField` multilínea → Save → guarda como `VaultItem(type=NOTE)` → cierra.
6. **From Clipboard**: lee el clipboard al pulsar el botón (no en `onCreate`, por restricciones de Android 10+) → preview del texto → Save → guarda como `VaultItem(type=CLIPBOARD)` → cierra.

Notas técnicas:
- Disponible desde API 24 (Android 7), aunque la app requiere API 26 como mínimo.
- No requiere permisos especiales para el tile; el usuario lo añade manualmente arrastrándolo al panel.
- La captura de pantalla usa MediaProjection API, que requiere un permiso one-time del usuario.
- `ScreenshotService` es un foreground service con tipo `mediaProjection` (requisito API 34+).

#### Share Target

La app se registra como destino para intents `ACTION_SEND` y `ACTION_SEND_MULTIPLE`, aceptando:
- `text/plain` (texto, URLs)
- `image/*` (imágenes)

Cuando el usuario comparte contenido desde cualquier app, QNotes aparece en el diálogo de compartir y lanza directamente la pantalla de captura con el contenido pre-cargado.

#### Launcher (entrada normal)

Abre la pantalla principal (`HomeScreen`) con la lista de todos los items guardados.

### 2. Capa de UI (Jetpack Compose)

#### Navegación

Definida en `NavGraph.kt` con una sealed class `Screen`:

```kotlin
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Capture : Screen("capture")
    data object Detail : Screen("detail/{itemId}")
    data object Settings : Screen("settings")
}
```

`NavHost` configurado en `MainActivity` con `Scaffold` y soporte para edge-to-edge.

#### Pantallas

| Pantalla | Prioridad | Función | Acceso |
|---|---|---|---|
| `QuickCaptureScreen` | **Principal** | Diálogo mínimo: screenshot, nota rápida, portapapeles. Debe cargar instantáneamente | Tile, Share |
| `HomeScreen` | Secundaria | Lista de items con búsqueda, filtro por categoría y ordenación | Entrada normal de la app |
| `DetailScreen` | Secundaria | Vista y edición completa de un item existente | Tap en un item desde Home |
| `SettingsScreen` | Secundaria | Idioma, gestión de categorías | Menú desde Home |

#### Principios de diseño de UI

- **Captura rápida primero**: `QuickCaptureScreen` es un diálogo que flota sobre la pantalla actual. No es una pantalla completa de la app. Carga en <200ms, sin splash, sin animación de entrada. Un botón grande para screenshot, uno para nota, uno para portapapeles. Guardar cierra todo inmediatamente.
- La app interna (Home, Detail, Settings) usa navegación Compose estándar y puede permitirse tiempos de carga normales.
- Material 3 con soporte de Dynamic Color (Material You) cuando el dispositivo lo soporte.
- Modo oscuro automático siguiendo la configuración del sistema.

### 3. Capa de ViewModels

Cada pantalla tiene su ViewModel asociado, inyectado por Hilt:

- **`CaptureViewModel`**: `@HiltViewModel` con `ItemRepository` inyectado. Estado: `CaptureUiState(noteText, clipboardContent, isSaving, savedSuccessfully)`. Métodos: `saveQuickNote()`, `saveClipboardContent()`, `saveScreenshot(imageBytes)`. Cuando `savedSuccessfully = true`, la actividad hace `finish()`.
- **`HomeViewModel`**: expone la lista de items como `StateFlow`, gestiona filtros y búsqueda.
- **`DetailViewModel`**: carga un item por ID, gestiona la edición y el borrado.
- **`SettingsViewModel`**: gestiona preferencias de idioma y categorías personalizadas.

### 4. Capa de datos

#### Entidades Room

```kotlin
@Entity(
    tableName = "vault_items",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("categoryId")]
)
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val type: ItemType,
    val imagePath: String? = null,
    val tags: String = "",
    val categoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ItemType { NOTE, IMAGE, LINK, CLIPBOARD }
```

```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int? = null,
    val sortOrder: Int = 0
)
```

Notas de diseño:
- `imagePath` almacena solo el nombre del fichero (no ruta completa). `FileStorage` resuelve la ruta absoluta.
- `tags` es un String con valores separados por coma. Se busca con `LIKE`. Suficiente para v1; migrar a tabla relacional si escala.
- La foreign key con `ON DELETE SET NULL` permite borrar categorías sin perder items: el `categoryId` pasa a `null`.
- El índice en `categoryId` optimiza las queries de filtro por categoría.

#### DAOs

```kotlin
@Dao
interface VaultItemDao {
    @Query("SELECT * FROM vault_items ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getById(id: Long): VaultItem?

    @Query("SELECT * FROM vault_items WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getByCategory(categoryId: Long): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE title LIKE '%' || :query || '%' " +
           "OR content LIKE '%' || :query || '%' " +
           "OR tags LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VaultItem): Long

    @Insert
    suspend fun insert(item: VaultItem): Long

    @Update
    suspend fun update(item: VaultItem)

    @Delete
    suspend fun delete(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

```kotlin
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}
```

#### Base de datos

```kotlin
@Database(
    entities = [VaultItem::class, Category::class],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun categoryDao(): CategoryDao
}
```

#### Repositorio

`ItemRepository` actúa como fuente única de verdad. Coordina operaciones entre Room (metadatos) y `FileStorage` (imágenes). Expone `Flow` para observación reactiva desde los ViewModels.

```kotlin
@Singleton
class ItemRepository @Inject constructor(
    private val vaultItemDao: VaultItemDao,
    private val fileStorage: FileStorage
) {
    fun getAll(): Flow<List<VaultItem>> = vaultItemDao.getAll()
    suspend fun getById(id: Long): VaultItem? = vaultItemDao.getById(id)
    fun getByCategory(categoryId: Long): Flow<List<VaultItem>> = vaultItemDao.getByCategory(categoryId)
    fun search(query: String): Flow<List<VaultItem>> = vaultItemDao.search(query)

    suspend fun save(item: VaultItem, imageBytes: ByteArray? = null): Long {
        val finalItem = if (imageBytes != null) {
            val fileName = "img_${System.currentTimeMillis()}.png"
            fileStorage.saveImage(fileName, imageBytes)
            item.copy(imagePath = fileName)
        } else item
        return vaultItemDao.upsert(finalItem)
    }

    suspend fun delete(item: VaultItem) {
        item.imagePath?.let { fileStorage.deleteImage(it) }
        vaultItemDao.delete(item)
    }

    suspend fun deleteById(id: Long) {
        val item = vaultItemDao.getById(id)
        if (item != null) delete(item)
    }
}
```

Notas de diseño:
- `save()` usa `upsert` internamente: si el item tiene `id = 0` se inserta como nuevo, si tiene un ID existente se reemplaza.
- Al guardar con `imageBytes`, el repositorio genera un nombre de fichero único con timestamp y delega a `FileStorage`.
- `delete()` y `deleteById()` eliminan tanto el registro en Room como el fichero de imagen asociado (si existe).

#### Almacenamiento de imágenes (`FileStorage`)

```kotlin
@Singleton
class FileStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun saveImage(fileName: String, bytes: ByteArray): String
    fun readImage(fileName: String): ByteArray?
    fun deleteImage(fileName: String): Boolean
    fun getImageFile(fileName: String): File
    fun imageExists(fileName: String): Boolean
}
```

Las imágenes se guardan en `context.filesDir/images/`. Esto evita necesitar permisos de almacenamiento externo y garantiza que los archivos se eliminan si se desinstala la app.

### 5. Inyección de dependencias (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VaultDatabase {
        return Room.databaseBuilder(
            context,
            VaultDatabase::class.java,
            "vault_database"
        ).build()
    }

    @Provides
    fun provideVaultItemDao(database: VaultDatabase): VaultItemDao {
        return database.vaultItemDao()
    }

    @Provides
    fun provideCategoryDao(database: VaultDatabase): CategoryDao {
        return database.categoryDao()
    }
}
```

### 6. Internacionalización (i18n)

#### Problema a resolver

En dispositivos configurados en idiomas minoritarios (como el catalán), muchas apps caen directamente al inglés si no tienen traducción específica, sin ofrecer la posibilidad de elegir un idioma intermedio (como el español) sin cambiar la configuración global del dispositivo.

#### Solución

QNotes implementa selección de idioma a nivel de app usando la API `AppCompatDelegate.setApplicationLocales()` (disponible desde AndroidX AppCompat 1.6+), que permite:

1. **Detección automática**: al primer inicio, la app detecta el idioma del dispositivo y selecciona el idioma más cercano disponible. Si el dispositivo está en catalán y la app tiene catalán, lo usa; si no lo tiene, prioriza español sobre inglés como fallback.
2. **Selección manual**: el usuario puede elegir el idioma de la app independientemente del idioma del sistema, desde `SettingsScreen`.
3. **Persistencia**: la preferencia se almacena automáticamente por AndroidX y sobrevive reinicios.

#### Cadena de fallback personalizada

```kotlin
object LocaleHelper {
    val availableLocales = listOf("en", "es", "ca")

    private val fallbackChains = mapOf(
        "ca" to listOf("es", "en"),
        "gl" to listOf("es", "en"),
        "eu" to listOf("es", "en"),
        "pt" to listOf("es", "en"),
    )

    fun resolveLocale(deviceLocale: String): String {
        val language = deviceLocale.substringBefore("-")
        if (language in availableLocales) return language
        val chain = fallbackChains[language] ?: listOf("en")
        return chain.firstOrNull { it in availableLocales } ?: "en"
    }
}
```

#### Ficheros de recursos

```
res/values/strings.xml          → inglés (fallback obligatorio de Android)
res/values-es/strings.xml       → español
res/values-ca/strings.xml       → catalán
```

Todos los strings visibles al usuario deben estar en `strings.xml`. No se permiten strings hardcodeados en el código.

### 7. Servicios del sistema

#### QuickSettingsTileService (`service/QuickSettingsTileService.kt`)

Tile para el panel de ajustes rápidos. No usa Hilt (no soportado para `TileService`). Solo lanza `QuickCaptureActivity` con `FLAG_ACTIVITY_NEW_TASK`.

- API 34+: usa `startActivityAndCollapse(PendingIntent)` (overload con `Intent` deprecado).
- API <34: usa `startActivityAndCollapse(Intent)`.
- `onStartListening()`: establece tile state a `STATE_INACTIVE`.

#### ScreenshotService (`service/ScreenshotService.kt`)

Foreground service para captura de pantalla via MediaProjection.

- `@AndroidEntryPoint` (extiende `Service`, no `LifecycleService` — Hilt no soporta `LifecycleService`).
- Usa `CoroutineScope` manual (`SupervisorJob() + Dispatchers.Main`) en lugar de `lifecycleScope`.
- Recibe `resultCode` + `data` del `MediaProjectionManager` via intent extras.
- `startForeground()` con notificación antes de `getMediaProjection()` (requisito API 34+).
- Espera 350ms para que `QuickCaptureActivity` desaparezca de pantalla.
- Captura via `ImageReader` + `VirtualDisplay`, convierte a PNG, guarda via `ItemRepository`.
- Usa `WindowManager.getCurrentWindowMetrics().bounds` (API 30+) en lugar de `Display.getRealMetrics()` (deprecado).

### 8. Permisos

| Permiso | Uso | Obligatorio |
|---|---|---|
| Ninguno para almacenamiento | Las imágenes se guardan en almacenamiento interno de la app | — |
| `FOREGROUND_SERVICE` | Requerido para `ScreenshotService` (captura de pantalla) | Sí |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Tipo de foreground service para MediaProjection (API 34+) | Sí |
| `SYSTEM_ALERT_WINDOW` | Solo si se implementa overlay flotante (fase futura) | No |

El Quick Settings Tile y el Share Target no requieren permisos adicionales.

## Decisiones técnicas

### ¿Por qué no overlay flotante en v1?

El botón flotante requiere `SYSTEM_ALERT_WINDOW`, un permiso que el usuario debe conceder manualmente y que algunos fabricantes restringen. Además, añade complejidad de ciclo de vida significativa. El Quick Settings Tile + Share Target cubren los casos de uso principales sin fricción de permisos.

### ¿Por qué Room y no SQLite directo?

Room proporciona verificación en tiempo de compilación de queries SQL, integración nativa con Flow/coroutines y migraciones de esquema gestionadas. El overhead es mínimo y la ganancia en mantenibilidad es significativa.

### ¿Por qué Hilt y no Koin?

Hilt genera código en compilación (menos errores en runtime), es el estándar recomendado por Google para proyectos Android con Compose, y la integración con ViewModels es directa con `@HiltViewModel`.

### ¿Por qué KSP2 y no KAPT?

KAPT está deprecado. KSP2 es el procesador de anotaciones recomendado y es obligatorio con AGP 9 + Kotlin built-in. La versión 2.2.10-2.0.2 es compatible con Kotlin 2.2.10.

### ¿Por qué tags como String separado por comas?

Para v1, evita la complejidad de una tabla de relación muchos-a-muchos. La búsqueda con `LIKE` es suficiente para el volumen de datos esperado. Si escala, migrar a una tabla de tags es una migración Room estándar.
