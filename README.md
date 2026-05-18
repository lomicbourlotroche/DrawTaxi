# DrawTaxi

Application Android native (Kotlin + Jetpack Compose) de gestion de courses taxi/VTC. Réception automatique des commandes par SMS, suivi de rentabilité, synchronisation comptable Kolecto, et navigation GPS intégrée.

---

## Fonctionnalités

### Gestion des Courses
- **Réception automatique SMS** - Détection en temps réel des nouvelles commandes via `BroadcastReceiver` + `ContentObserver` avec debounce et déduplication
- **Parsing intelligent** - Analyse contextuelle des SMS avec scoring de confiance, détection d'annulation/modification/confirmation
- **Création manuelle** - Ajout de courses via formulaire ou partage depuis d'autres apps
- **Cycle de vie complet** - En attente → Confirmée → En cours → Terminée
- **Devis** - Génération et envoi de devis par SMS/WhatsApp/Email

### Rentabilité & Comptabilité
- **Calcul de rentabilité** - Pourcentage de marge par course (revenu - carburant - coûts opérationnels)
- **Coûts configurables** - Coût carburant/km et coût opérationnel/heure dans les paramètres
- **Statistiques** - Revenus, bénéfices nets, rentabilité par jour/semaine/mois
- **Écran Factures** - Liste des courses terminées avec filtres, détails pour création manuelle sur Kolecto
- **Reçus** - Envoi de reçus textuels par SMS/WhatsApp/Email

### Navigation & Pilotage
- **Mode Pilotage** - Écran GPS intégré avec osmdroid/OpenStreetMap
- **Suivi en temps réel** - Position GPS, vitesse, ETA, distance restante
- **Fused Location** - Utilisation de Google Play Services pour une localisation précise

### Interface Moderne
- **Design React-like** - Cards avec ombres, gradients, badges de statut, icônes dans des conteneurs arrondis
- **Palette Tailwind** - Couleurs Indigo, Emerald, Rose, Amber, Violet
- **Mode sombre** - Support complet du dark mode
- **Navigation bottom** - Barre flottante avec indicateur de messages en attente

---

## Architecture

```
com.drawtaxi.app/
├── MainActivity.kt              # Point d'entrée, navigation
├── TaxiApplication.kt           # Application class, init DB/Repository
├── data/
│   ├── Models.kt                # RideRequest, AppSettings, Quote, Absence, Client
│   ├── TaxiRepository.kt        # Couche repository (Flow, StateFlow)
│   ├── BackupManager.kt         # Sauvegarde/restauration JSON
│   └── local/
│       ├── AppDatabase.kt       # Room Database (v6)
│       ├── RideDao.kt           # Opérations CRUD rides
│       ├── RideEntity.kt        # Entity Room + mappings
│       └── SettingsManager.kt   # DataStore Preferences
├── logic/
│   ├── ParseSms.kt              # Parsing SMS avec IA contextuelle
│   ├── SmsReceiver.kt           # BroadcastReceiver SMS entrants
│   ├── SmsWatcher.kt            # ContentObserver avec debounce
│   ├── SmsForegroundService.kt  # Service foreground surveillance SMS
│   ├── SmsScanner.kt            # Scan historique SMS
│   ├── RideMatcher.kt           # Matching SMS ↔ courses existantes
│   ├── ShareUtils.kt            # Partage reçus (SMS/WhatsApp/Email)
│   ├── MessageSender.kt         # Envoi SMS/WhatsApp/Email
│   ├── SmsUtils.kt              # Utilitaires SMS
│   ├── NotificationHelper.kt    # Notifications système
│   ├── PermissionHelper.kt      # Gestion des permissions
│   ├── LocationTrackingService.kt # Suivi de position
│   ├── FetchRoute.kt            # Récupération d'itinéraire
│   ├── BootReceiver.kt          # Redémarrage au boot
│   ├── StatsReportScheduler.kt  # Planification rapports
│   └── WebFormApiHandler.kt     # Gestion formulaires web
├── ui/
│   ├── TaxiViewModel.kt         # ViewModel principal + Factory
│   ├── components/
│   │   ├── BottomNavigation.kt  # Navigation bottom flottante
│   │   ├── TaxiCard.kt          # Cards modernes avec ombres
│   │   ├── RideCard.kt          # Cards courses avec rentabilité
│   │   ├── TaxiInputField.kt    # Champs de saisie stylisés
│   │   ├── TaxiLogo.kt          # Logo de l'app
│   │   ├── RideCreateForm.kt    # Formulaire création course
│   │   ├── RideDetailMapSection.kt # Section map détail
│   │   ├── RideMap.kt           # Map osmdroid
│   │   └── RouteToClientMap.kt  # Map itinéraire client
│   ├── screens/
│   │   ├── HomeScreen.kt        # Accueil avec gradient header
│   │   ├── ControlCenterScreen.kt # Centre de contrôle (en attente)
│   │   ├── StatsScreen.kt       # Statistiques avec rentabilité
│   │   ├── AccountingScreen.kt  # Comptabilité + rentabilité
│   │   ├── InvoiceScreen.kt     # Liste factures + détails Kolecto
│   │   ├── RideDetailScreen.kt  # Détail course + rentabilité
│   │   ├── RideCompletionScreen.kt # Fin de course + Kolecto
│   │   ├── RideCreateScreen.kt  # Création manuelle
│   │   ├── GpsNavigationScreen.kt # Navigation GPS
│   │   ├── SettingsMain.kt      # Paramètres principaux
│   │   ├── QuoteScreen.kt       # Gestion des devis
│   │   ├── AgendaScreen.kt      # Gestion absences
│   │   ├── ActiveRideScreen.kt  # Course en cours
│   │   ├── OnboardingScreen.kt  # Premier lancement
│   │   ├── BackupSettingsScreen.kt # Sauvegarde
│   │   ├── BrandingSettings.kt  # Personnalisation visuelle
│   │   ├── ClientDirectoryScreen.kt # Carnet clients
│   │   ├── ExportScreen.kt      # Export données
│   │   ├── MessageTemplatesScreen.kt # Templates messages
│   │   ├── ProInfoSettings.kt   # Infos professionnelles
│   │   └── ProfileScreen.kt     # Profil
│   └── theme/
│       ├── Color.kt             # Palette de couleurs
│       ├── Theme.kt             # Thème Material3
│       └── Typography.kt        # Typographie
└── car/
    ├── TaxiCarAppService.kt     # Android Auto
    ├── RideListScreen.kt        # Liste courses Android Auto
    ├── RideDetailCarScreen.kt   # Détail Android Auto
    └── EditRideCarScreen.kt     # Édition Android Auto
```

