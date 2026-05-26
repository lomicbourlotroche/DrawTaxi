# DrawTaxi

Application Android native (Kotlin + Jetpack Compose) de gestion de courses taxi/VTC. Réception automatique des commandes par SMS (avec parsing IA locale via **Nexa SDK** et regex optimisés), suivi de rentabilité, navigation GPS intégrée (**MapLibre + OSRM + NavigationEngine**), comptabilité et support Android Auto.

---

## Fonctionnalités

### Réception & Parsing SMS (3 couches + Regex Optimisé)
- **SmsReceiver** — BroadcastReceiver temps réel avec priorité 999, déduplication 30s
- **SmsWatcher** — ContentObserver avec debounce 2s, utilisé par le foreground service
- **SmsForegroundService** — Service foreground avec polling 10s + ContentObserver, vérifie boîte réception **et** envoyés
- **Parsing regex avancé** : 
  - Détection intelligente des **noms/prénoms** (ex: "Bonjour Jean", "Je m'appelle...")
  - Extraction robuste des **heures** (14h, 14h30, 14:30, dans 30min)
  - Gestion des **dates relatives** (lundi, demain, après-demain, 15 jan)
  - Vocabulaire enrichi pour les lieux ("rdv à", "direction", "prendre rue")
- **Parsing IA** — Llama 3.2 3B (Q4_K_M, ~2 Go) via **Nexa SDK** (on-device CPU/GPU/NPU), timeout 60s

### Gestion des Courses
- **Devis** — Génération et envoi par SMS/WhatsApp/Email avec affichage de la **rentabilité estimée** avant envoi
- **Cycle de vie** — Brouillon → Devis envoyé → Confirmée → En cours → Terminée / Annulée / Absent
- **RideMatcher** — Détection automatique : nouvelle course, doublon, modification, annulation, clarification
- **Création manuelle** — Formulaire + partage depuis d'autres apps

### Tarification & Rentabilité
- **PriceEngine** — Calcul complet : prise en charge + km + surcharges nuit/dimanche/jour férié + attente + **TVA Unique 10%**
- **Rentabilité** — `((prix - coût_déplacement) / prix) × 100` où `coût_déplacement = distance_retour_vide × coutParKmDeplacement` (configurable, défaut 0,15 €/km)
- **Statistiques** — Revenus, bénéfices, rentabilité par jour/semaine/mois

### Navigation & Pilotage
- **MapLibre Native** — Carte OpenStreetMap avec `maplibre-compose 0.13.0`
- **NavigationEngine** — Wrapper autour de `maplibre-navigation-core 5.0.0-pre12` avec `MapLibreNavigation`, OSRM, polyline decoder
- **Mode pilotage** — Vitesse, ETA, distance restante, instructions tour-à-tour en français
- **Geocoding** — **Photon uniquement** (`photon.komoot.io`), pas de fallback
- **LocationTrackingService** — Service foreground de suivi GPS avec historique

