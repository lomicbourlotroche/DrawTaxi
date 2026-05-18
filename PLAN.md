# PLAN DE DÉVELOPPEMENT - DrawTaxi

## Résumé des modifications demandées

### Suppressions
- ❌ Paramètre "Visuel & Marque"
- ❌ Carnet client
- ❌ Mode sombre
- ❌ Test de notification

### Ajouts et modifications majeures
- ✅ Fonctionnement continu avec notification persistante
- ✅ Écran Tarifs & Coûts dédié (onglet)
- ✅ Option désactivation IA + vérification disponibilité
- ✅ Sélecteurs natifs date/heure dans création manuelle
- ✅ Envoi SMS devis automatique (sans quitter l'app)
- ✅ Capture réponse "OUI" par l'IA
- ✅ Courses confirmées affichées dans l'écran Accueil
- ✅ Refonte complète de l'écran de pilotage avec workflow étapes

---

## PHASE 1 : Suppression des fonctionnalités

### 1.1 Supprimer "Visuel & Marque"
**Fichiers :**
- `SettingsMain.kt` - Ligne 70 : Supprimer menu item
- `MainActivity.kt` - Ligne 438 : Supprimer navigation "branding"

**Action :** Supprimer la section complète

### 1.2 Supprimer "Carnet Client"
**Fichiers :**
- `SettingsMain.kt` - Ligne 86 : Supprimer menu item
- `MainActivity.kt` - Lignes 455-459 : Supprimer navigation "clients"

**Action :** Supprimer la section Communication > Carnet Client

### 1.3 Supprimer "Mode Sombre"
**Fichiers :**
- `SettingsMain.kt` - Lignes 112-120 : Supprimer section Apparence
- `MainActivity.kt` - Ligne 161 : Forcer `darkTheme = false`

**Action :** Toujours utiliser le thème clair

### 1.4 Supprimer "Test de notification"
**Fichiers :**
- `SettingsMain.kt` - Lignes 185-187 : Supprimer section Diagnostic
- `MainActivity.kt` : Supprimer paramètre `onTestNotification` dans SettingsMain

---

## PHASE 2 : Fonctionnement continu

### 2.1 Démarrage automatique du service SMS
**Fichier :** `MainActivity.kt`
```kotlin
// Dans onCreate(), après setContent
if (settings.monitorSms && hasSmsPermissions()) {
    (application as? TaxiApplication)?.startSmsServiceIfEnabled()
}
```

### 2.2 Notification persistante
**Fichier :** `SmsForegroundService.kt`
- Modifier `startForeground()` pour afficher notification persistante
- Titre : "DrawTaxi - Surveillance active"
- Texte : "En attente de demandes de course..."
- Icône : Icône taxi
- Non dismissible

---

## PHASE 3 : Écran Tarifs & Coûts dédié

### 3.1 Créer `PricingSettingsScreen.kt`
**Chemin :** `app/src/main/java/com/drawtaxi/app/ui/screens/PricingSettingsScreen.kt`

**Contenu :**
- Prix au kilomètre (€/km)
- Prise en charge (€)
- Majoration nuit (%)
- Majoration dimanche (%)
- Majoration férié (%)
- Heure début nuit
- Heure fin nuit
- Coût carburant/km (€)
- Coût opérationnel/heure (€)
- TVA transport (%)
- TVA attente (%)

**Design :** Cartes par catégorie (Tarifs / Coûts / TVA)

### 3.2 Modifier `SettingsMain.kt`
- Remplacer section "Tarifs & Coûts" actuelle (lignes 73-81)
- Par un `SettingsMenuItem` pointant vers `PricingSettingsScreen`

---

## PHASE 4 : Intelligence Artificielle

### 4.1 Section IA améliorée dans `SettingsMain.kt`
**Remplacer lignes 89-110 par :**
```kotlin
TaxiCard(title = "Intelligence Artificielle") {
    // Toggle activation IA
    TaxiToggleRow(
        title = "Activer l'analyse IA",
        subtitle = "Utiliser l'IA pour analyser les SMS",
        checked = settings.aiEnabled,
        onCheckedChange = { onUpdate(settings.copy(aiEnabled = it)) },
        icon = Icons.Default.SmartToy
    )
    
    // Indicateur de statut
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        val (color, text, icon) = when {
            !settings.aiEnabled -> Triple(Color.Gray, "IA désactivée", Icons.Default.Block)
            isAiModelAvailable() -> Triple(Color.Green, "IA opérationnelle", Icons.Default.CheckCircle)
            else -> Triple(Color.Yellow, "IA indisponible - Mode regex", Icons.Default.Warning)
        }
        // Affichage indicateur
    }
}
```

### 4.2 Fonction de vérification
**Fichier :** `SettingsMain.kt` ou helper
```kotlin
@Composable
fun isAiModelAvailable(): Boolean {
    val context = LocalContext.current
    return remember { LlamaModelManager.isModelAvailable(context) }
}
```

---

## PHASE 5 : Sélecteurs Date/Heure natifs

### 5.1 Modifier `RideCreateScreen.kt`
**Remplacer lignes 323-341 (champs texte date/heure) :**

```kotlin
// Date picker
val context = LocalContext.current
val calendar = remember { Calendar.getInstance() }

var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
var selectedTime by remember { mutableStateOf(Calendar.getInstance()) }

val datePickerDialog = DatePickerDialog(
    context,
    { _, year, month, day ->
        selectedDate.set(year, month, day)
        date = String.format("%02d/%02d/%04d", day, month + 1, year)
    },
    calendar.get(Calendar.YEAR),
    calendar.get(Calendar.MONTH),
    calendar.get(Calendar.DAY_OF_MONTH)
)

val timePickerDialog = TimePickerDialog(
    context,
    { _, hour, minute ->
        selectedTime.set(Calendar.HOUR_OF_DAY, hour)
        selectedTime.set(Calendar.MINUTE, minute)
        time = String.format("%02dh%02d", hour, minute)
    },
    calendar.get(Calendar.HOUR_OF_DAY),
    calendar.get(Calendar.MINUTE),
    true
)

// UI
Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedButton(
        onClick = { datePickerDialog.show() },
        modifier = Modifier.weight(1f)
    ) {
        Icon(Icons.Default.CalendarToday, null)
        Text(date.ifEmpty { "Date" })
    }
    OutlinedButton(
        onClick = { timePickerDialog.show() },
        modifier = Modifier.weight(1f)
    ) {
        Icon(Icons.Default.Schedule, null)
        Text(time.ifEmpty { "Heure" })
    }
}
```

---

## PHASE 6 : Envoi SMS automatique

### 6.1 Modifier `MessageSender.kt`
**Remplacer la fonction `sendSms` :**

```kotlin
fun sendSms(context: Context, phone: String, message: String): Boolean {
    return try {
        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
        Toast.makeText(context, "SMS envoyé", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur envoi SMS", Toast.LENGTH_SHORT).show()
        false
    }
}
```

**Permission requise :** `SEND_SMS` (déjà présente)

### 6.2 Modifier `ControlCenterScreen.kt` - Envoi devis
**Ligne ~461 (onSendQuote) :**
- Appeler directement `MessageSender.sendSms()` au lieu d'ouvrir l'app SMS
- Afficher Toast de confirmation

---

## PHASE 7 : Capture réponse "OUI"

### 7.1 Vérifier `AiSmsParser.kt`
**Déjà présent :** `isConfirmation` dans `AiParsedResult`

**À ajouter dans le prompt système :**
```
Détecte si le message est une confirmation (OUI, accepté, ok, c'est bon, etc.)
isConfirmation: true/false
```

### 7.2 Créer `QuoteResponseHandler.kt`
**Nouveau fichier** pour gérer les réponses aux devis :
```kotlin
object QuoteResponseHandler {
    fun handleResponse(context: Context, sender: String, body: String, viewModel: TaxiViewModel) {
        // Chercher le devis correspondant au sender
        // Si OUI → Confirmer la course
        // Si NON → Marquer comme refusée
    }
}
```

### 7.3 Intégrer dans `SmsProcessor.kt`
- Après parsing, si `isConfirmation` → Appeler `QuoteResponseHandler`

---

## PHASE 8 : Courses confirmées dans Accueil

### 8.1 Modifier `HomeScreen.kt`
**Ajouter affichage des courses confirmées :**

```kotlin
// Dans HomeScreen, ajouter paramètre
confirmedRides: List<RideRequest>

// Afficher section "Courses confirmées"
if (confirmedRides.isNotEmpty()) {
    Text("Courses confirmées", style = titleStyle)
    confirmedRides.forEach { ride ->
        ConfirmedRideCard(ride, onClick)
    }
}
```

### 8.2 Modifier `MainActivity.kt` - Ligne 309-320
**Passer les courses confirmées à HomeScreen :**
```kotlin
"home" -> {
    val confirmedRides = validatedRides.filter { 
        it.status == RideStatus.CONFIRMED || it.status == RideStatus.IN_PROGRESS 
    }
    HomeScreen(
        validatedRides = validatedRides.filter { it.status == RideStatus.COMPLETED },
        confirmedRides = confirmedRides,
        ...
    )
}
```

### 8.3 Modifier `ControlCenterScreen.kt`
**Filtrer pour ne plus afficher les confirmées :**
```kotlin
// Dans MainActivity, passer uniquement les non-confirmées
pendingRides = pendingRides.filter { 
    it.status != RideStatus.CONFIRMED && it.status != RideStatus.IN_PROGRESS 
}
```

---

## PHASE 9 : Refonte écran de pilotage

### 9.1 Simplifier `ActiveRideScreen.kt`

**Structure cible :**
```kotlin
@Composable
fun ActiveRideScreen(
    ride: RideRequest,
    settings: AppSettings,
    onComplete: (RideRequest) -> Unit,
    onCancel: () -> Unit
) {
    // État de l'étape actuelle
    var currentStep by remember { mutableStateOf(NavigationStep.TO_PICKUP) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Carte en plein écran
        NavigationMapView(ride, currentStep)
        
        // Overlay instructions
        NavigationOverlay(currentStep, onStepComplete)
        
        // Bouton action principal flottant
        FloatingActionButton(onStepComplete)
    }
}
```

### 9.2 Étapes de navigation

```kotlin
enum class NavigationStep {
    TO_PICKUP,      // Aller chercher le client
    TO_DESTINATION, // Aller à destination
    TO_HOME,        // Retour maison (optionnel)
    COMPLETE        // Terminer
}
```

### 9.3 Workflow détaillé

**Étape 1 - ALLER_RÉCUPÉRER_CLIENT :**
- Destination : `ride.departure`
- Bouton : "Client récupéré" → Étape 2

**Étape 2 - ALLER_DESTINATION :**
- Destination : `ride.arrival`
- Bouton : "Destination atteinte" → Afficher choix

**Étape 3 - Choix après destination :**
```
┌─────────────────────────────────┐
│  Que souhaitez-vous faire ?     │
│                                 │
│  [ Retour à domicile ]          │
│  [ Terminer la course ]         │
└─────────────────────────────────┘
```

**Si "Retour à domicile" :**
- Navigation vers `settings.homeAddress`
- Bouton "Terminer" → Étape 4

**Si "Terminer la course" :**
- Directement Étape 4

**Étape 4 - RÉCAPITULATIF :**
- Distance totale parcourue
- Temps total
- Prix final
- Bouton "Générer facture" → `RideCompletionScreen`

**Étape 5 - FERMETURE :**
- Sauvegarder la course
- Retour à l'écran Accueil

### 9.4 Supprimer les fonctionnalités non essentielles
- ❌ Bouton appel depuis l'écran
- ❌ Bouton message depuis l'écran  
- ❌ Menu options (3 points)
- ✅ Garder uniquement : Carte + Bouton étape + Infos essentielles

---

## Fichiers à modifier/créer

### Fichiers à modifier
1. `MainActivity.kt` - Navigation et structure
2. `SettingsMain.kt` - Suppressions et ajouts
3. `ControlCenterScreen.kt` - Filtre et envoi SMS
4. `HomeScreen.kt` - Affichage courses confirmées
5. `ActiveRideScreen.kt` - Refonte complète
6. `RideCreateScreen.kt` - Date/Heure pickers
7. `MessageSender.kt` - Envoi SMS automatique
8. `AiSmsParser.kt` - Vérification réponse OUI
9. `SmsForegroundService.kt` - Notification persistante

### Fichiers à créer
1. `PricingSettingsScreen.kt` - Écran tarifs dédié
2. `QuoteResponseHandler.kt` - Gestion réponses devis

### Fichiers à supprimer (optionnel)
1. `BrandingSettings.kt`
2. `ClientDirectoryScreen.kt`
3. `GpsNavigationScreen.kt` (remplacé par ActiveRideScreen)

---

## Ordre d'implémentation recommandé

1. **Phase 1** - Suppressions (rapide)
2. **Phase 3** - Écran Tarifs (isolated)
3. **Phase 5** - Date/Heure pickers (isolated)
4. **Phase 4** - IA toggle (isolated)
5. **Phase 6** - Envoi SMS auto (testable)
6. **Phase 8** - Courses confirmées Accueil
7. **Phase 7** - Capture OUI (nécessite 6)
8. **Phase 2** - Notification persistante
9. **Phase 9** - Refonte pilotage (complexe)

---

## Notes techniques

### Permissions nécessaires
- `SEND_SMS` - Déjà présente
- `RECEIVE_SMS` - Déjà présente
- `FOREGROUND_SERVICE` - Déjà présente
- `POST_NOTIFICATIONS` - Déjà présente

### Dépendances
- `androidx.compose.material3:material3` - DatePickerDialog
- Osmdroid - Déjà présent

### Tests à effectuer
- Envoi SMS sans quitter l'app
- Réception SMS avec réponse OUI
- Navigation étapes pilotage
- Notification persistante après reboot

---

*Document créé le : 18/05/2026*
*Projet : DrawTaxi*