---

## Modèle de Données

### RideRequest
| Champ | Type | Description |
|-------|------|-------------|
| `id` | String | UUID stable (hash sender+body+timestamp) |
| `sender` | String | Numéro de téléphone du client |
| `body` | String | Contenu du SMS original |
| `departure` | String | Lieu de départ |
| `arrival` | String | Lieu d'arrivée |
| `time` | String | Heure de prise en charge |
| `date` | String | Date de la course |
| `distanceKm` | Double | Distance en kilomètres |
| `price` | Double | Prix de la course |
| `status` | RideStatus | PENDING, QUOTED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, ABSENT |
| `fuelCost` | Double | Coût carburant estimé |
| `operatingCost` | Double | Coûts opérationnels estimés |
| `durationMinutes` | Int | Durée de la course en minutes |
| `profitabilityPercent` | Double | Pourcentage de rentabilité |
| `clientEmail` | String | Email du client |
| `messageChannel` | MessageChannel | SMS, WHATSAPP, EMAIL, WEB_FORM |
| `quoteId` | String | ID du devis associé |
| `notes` | String | Notes sur la course |

### AppSettings
| Champ | Type | Description |
|-------|------|-------------|
| `companyName` | String | Nom de l'entreprise |
| `name` | String | Nom du chauffeur |
| `siret` | String | Numéro SIRET |
| `vehicle` | String | Véhicule |
| `pricePerKm` | String | Prix par kilomètre |
| `basePrice` | String | Prise en charge |
| `brandColor` | Color | Couleur principale |
| `darkMode` | Boolean | Mode sombre |
| `monitorSms` | Boolean | Surveillance SMS |
| `kolectoSyncEnabled` | Boolean | Sync Kolecto activée |
| `kolectoApiKey` | String | Clé API Kolecto |
| `fuelCostPerKm` | Double | Coût carburant/km (défaut: 0.12) |
| `operatingCostPerHour` | Double | Coût opérationnel/heure (défaut: 15.0) |

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

## Parsing SMS

Le parser utilise une approche contextuelle avec :

1. **Détection de mots-clés** taxi, départ, arrivée, heure
2. **Analyse de contexte** : salutations, politesse, urgence, longueur du message
3. **Scoring de confiance** par champ et global
4. **Déduplication** : cache des SMS traités (fenêtre 30s)
5. **Détection d'intention** : confirmation, annulation, modification
6. **Extraction de date** : aujourd'hui, demain, dates relatives

---

## Permissions Requises

| Permission | Usage |
|------------|-------|
| `RECEIVE_SMS` | Réception automatique des commandes |
| `READ_SMS` | Lecture de l'historique SMS |
| `SEND_SMS` | Envoi de réponses aux clients |
| `POST_NOTIFICATIONS` | Notifications de nouvelles courses |
| `ACCESS_FINE_LOCATION` | GPS mode pilotage |
| `ACCESS_COARSE_LOCATION` | Localisation approximative |
| `FOREGROUND_SERVICE` | Surveillance SMS en background |

---

## Configuration Technique

- **Kotlin** : 1.9.23
- **JVM Target** : 1.8
- **Compose Compiler** : 1.5.11
- **Min SDK** : 24
- **Target SDK** : 34
- **Compile SDK** : 34
- **Room** : 2.6.1
- **Navigation Compose** : 2.7.7
- **osmdroid** : 6.1.20
- **Google Play Location** : 21.1.0

---

## Dépendances Clés

```kotlin
// Compose
implementation("androidx.compose:compose-bom:2024.02.01")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.7")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Maps
implementation("org.osmdroid:osmdroid-android:6.1.20")

// Location
implementation("com.google.android.gms:play-services-location:21.1.0")

// Android Auto
implementation("androidx.car.app:app:1.4.0")
```

---

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Tests
./gradlew test

# Lint
./gradlew lint

# Clean
./gradlew clean
```

---

## Changelog

### v2.0 (Mise à jour majeure)
- **Suppression** de la génération de factures PDF
- **Suppression** de l'intégration API Kolecto (remplacée par saisie manuelle)
- **Ajout** de l'écran Factures avec liste filtrable et détails pour Kolecto
- **Ajout** du calcul de rentabilité en pourcentage
- **Amélioration** du parsing SMS avec analyse contextuelle
- **Amélioration** de la réception SMS (debounce, déduplication)
- **Ajout** du mode navigation GPS avec osmdroid
- **Redesign** complet de l'UI (style moderne React-like, palette Tailwind)

### v1.0
- Gestion de courses avec réception SMS
- Génération de factures PDF
- Statistiques de base
- Android Auto
