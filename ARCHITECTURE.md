# Arquitectura — QNotes

## Visión general

QNotes sigue una arquitectura MVVM (Model-View-ViewModel) con una capa de repositorio que abstrae el acceso a datos. La inyección de dependencias se gestiona con Hilt. La UI se construye íntegramente con Jetpack Compose y Material 3.

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

Servicio que registra un tile en el panel de ajustes rápidos de Android. Es el mecanismo principal de uso de la app. Al pulsarlo, lanza `QuickCaptureActivity` como actividad independiente (nueva tarea, tema dialog/transparente), permitiendo capturar contenido sin salir del contexto actual.

El flujo ideal desde el tile:
1. El usuario ve algo interesante en pantalla (un vídeo, un chat, una web).
2. Baja el panel de ajustes rápidos y pulsa el tile de QNotes.
3. Se abre un diálogo mínimo sobre la pantalla actual con opciones: "Captura de pantalla", "Nota rápida", "Desde portapapeles".
4. Un toque en "Captura de pantalla" toma el screenshot, muestra una preview mínima y un botón de guardar.
5. Al guardar, el diálogo se cierra y el usuario vuelve exactamente a donde estaba.

Notas técnicas:
- Disponible desde API 24 (Android 7), aunque la app requiere API 26 como mínimo.
- No requiere permisos especiales para el tile; el usuario lo añade manualmente arrastrándolo al panel.
- La captura de pantalla usa MediaProjection API, que requiere un permiso one-time del usuario (se solicita la primera vez y se puede mantener activo durante la sesión).

#### Share Target

La app se registra como destino para intents `ACTION_SEND` y `ACTION_SEND_MULTIPLE`, aceptando:
- `text/plain` (texto, URLs)
- `image/*` (imágenes)

Cuando el usuario comparte contenido desde cualquier app, QNotes aparece en el diálogo de compartir y lanza directamente la pantalla de captura con el contenido pre-cargado.

#### Launcher (entrada normal)

Abre la pantalla principal (`HomeScreen`) con la lista de todos los items guardados.

### 2. Capa de UI (Jetpack Compose)

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

- **`CaptureViewModel`**: gestiona el estado del formulario de captura, valida el contenido, y delega el guardado al repositorio. Procesa intents de compartir recibidos.
- **`HomeViewModel`**: expone la lista de items como `StateFlow`, gestiona filtros y búsqueda.
- **`DetailViewModel`**: carga un item por ID, gestiona la edición y el borrado.
- **`SettingsViewModel`**: gestiona preferencias de idioma y categorías personalizadas.

### 4. Capa de datos

#### Entidades Room

```kotlin
@Entity(tableName = "items")
data class VaultItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: ItemType,          // NOTE, IMAGE, LINK, CLIPBOARD
    val title: String,
    val textContent: String?,
    val imagePath: String?,      // ruta relativa en almacenamiento interno
    val category: String = "general",
    val tags: String = "",       // separados por coma, búsqueda con LIKE
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ItemType { NOTE, IMAGE, LINK, CLIPBOARD }
```

```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String,
    val color: Int?,             // color en formato ARGB
    val sortOrder: Int = 0
)
```

#### DAO

```kotlin
@Dao
interface VaultItemDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM items WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<VaultItem>>

    @Query("SELECT * FROM items WHERE title LIKE '%' || :query || '%' OR textContent LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VaultItem)

    @Delete
    suspend fun delete(item: VaultItem)
}
```

#### Repositorio

`ItemRepository` actúa como fuente única de verdad. Coordina las operaciones entre Room (metadatos) y el sistema de ficheros (imágenes). Expone `Flow` para observación reactiva desde los ViewModels.

```kotlin
class ItemRepository @Inject constructor(
    private val dao: VaultItemDao,
    private val fileStorage: FileStorage
) {
    fun getAllItems(): Flow<List<VaultItem>> = dao.getAllItems()

    suspend fun saveItem(item: VaultItem, imageBytes: ByteArray? = null): VaultItem {
        val finalItem = if (imageBytes != null) {
            val path = fileStorage.saveImage(item.id, imageBytes)
            item.copy(imagePath = path)
        } else item
        dao.upsert(finalItem)
        return finalItem
    }

    suspend fun deleteItem(item: VaultItem) {
        item.imagePath?.let { fileStorage.deleteImage(it) }
        dao.delete(item)
    }
}
```

#### Almacenamiento de imágenes (`FileStorage`)

Las imágenes se guardan en el directorio interno de la app (`context.filesDir/images/`). Esto evita necesitar permisos de almacenamiento externo y garantiza que los archivos se eliminan si se desinstala la app.

### 5. Internacionalización (i18n)

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
    // Mapa de idiomas disponibles
    val availableLocales = listOf(
        "en",  // English (fallback base)
        "es",  // Español
        "ca",  // Català
    )

    // Fallback chains: si un idioma no está disponible, probar estos en orden
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

### 6. Inyección de dependencias (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VaultDatabase =
        Room.databaseBuilder(context, VaultDatabase::class.java, "qnotes.db").build()

    @Provides
    fun provideItemDao(db: VaultDatabase): VaultItemDao = db.itemDao()

    @Provides @Singleton
    fun provideFileStorage(@ApplicationContext context: Context): FileStorage =
        FileStorage(context)
}
```

### 7. Permisos

| Permiso | Uso | Obligatorio |
|---|---|---|
| Ninguno para almacenamiento | Las imágenes se guardan en almacenamiento interno de la app | — |
| `FOREGROUND_SERVICE` | Solo si se implementa overlay flotante (fase futura) | No |
| `SYSTEM_ALERT_WINDOW` | Solo si se implementa overlay flotante (fase futura) | No |

El Quick Settings Tile y el Share Target no requieren permisos adicionales.

## Decisiones técnicas

### ¿Por qué no overlay flotante en v1?

El botón flotante requiere `SYSTEM_ALERT_WINDOW`, un permiso que el usuario debe conceder manualmente y que algunos fabricantes restringen. Además, añade complejidad de ciclo de vida significativa. El Quick Settings Tile + Share Target cubren los casos de uso principales sin fricción de permisos.

### ¿Por qué Room y no SQLite directo?

Room proporciona verificación en tiempo de compilación de queries SQL, integración nativa con Flow/coroutines y migraciones de esquema gestionadas. El overhead es mínimo y la ganancia en mantenibilidad es significativa.

### ¿Por qué Hilt y no Koin?

Hilt genera código en compilación (menos errores en runtime), es el estándar recomendado por Google para proyectos Android con Compose, y la integración con ViewModels es directa con `@HiltViewModel`.

### ¿Por qué tags como String separado por comas?

Para v1, evita la complejidad de una tabla de relación muchos-a-muchos. La búsqueda con `LIKE` es suficiente para el volumen de datos esperado. Si escala, migrar a una tabla de tags es una migración Room estándar.
