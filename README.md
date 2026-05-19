# DrawTaxi

Application Android native (Kotlin + Jetpack Compose) de gestion de courses taxi/VTC. Réception automatique des commandes par SMS (avec parsing IA locale), suivi de rentabilité, navigation GPS intégrée, comptabilité et support Android Auto.

---

## Fonctionnalités

### Réception & Parsing SMS (3 couches)
- **SmsReceiver** — BroadcastReceiver temps réel avec priorité 999, déduplication 30s
- **SmsWatcher** — ContentObserver avec debounce 2s, utilisé par le foreground service
- **SmsForegroundService** — Service foreground avec polling 10s + ContentObserver, vérifie boîte réception **et** envoyés
- **Parsing regex** — Analyse contextuelle avancée (salutations, politesse, urgence), scoring de confiance, détection annulation/modification/confirmation
- **Parsing IA** — Llama 3.2 3B (Q4_K_M, ~2 Go) exécuté localement pour classification et extraction structurée

### Gestion des Courses
- **Devis** — Génération et envoi par SMS/WhatsApp/Email
- **Cycle de vie** — Brouillon → Devis envoyé → Confirmée → En cours → Terminée / Annulée / Absent
- **RideMatcher** — Détection automatique : nouvelle course, doublon, modification, annulation, clarification
- **Création manuelle** — Formulaire + partage depuis d'autres apps

### Tarification & Rentabilité
- **PriceEngine** — Calcul complet : prise en charge + km + surcharges nuit/dimanche/jour férié + attente + TVA (10% transport, 20% attente)
- **Rentabilité** — `((prix - coûts) / prix) × 100` avec carburant (0,12 €/km) et coûts opérationnels (15 €/h)
- **Statistiques** — Revenus, bénéfices, rentabilité par jour/semaine/mois

### Navigation & Pilotage
- **osmdroid** — Carte OpenStreetMap avec itinéraire OSRM
- **FusedLocationProvider** — Localisation précise en temps réel
- **Mode pilotage** — Vitesse, ETA, distance restante
- **Geocoding** — Multi-provider (Photon, Nominatim, Android Geocoder)
- **LocationTrackingService** — Service foreground de suivi GPS avec historique

