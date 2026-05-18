# Agent Guidelines for DrawTaxi

## Project Overview
- **Type**: Android native application (Kotlin)
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (v6)
- **Min SDK**: 24, Target SDK: 34, Compile SDK: 34

---

## Build Commands

### Gradle Wrapper
Always use `./gradlew` (Unix/Mac) or `gradlew.bat` (Windows) from the project root.

### Build Debug APK
```bash
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
./gradlew assembleRelease
# APK output: app/build/outputs/apk/release/app-release.apk
```

### Clean Build
```bash
./gradlew clean
```

### Run Tests
```bash
# All unit tests
./gradlew test

# Single test class
./gradlew test --tests "com.drawtaxi.app.logic.SmsParserTest"

# Single test method
./gradlew test --tests "com.drawtaxi.app.logic.SmsParserTest.testParseStandardSms"
```

### Instrumentation Tests (on device/emulator)
```bash
./gradlew connectedAndroidTest
```

### Lint Analysis
```bash
./gradlew lint
# Results: app/build/reports/lint-results.html
```

### Build with Dependencies Refresh
```bash
./gradlew --refresh-dependencies assembleDebug
```

---

## Code Style Guidelines

### Kotlin Version & Configuration
- **Kotlin Version**: 1.9.23
- **JVM Target**: 1.8
- **Compose Compiler Version**: 1.5.11

### Package Structure
```
com.drawtaxi.app/
├── data/           # Data layer (Repository, Models, local storage)
│   └── local/      # Room database, DAOs, SettingsManager
├── logic/          # Business logic (SmsParser, KolectoManager, etc.)
├── ui/             # Presentation layer
│   ├── components/ # Reusable Compose components
│   ├── screens/    # Screen composables
│   └── theme/      # Colors, Typography, Theme
└── car/            # Android Auto specific code
```

### Import Organization
1. Android/Kotlin standard library
2. AndroidX libraries (core, lifecycle, compose, etc.)
3. Third-party libraries (Room, Compose Material, etc.)
4. Internal app imports (grouped by package)

**Example from MainActivity.kt**:
```kotlin
import android.Manifest
import android.content.Intent
import androidx.activity.compose.*
import androidx.compose.foundation.*       // Wildcard for layout/foundation
import androidx.compose.material3.*         // Material3 components
import androidx.compose.runtime.*           // State, remember, LaunchedEffect
import androidx.compose.ui.*
import androidx.core.content.*
import androidx.lifecycle.viewmodel.compose.*
import com.drawtaxi.app.ui.*
import com.drawtaxi.app.data.*
```

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `TaxiViewModel`, `RideRequest` |
| Functions | camelCase | `parseSms()`, `validateRide()` |
| Properties | camelCase | `brandColor`, `validatedRides` |
| Constants | PascalCase | `companion object` members use camelCase |
| Composables | PascalCase | `TaxiCard()`, `RideDetailScreen()` |
| Enum values | PascalCase | `SmsField.DE`, `SmsField.VERS` |
| File names | Match class/function name | `ParseSms.kt`, `TaxiCard.kt` |

### Composable Function Guidelines

1. **Modifier Parameter**: Always include `modifier: Modifier = Modifier` as first optional param
2. **Preview Annotations**: Use `@Preview(showBackground = true)` for previews
3. **State Management**: Use `remember` for local state, `collectAsState()` for flows
4. **Experimental APIs**: Add `@OptIn(ExperimentalMaterial3Api::class)` when needed

**Template**:
```kotlin
@Composable
fun ComponentName(
    requiredParam: String,
    modifier: Modifier = Modifier,
    optionalParam: String? = null
) {
    // Implementation
}

@Preview(showBackground = true)
@Composable
fun ComponentNamePreview() {
    ComponentName(requiredParam = "value")
}
```

### Data Classes (Models)

- Use `data class` for immutable data models
- Default values for optional fields
- Use `companion object` for factory methods (e.g., `createStableId()`)
- Include profitability fields: `fuelCost`, `operatingCost`, `durationMinutes`, `profitabilityPercent`

**Example**:
```kotlin
data class RideRequest(
    val id: String,
    val sender: String,
    val body: String,
    val departure: String = "",
    val arrival: String = "",
    val time: String = "",
    val distanceKm: Double = 28.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = true,
    val date: String = "",
    val price: Double = 0.0,
    val fuelCost: Double = 0.0,
    val operatingCost: Double = 0.0,
    val durationMinutes: Int = 0,
    val profitabilityPercent: Double = 0.0
) {
    companion object {
        fun createStableId(sender: String, body: String, timestamp: Long): String {
            val raw = "$sender|$body|$timestamp"
            return java.util.UUID.nameUUIDFromBytes(raw.toByteArray()).toString()
        }

        fun calculateProfitability(price: Double, fuelCost: Double, operatingCost: Double): Double {
            val totalCost = fuelCost + operatingCost
            if (totalCost == 0.0 || price == 0.0) return 0.0
            return ((price - totalCost) / price) * 100.0
        }
    }
}
```

### ViewModel Pattern

- Extend `ViewModel` for state holders
- Expose state as `StateFlow`
- Use `ViewModelProvider.Factory` for dependency injection
- Wrap suspend operations in `viewModelScope.launch`