### Comptabilité & Factures
- **Écran Factures** — Liste des courses terminées avec filtres, préparation des données pour saisie manuelle sur Kolecto (pas d'API)
- **AccountingScreen** — Analyse revenus/coûts
- **Pas de reçus** — Le chauffeur saisit les données manuellement sur Kolecto

### Messagerie
- **4 canaux** — SMS, WhatsApp, Email, Formulaire web
- **Templates personnalisables** — Devis, refus, arrivée, absence, demande d'infos
- **OVH** — SMTP (envoi) + IMAP (réception des réservations web) avec credentials chiffrés

### Intelligence Artificielle (Locale — Nexa SDK)
- **Llama 3.2 3B Instruct** — Modèle GGUF téléchargé depuis HuggingFace (~2 Go)
- **Nexa SDK** (`ai.nexa:core:0.0.24`) — Inférence on-device (CPU/GPU/NPU), pas de JNI/CMake
- **Téléchargement** — `LlamaModelManager` : DownloadManager + fallback OkHttp, reprise, 5 tentatives
- **Gestion** — `NexaEngine` : chargement paresseux, timeout 60s, flux streamé (`generateStreamFlow`)
- **Utilisation** — Classification SMS (TAXI/NON_TAXI), parsing SMS (JSON structuré), parsing email

### Android Auto
- **3 écrans** — Liste des courses, détail, édition (avec actions inline pour distance/prix)
- **CarAppService** — Session Android Auto avec ListTemplate/PaneTemplate, capacités `navigation` et `message`

### Services Background
| Service | Type | Rôle |
|---------|------|------|
| SmsForegroundService | Foreground | Surveillance SMS avec ContentObserver + polling 10s |
| OvhImapService | Foreground | Vérification IMAP OVH (toutes les 5 min) |
| LocationTrackingService | Foreground | Suivi GPS en temps réel |
| SmsScanWorker | WorkManager | Scan périodique SMS (60 min) |
| StatsReportScheduler | WorkManager | Rapports stats programmés |

### Autres
- **Agenda** — Gestion des absences avec envoi automatique aux clients en attente
- **Sauvegarde/Restauration** — JSON, export/import fichier, auto-backup (journalier/hebdomadaire/mensuel)
- **Carnet clients** — Historique des courses par client
- **Onboarding** — Configuration permissions + téléchargement modèle IA
- **Mode sombre** — Support complet
- **BootReceiver** — Redémarrage automatique des services au boot
- **Exemption Batterie** — Demande automatique d'ignorer les optimisations batterie au démarrage

---

## Architecture

**MVVM + Repository + 3 couches de détection SMS**

```
com.drawtaxi.app/
├── MainActivity.kt              # Single Activity, navigation state-machine
├── TaxiApplication.kt           # Init DB/Repository/ Nexa SDK + callback onNewSms() + Battery Opt.
├── car/                         # Android Auto
│   ├── TaxiCarAppService.kt
│   ├── RideListScreen.kt
│   ├── RideDetailCarScreen.kt
│   └── EditRideCarScreen.kt
├── data/                        # Data layer
│   ├── Models.kt                # RideRequest, AppSettings, Quote, Absence, Client, StatsReport
│   ├── TaxiRepository.kt        # Repository (Flow → StateFlow)
│   ├── BackupManager.kt         # Backup/restore JSON
│   ├── SecureCredentialsManager.kt
│   └── local/
│       ├── AppDatabase.kt       # Room v6 (rides, quotes, absences)
│       ├── RideEntity.kt        # Entities + domain mappers
│       ├── RideDao.kt           # CRUD : RideDao, QuoteDao, AbsenceDao
│       └── SettingsManager.kt   # DataStore (~50 clés)
├── logic/                       # Business logic
│   ├── ai/
│   │   ├── NexaEngine.kt        # Nexa SDK inference wrapper (Flow stream)
│   │   └── LlamaModelManager.kt  # Download GGUF via DownloadManager/OkHttp
│   ├── geocoding/
│   │   └── GeocodingService.kt   # Photon uniquement (komoot)
│   ├── messaging/
│   │   ├── MessageSender.kt
│   │   ├── NotificationHelper.kt
│   │   ├── EmailFormParser.kt
│   │   ├── OvhMailSender.kt
│   │   └── WebFormApiHandler.kt
│   ├── pricing/
│   │   ├── PriceEngine.kt
│   │   ├── QuoteResponseHandler.kt
│   │   └── RideCalculator.kt
│   ├── routing/
│   │   └── NavigationEngine.kt   # MapLibreNavigation + OSRM + polyline decode + instructions FR
│   └── sms/
│       ├── ParseSms.kt           # Regex parser optimisé
│       ├── AiSmsParser.kt        # AI parser (NexaEngine)
│       ├── SmsProcessor.kt       # Pipeline orchestrator
│       ├── SmsWatcher.kt
│       ├── SmsScanner.kt
│       ├── SmsUtils.kt
│       └── RideMatcher.kt
├── receiver/
│   ├── SmsReceiver.kt
│   └── BootReceiver.kt
├── service/
│   ├── foreground/
│   │   ├── SmsForegroundService.kt
│   │   └── OvhImapService.kt
│   ├── tracking/
│   │   └── LocationTrackingService.kt
│   └── worker/
│       ├── SmsScanWorker.kt
│       └── StatsReportScheduler.kt
├── ui/
│   ├── TaxiViewModel.kt
│   ├── components/
│   │   ├── BottomNavigation.kt   # 4 tabs flottants
│   │   ├── MainTopBar.kt
│   │   ├── RideCard.kt / TaxiCard.kt
│   │   ├── RideMap.kt / RouteToClientMap.kt / NavigationMapView.kt
│   │   ├── RideDetailMapSection.kt / RideDetailInfoCard.kt
│   │   ├── RideCreateForm.kt / RideCreateTimerButton.kt
│   │   ├── TaxiInputField.kt / TaxiLogo.kt
│   │   └── ProfitabilityCard.kt
│   ├── screens/
│   │   ├── dashboard/DashboardScreen.kt
│   │   ├── home/HomeScreen.kt
│   │   ├── invoices/InvoiceScreen.kt + AccountingScreen.kt
│   │   ├── messages/ControlCenterScreen.kt + QuoteScreen.kt
│   │   ├── navigation/GpsNavigationScreen.kt + RideNavigationScreen.kt
│   │   ├── onboarding/OnboardingScreen.kt + AiModelDownloadScreen.kt
│   │   ├── rides/
│   │   │   ├── ActiveRideScreen.kt / RideCreateScreen.kt
│   │   │   ├── RideDetailScreen.kt / RideCompletionScreen.kt
│   │   │   ├── RideHistoryItem.kt / PendingRideItem.kt
│   │   │   └── ReturnHomeScreen.kt
│   │   └── settings/ (~15 écrans)
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Typography.kt
└── util/
    └── PermissionHelper.kt
```

### Navigation (sans Jetpack Navigation)

Navigation par **state machine** dans MainActivity avec variables `mutableStateOf` :

```
activeTab ∈ { home, message, dashboard, settings }
settingsView ∈ { main, proInfo, pricing, backup, messageTemplates, ovhMail, aiDownload, export }
overlays → selectedRide, activeRide, isCreatingRide, editingRide,
           showAgendaScreen, showCompletionScreen, showReturnHomeScreen
```

**4 onglets bottom navigation** : Accueil | Messages | Dashboard | Paramètres

---

## Modèle de Données

### RideRequest
| Champ | Type | Description |
|-------|------|-------------|
| `id` | String | UUID stable (hash sender+body+timestamp) |
| `sender` | String | Numéro téléphone client |
| `body` | String | Contenu SMS original |
| `departure` | String | Lieu de départ |
| `arrival` | String | Destination |
| `time` | String | Heure prise en charge |
| `date` | String | Date course |
| `distanceKm` | Double | Distance estimée |
| `price` | Double | Prix |
| `status` | RideStatus | DRAFT → QUOTED → CONFIRMED → IN_PROGRESS → COMPLETED / CANCELLED / ABSENT |
| `fuelCost` | Double | Coût carburant estimé |
| `operatingCost` | Double | Coûts opérationnels |
| `durationMinutes` | Int | Durée estimée |
| `profitabilityPercent` | Double | Rentabilité % |
| `clientEmail` | String | Email client |
| `messageChannel` | MessageChannel | Canal de communication |
| `quoteId` | String | Devis associé |
| `clientName` | String | Nom client |
| `clientFirstName` | String | Prénom client |
| `clientPhone` | String | Téléphone client |
| `homeAddress` | String | Adresse retour |
| `distanceReelleKm` | Double | Distance réelle (GPS) |
| `waitMinutes` | Int | Temps d'attente |
| `priceBreakdown` | String | Détail prix (JSON) |
| `latitudeDepart` / `longitudeDepart` | Double | Coordonnées départ |
| `latitudeDestination` / `longitudeDestination` | Double | Coordonnées destination |
| `startedAt` / `endedAt` | Long | Timestamps début/fin |
| `isTracking` | Boolean | Suivi GPS actif |
| `lastLatitude` / `lastLongitude` | Double | Dernière position |
| `destinationModifiee` | String | Destination modifiée |
| `hasMissingInfo` | Boolean | Infos manquantes |
| `missingFieldsList` | String | Champs manquants (JSON list) |
| `invoiceNumber` | String | Numéro facture |
| `notes` | String | Notes |
| `absenceMessageSent` | Boolean | Message d'absence envoyé |

> **Note** : Les reçus textes ne sont plus envoyés par l'application. Le chauffeur remplit Kolecto manuellement en fin de course.

### AppSettings (~50 champs)
| Catégorie | Exemples |
|-----------|----------|
| Pro | `companyName`, `name`, `siret`, `tva`, `vehicle`, `address`, `city`, `signature` |
| Tarifs | `pricePerKm`, `basePrice`, `fuelCostPerKm`, `operatingCostPerHour`, `euroPerMinute` |
| Surcharges | `nightSurchargePercent`, `sundaySurchargePercent`, `holidaySurchargePercent` |
| TVA | `tvaTransportRate` (10% unique) |
| SMS | `monitorSms`, `enableNotifications`, `smsScanIntervalMinutes`, `aiEnabled` |
| Templates | `quoteTemplate`, `missingInfoTemplate`, `arrivalMessageTemplate`, `rejectionTemplate`, `invoiceTemplate`, `absenceMessageTemplate` |
| OVH SMTP | `ovhSmtpEnabled`, `ovhSmtpServer`, `ovhSmtpPort`, `ovhSmtpUsername`, `ovhSmtpPassword` |
| OVH IMAP | `ovhImapEnabled`, `ovhImapServer`, `ovhImapPort`, `ovhImapCheckInterval` |
| Backup | `autoBackupEnabled`, `autoBackupInterval` |
| Apparence | `brandColor`, `darkMode`, `showLogo` |
| Localisation | `trackLocation`, `homeAddress` |
| Divers | `isFirstLaunch`, `autoGenerateStatsReport`, `statsReportTime` |

---

## Calcul de Rentabilité

La rentabilité est estimée avant l'envoi du devis et affichée dans l'écran `QuoteScreen`. Elle se base sur le coût du retour à vide estimé.

```kotlin
// Dans RideRequest.kt
fun calculateProfitability(price: Double, coutDeplacement: Double): Double {
    if (coutDeplacement == 0.0 || price == 0.0) return 0.0
    return ((price - coutDeplacement) / price) * 100.0
}

fun calculateCoutDeplacement(distanceDomicileKm: Double, coutParKm: Double): Double {
    return distanceDomicileKm * coutParKm
}
```

- **distanceDomicileKm** : Estimée à 30% de la distance totale de la course par défaut.
- **coutParKm** : Configurable dans les paramètres (défaut 0,15 €/km).

- **≥ 70%** : Vert (excellente)
- **50-69%** : Orange (correcte)
- **< 50%** : Rouge (faible)

---

## Pricing Engine (PriceEngine)

Le moteur de tarification calcule le prix complet avec :

| Composante | Description |
|------------|-------------|
| **Prise en charge** | Tarif de base (`basePrice`) |
| **Kilométrique** | `distance × pricePerKm` |
| **Nuit** | Surcharge % si heure entre nightStartHour et nightEndHour |
| **Dimanche** | Surcharge % si jour = dimanche |
| **Jour férié** | Surcharge % si jour férié français (calcul calendrier incl. Pâques) |
| **Attente** | `waitMinutes × euroPerMinute` |
| **TVA Unique** | 10% sur le total HT (transport + attente + suppléments) |

---

## Parsing SMS

### Regex (ParseSms) - Optimisé
1. **Détection Noms/Prénoms** : "Bonjour Jean", "Je m'appelle..."
2. **Heures Intelligentes** : `14h`, `14h30`, `14:30`, `dans 30min`, `dans 1h`
3. **Dates Avancées** : Jours de la semaine ("lundi" → date exacte), "demain", "après-demain", formats textuels ("15 jan")
4. **Vocabulaire Étendu** : "rdv à", "direction", "prendre rue", "jusqu'à"
5. **Analyse de contexte** : salutations, politesse, urgence, longueur
6. **Scoring de confiance** par champ et global
7. **Cache déduplication** : fenêtre 30s

### IA (AiSmsParser)
- Modèle Llama 3.2 3B quantifié (Q4_K_M) exécuté via **Nexa SDK** (CPU/GPU/NPU)
- Timeout 60s sur l'inférence, flux streamé (`generateStreamFlow`)
- Classification binaire : TAXI / NON_TAXI
- Extraction JSON structurée : départ, arrivée, heure, date, passagers, prix, nom, email
- Fallback automatique vers regex si modèle non disponible

---

## Flux SMS Complet

```
SMS entrant
    │
    ├─ SmsReceiver (BroadcastReceiver, temps réel, priority 999)
    ├─ SmsWatcher (ContentObserver, debounce 2s)
    └─ SmsForegroundService (polling 10s + ContentObserver)
    │
    └─ TaxiApplication.onNewSms()
        │
        └─ SmsProcessor.processSms()
            ├─ AiSmsParser.parseWithAI() (Nexa SDK, timeout 60s) ou fallback regex
            ├─ QuoteResponseHandler.handleResponse() (réponse à devis ?)
            ├─ RideMatcher.matchSmsToRides() → NEW / DUPLICATE / MODIFICATION / DELETION
            └─ NotificationHelper (alerte nouvelle course / mise à jour)
```

---

## Permissions Requises

| Permission | Usage |
|------------|-------|
| `RECEIVE_SMS` | Réception automatique des commandes |
| `READ_SMS` | Lecture historique + scan périodique |
| `SEND_SMS` | Envoi réponses aux clients |
| `POST_NOTIFICATIONS` | Notifications nouvelles courses |
| `ACCESS_FINE_LOCATION` | GPS mode pilotage |
| `ACCESS_COARSE_LOCATION` | Localisation approximative |
| `FOREGROUND_SERVICE` | Surveillance SMS + suivi GPS en background |
| `INTERNET` | OSRM, géocodage, OVH SMTP/IMAP |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Exemption batterie automatique |

---

## Dépendances Clés (4 SDKs)

```kotlin
// Compose BOM 2024.05.00
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// [SDK 1] Room 2.8.4 (KSP)
ksp("androidx.room:room-compiler:2.8.4")

// DataStore Preferences 1.0.0
implementation("androidx.datastore:datastore-preferences:1.0.0")

// [SDK 2] MapLibre Compose 0.13.0 (rendu carte Compose-native)
implementation("org.maplibre.compose:maplibre-compose:0.13.0") {
    exclude(group = "org.maplibre.gl", module = "android-sdk-geojson")
    exclude(group = "org.maplibre.gl", module = "android-sdk-turf")
}

// [SDK 3] MapLibre Navigation Core 5.0.0-pre12 (moteur navigation tour-à-tour)
implementation("org.maplibre.navigation:navigation-core:5.0.0-pre12")

// [SDK 4] Nexa SDK (ai.nexa:core:0.0.24) — Inférence IA on-device
implementation("ai.nexa:core:0.0.24")

// Android Auto 1.4.0
implementation("androidx.car.app:app:1.4.0")

// WorkManager 2.9.0
implementation("androidx.work:work-runtime-ktx:2.9.0")

// OkHttp 4.12.0 (téléchargement modèle, géocodage Photon)
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// JavaMail 1.6.7 (OVH SMTP/IMAP)
implementation("com.sun.mail:android-mail:1.6.7")

// Security Crypto (Credentials OVH)
implementation("androidx.security:security-crypto:1.0.0")
```

---

## Build

```bash
# Debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK
./gradlew assembleRelease

# Tests unitaires
./gradlew test

# Lint
./gradlew lint

# Clean
./gradlew clean
```

### Configuration Technique
| Paramètre | Valeur |
|-----------|--------|
| Kotlin | 2.1.0 |
| JVM Target | 1.8 |
| Compose Compiler | plugin (`org.jetbrains.kotlin.plugin.compose`) |
| Min SDK | 27 |
| Target SDK | 35 |
| Compile SDK | 36 |
| Room | 2.8.4 |
| Database version | 6 |
| KSP | 2.1.0-1.0.29 |

---

## Notes Importantes

- **4 SDKs intégrés** : Room (DB), MapLibre (cartes), MapLibre Navigation (guidage), Nexa (IA)
- **Nexa SDK remplace llama.cpp** — Plus de JNI, CMake, ou bibliothèques natives. Inférence via `LlmWrapper.generateStreamFlow()` (Flow Kotlin)
- **Photon uniquement** pour le géocodage — pas de Nominatim ni Android Geocoder
- **NavigationEngine** — Wrapper unique autour de `MapLibreNavigation` + OSRM `fetchRoute()` + polyline decoder
- **Pas de PDF** — Pas de génération de factures dans l'app
- **Pas d'API Kolecto** — L'écran factures prépare les données pour saisie manuelle sur Kolecto web
- **Modèle IA** — Llama 3.2 3B (~2 Go) téléchargé au premier lancement, stocké dans `filesDir`
- **SMS permissions critiques** — L'app ne fonctionne pas sans `RECEIVE_SMS`, `READ_SMS`, `SEND_SMS`
- **Foreground service** — SmsForegroundService se redémarre automatiquement si tué
- **Rentabilité** — Affichée uniquement dans l'écran de devis avant envoi
- **TVA** — Unique à 10% sur le total HT
- **Base de données** — Version 6, migration destructive (`fallbackToDestructiveMigration()`)
- **Sécurité** — Credentials OVH chiffrés via `EncryptedSharedPreferences`