### Comptabilité & Factures
- **Écran Factures** — Liste des courses terminées avec filtres, préparation des données pour saisie manuelle sur Kolecto (pas d'API)
- **AccountingScreen** — Analyse revenus/coûts
- **Pas de reçus** — Le chauffeur saisit les données manuellement sur Kolecto

### Messagerie
- **4 canaux** — SMS, WhatsApp, Email, Formulaire web
- **Templates personnalisables** — Devis, refus, arrivée, absence, demande d'infos
- **OVH** — SMTP (envoi) + IMAP (réception des réservations web)

### Intelligence Artificielle (Locale)
- **Llama 3.2 3B Instruct** — Modèle GGUF téléchargé depuis HuggingFace (~2 Go)
- **Téléchargement** — DownloadManager + fallback OkHttp, reprise, 5 tentatives
- **Gestion** — Auto-déchargement après 10 min d'inactivité, statuts : NOT_DOWNLOADED → DOWNLOADING → READY → UNLOADED
- **Utilisation** — Classification SMS (TAXI/NON_TAXI), parsing SMS (JSON structuré), parsing email

### Android Auto
- **3 écrans** — Liste des courses, détail, édition
- **CarAppService** — Session Android Auto avec ListTemplate

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

---

## Architecture

**MVVM + Repository + 3 couches de détection SMS**

```
com.drawtaxi.app/
├── MainActivity.kt              # Single Activity, navigation state-machine
├── TaxiApplication.kt           # Init DB/Repository + callback onNewSms()
├── car/                         # Android Auto
│   ├── TaxiCarAppService.kt
│   ├── RideListScreen.kt
│   ├── RideDetailCarScreen.kt
│   └── EditRideCarScreen.kt
├── data/                        # Data layer
│   ├── Models.kt                # RideRequest, AppSettings, Quote, Absence, Client, StatsReport
│   ├── TaxiRepository.kt        # Repository (Flow → StateFlow)
│   ├── BackupManager.kt         # Backup/restore JSON
│   └── local/
│       ├── AppDatabase.kt       # Room v6 (rides, quotes, absences)
│       ├── RideEntity.kt        # Entities + domain mappers
│       ├── RideDao.kt           # CRUD : RideDao, QuoteDao, AbsenceDao
│       └── SettingsManager.kt   # DataStore (~50 clés)
├── logic/                       # Business logic
│   ├── ai/
│   │   ├── LlamaModelManager.kt # Download & manage Llama 3.2
│   │   └── LlmRunner.kt         # Native inference runner
│   ├── geocoding/
│   │   ├── GeocodingService.kt  # Multi-provider geocoding
│   │   └── AddressNormalizer.kt
│   ├── messaging/
│   │   ├── MessageSender.kt     # SMS/WhatsApp/Email
│   │   ├── NotificationHelper.kt
│   │   ├── EmailFormParser.kt   # Web form email parser
│   │   ├── OvhMailSender.kt     # SMTP OVH
│   │   └── WebFormApiHandler.kt
│   ├── pricing/
│   │   ├── PriceEngine.kt       # Full pricing with surcharges & TVA
│   │   ├── QuoteResponseHandler.kt
│   │   └── RideCalculator.kt    # Period stats & daily breakdown
│   ├── routing/
│   │   ├── OsrmRoutingService.kt
│   │   ├── FetchRoute.kt        # OSRM route fetch
│   │   └── OsmRoutingService.kt
│   └── sms/
│       ├── ParseSms.kt          # Regex parser (contextual, 30+ keywords)
│       ├── AiSmsParser.kt       # AI parser (Llama 3.2)
│       ├── SmsProcessor.kt      # Processing pipeline orchestrator
│       ├── SmsWatcher.kt        # ContentObserver
│       ├── SmsScanner.kt        # One-time historical scan
│       ├── SmsUtils.kt
│       └── RideMatcher.kt       # Match SMS → existing rides
├── receiver/
│   ├── SmsReceiver.kt           # BroadcastReceiver (priority 999)
│   └── BootReceiver.kt          # Boot + package replaced
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
│   ├── TaxiViewModel.kt         # ViewModel principal
│   ├── components/
│   │   ├── BottomNavigation.kt  # 4 tabs flottants
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
│       ├── Color.kt             # Palette Tailwind
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
| TVA | `tvaTransportRate` (10%), `tvaWaitTimeRate` (20%) |
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

```kotlin
fun calculateProfitability(price: Double, fuelCost: Double, operatingCost: Double): Double {
    val totalCost = fuelCost + operatingCost
    if (totalCost == 0.0 || price == 0.0) return 0.0
    return ((price - totalCost) / price) * 100.0
}
```

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
| **TVA transport** | 10% sur montant transport |
| **TVA attente** | 20% sur montant attente |

---

## Parsing SMS

### Regex (ParseSms)
1. **30+ mots-clés** par catégorie (départ, arrivée, heure, confirmation, annulation, modification)
2. **Analyse de contexte** : salutations, politesse, urgence, longueur
3. **Scoring de confiance** par champ et global
4. **Cache déduplication** : fenêtre 30s
5. **Extraction date/heure** : aujourd'hui, demain, dates relatives, formats `14h30`, `14:30`

### IA (AiSmsParser)
- Modèle Llama 3.2 3B quantifié (Q4_K_M) exécuté en local
- Classification binaire : TAXI / NON_TAXI
- Extraction JSON structurée : départ, arrivée, heure, date, passagers, prix, nom, email
- Fallback automatique vers regex si modèle non disponible

---

## Flux SMS Complet

```
SMS entrant
    │
    ├─ SmsReceiver (BroadcastReceiver, temps réel)
    ├─ SmsWatcher (ContentObserver, debounce 2s)
    └─ SmsForegroundService (polling 10s)
    │
    └─ TaxiApplication.onNewSms()
        │
        └─ SmsProcessor.processSms()
            ├─ AiSmsParser.parseWithAI() (ou regex fallback)
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

---

## Dépendances Clés

```kotlin
// Compose BOM 2024.05.00
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.compose.material:material-icons-extended")

// Room 2.6.1
kapt("androidx.room:room-compiler:2.6.1")

// DataStore Preferences 1.0.0
implementation("androidx.datastore:datastore-preferences:1.0.0")

// osmdroid 6.1.20 (cartes OSM)
// MapLibre 10.3.1 (alternative)

// Google Play Services Location 21.1.0
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

// Android Auto 1.4.0
implementation("androidx.car.app:app:1.4.0")

// WorkManager 2.9.0
implementation("androidx.work:work-runtime-ktx:2.9.0")

// OkHttp 4.12.0 (IA inference, géocodage fallback)
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// JavaMail 1.6.7 (OVH SMTP/IMAP)
implementation("com.sun.mail:android-mail:1.6.7")
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
| Kotlin | 1.9.23 |
| JVM Target | 1.8 |
| Compose Compiler | 1.5.11 |
| Min SDK | 24 |
| Target SDK | 34 |
| Compile SDK | 34 |
| Room | 2.6.1 |
| Database version | 6 |

---

## Notes Importantes

- **Pas de PDF** — Pas de génération de factures dans l'app
- **Pas d'API Kolecto** — L'écran factures prépare les données pour saisie manuelle sur Kolecto web
- **Pas de reçus automatiques** — Le chauffeur remplit Kolecto manuellement, aucun reçu texte n'est envoyé par l'app
- **Modèle IA** — Llama 3.2 3B (~2 Go) téléchargé au premier lancement, stocké dans `filesDir`
- **SMS permissions critiques** — L'app ne fonctionne pas sans `RECEIVE_SMS`, `READ_SMS`, `SEND_SMS`
- **Foreground service** — SmsForegroundService se redémarre automatiquement si tué
- **Rentabilité** — `fuelCostPerKm = 0.12 €`, `operatingCostPerHour = 15 €` (configurables)
- **Base de données** — Version 6, migration destructive (`fallbackToDestructiveMigration()`)