**Example**:
```kotlin
class TaxiViewModel(private val repository: TaxiRepository) : ViewModel() {

    val validatedRides: StateFlow<List<RideRequest>> = repository.validatedRides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun validateRide(id: String) {
        viewModelScope.launch {
            repository.validateRide(id)
        }
    }
}

class TaxiViewModelFactory(private val repository: TaxiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaxiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaxiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

### Room Database

- Use `@Database` annotation with entities and version
- Create DAOs with suspend functions for DB operations
- Singleton pattern with `getDatabase()` factory method
- Use `@Volatile` for INSTANCE field
- Use `fallbackToDestructiveMigration()` for schema changes
- Current database version: **6**

### Coroutines & Flow

- Use `viewModelScope.launch` for ViewModel coroutines
- Use `Dispatchers.IO` for I/O operations (network, DB)
- Prefer `StateFlow` over `LiveData` for Compose

### Error Handling

- Return `null` for expected failure cases (e.g., `parseSms()` returns null if parsing fails)
- Throw `IllegalArgumentException` in factory `create()` methods for unknown types
- Use `runCatching` or `try-catch` for operations that may throw

### Null Safety

- Prefer nullable types (`String?`) over platform types
- Use `?.` and `?.let` for safe navigation
- Use Elvis operator `?:` for default values
- Avoid `!!` operator except in rare cases where null is provably impossible

### Testing

- Unit tests go in `app/src/test/java/...`
- Use JUnit 4 with `@Test` annotation
- Group related tests in a test class
- Use descriptive test names: `testParse<Scenario>()` or `should<Behavior>()`

**Example**:
```kotlin
class SmsParserTest {
    @Test
    fun testParseStandardSms() {
        val ride = SmsParser.parse("+33612345678", "Taxi depuis Paris vers Lyon à 14h30")
        assertNotNull(ride)
        assertEquals("Paris", ride?.departure)
    }
}
```

---

## UI Design Guidelines

### Color Palette (Tailwind-inspired)
- **Primary**: Indigo500 (`#6366F1`)
- **Success**: Emerald500 (`#10B981`)
- **Error**: Rose500 (`#F43F5E`)
- **Warning**: Amber500 (`#F59E0B`)
- **Slate**: Slate50 to Slate950 for neutrals

### Card Style
- Rounded corners: `RoundedCornerShape(20.dp)` for cards, `16.dp` for smaller cards
- Shadow elevation: 4dp with colored shadow
- Icon containers: `RoundedCornerShape(10.dp)` with `brandColor.copy(alpha = 0.1f)` background
- Gradient headers for hero sections

### Typography
- Headlines: `headlineMedium` / `headlineSmall` with `FontWeight.Bold`
- Titles: `titleMedium` with `FontWeight.Bold`
- Body: `bodyMedium` for regular text
- Labels: `labelSmall` / `labelMedium` for metadata

### Spacing
- Horizontal padding: `16.dp` for content, `20.dp` for section headers
- Vertical spacing: `6.dp` between cards, `12.dp` between sections
- Bottom nav padding: accounts for `100.dp` spacer at bottom of scrollable screens

---

## Key Features

### SMS Parsing (ParseSms.kt)
- Context-aware analysis (greeting, politeness, urgency detection)
- Confidence scoring per field and overall
- Deduplication cache (30s window)
- Intent detection: confirmation, cancellation, modification
- Missing field extraction for auto-replies

### SMS Reception
- **SmsReceiver**: BroadcastReceiver with deduplication
- **SmsWatcher**: ContentObserver with 2s debounce
- **SmsForegroundService**: Foreground service with polling (10s) + ContentObserver
- All three work together for reliable SMS capture

### Kolecto Integration
- Invoice screen with completed rides list and filters
- Manual invoice creation data for Kolecto web platform
- Ride details with HT/TVA/TTC breakdown
- Direct link to Kolecto web app
- Toggle in settings + invoices tab in bottom navigation

### Profitability
- Auto-calculated on ride completion based on:
  - `fuelCost = distanceKm × fuelCostPerKm`
  - `operatingCost = (durationMinutes / 60) × operatingCostPerHour`
  - `profitabilityPercent = ((price - totalCost) / price) × 100`
- Displayed in Stats, Accounting, RideDetail, and RideCompletion screens

### GPS Navigation (GpsNavigationScreen.kt)
- osmdroid map with OpenStreetMap tiles
- FusedLocationProviderClient for real-time position
- ETA, distance, speed display
- Start/stop navigation controls

---

## Common Operations

### Add a New Screen
1. Create file in `ui/screens/`
2. Add `@Composable` function with parameters for navigation state and callbacks
3. Register in Navigation (in MainActivity or NavHost)
4. Add bottom nav item if needed

### Add a New Data Model
1. Add data class in `data/Models.kt` or create new file in `data/`
2. If persistence needed, create Room Entity in `data/local/RideEntity.kt`
3. Add DAO methods in `data/local/RideDao.kt`
4. Update repository with CRUD operations
5. Increment database version in `AppDatabase.kt`

### Add a New Setting
1. Add field to `AppSettings` data class with default value
2. Add key in `SettingsManager.Keys` object
3. Update `settingsFlow` mapping in `SettingsManager`
4. Update `updateSettings()` to persist the value
5. Update settings UI screens to display/edit

### Add a New Logic Module
1. Create file in `logic/`
2. Use `object` for singletons (e.g., `KolectoManager`)
3. Use `Context` parameter for Android APIs
4. Log with `android.util.Log` using a `TAG` constant

---

## Important Notes

- **NO PDF generation**: Invoices were replaced by text receipts + manual Kolecto entry
- **Database version is 6**: Any schema change requires incrementing this
- **SMS permissions are critical**: App won't work without RECEIVE_SMS, READ_SMS, SEND_SMS
- **Foreground service**: SmsForegroundService auto-restarts on task removal
- **Profitability defaults**: fuelCostPerKm = 0.12, operatingCostPerHour = 15.0
- **Kolecto**: No API integration - use InvoiceScreen to prepare data for manual entry on Kolecto web
