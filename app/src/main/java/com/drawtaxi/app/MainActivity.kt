package com.drawtaxi.app

import java.util.Locale

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.components.core.DrawTaxiScaffold
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.data.Quote
import com.drawtaxi.app.ui.TaxiViewModel
import com.drawtaxi.app.ui.TaxiViewModelFactory
import com.drawtaxi.app.ui.components.BottomNavigationBar
import com.drawtaxi.app.ui.screens.dashboard.DashboardScreen
import com.drawtaxi.app.ui.screens.home.HomeScreen
import com.drawtaxi.app.ui.screens.invoices.InvoiceScreen
import com.drawtaxi.app.ui.screens.messages.ControlCenterScreen
import com.drawtaxi.app.ui.screens.navigation.RideNavigationScreen
import com.drawtaxi.app.ui.screens.onboarding.AiModelDownloadScreen
import com.drawtaxi.app.ui.screens.onboarding.OnboardingScreen
import com.drawtaxi.app.util.PermissionHelper
import com.drawtaxi.app.util.hasLocationPermissions
import com.drawtaxi.app.util.hasNotificationPermission
import com.drawtaxi.app.util.hasSmsPermissions
import com.drawtaxi.app.ui.screens.rides.RideCompletionScreen
import com.drawtaxi.app.ui.screens.rides.RideCreateScreen
import com.drawtaxi.app.ui.screens.rides.RideDetailScreen
import com.drawtaxi.app.ui.screens.rides.ReturnHomeScreen
import com.drawtaxi.app.ui.screens.settings.AgendaScreen
import com.drawtaxi.app.ui.screens.settings.BackupSettingsScreen
import com.drawtaxi.app.ui.screens.settings.ExportScreen
import com.drawtaxi.app.ui.screens.settings.MessageTemplatesScreen
import com.drawtaxi.app.ui.screens.settings.OvhMailSettingsScreen
import com.drawtaxi.app.ui.screens.settings.PricingSettingsScreen
import com.drawtaxi.app.ui.screens.settings.ProInfoSettings
import com.drawtaxi.app.ui.screens.settings.SettingsMain
import com.drawtaxi.app.ui.theme.DrawTaxiTheme
class MainActivity : ComponentActivity() {

    private var intentState = mutableStateOf<Intent?>(null)
    private lateinit var pendingSettingsUpdate: (AppSettings) -> Unit
    private var _currentSettings: () -> AppSettings = { AppSettings() }
    private fun currentSettings() = _currentSettings()

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        pendingSettingsUpdate(currentSettings().copy(monitorSms = allGranted))
        if (!allGranted) {
            val perms = listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS)
            val permanentlyDenied = perms.any { perm ->
                !grants.getOrDefault(perm, false) && !shouldShowRequestPermissionRationale(perm)
            }
            if (permanentlyDenied) redirectToAppSettings("SMS")
        } else {
            (application as? TaxiApplication)?.startSmsServiceIfEnabled()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingSettingsUpdate(currentSettings().copy(enableNotifications = granted))
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                redirectToAppSettings("Notifications")
            }
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        pendingSettingsUpdate(currentSettings().copy(trackLocation = allGranted))
        if (!allGranted) {
            val perms = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            val permanentlyDenied = perms.any { perm ->
                !grants.getOrDefault(perm, false) && !shouldShowRequestPermissionRationale(perm)
            }
            if (permanentlyDenied) redirectToAppSettings("Localisation")
        }
    }

    fun requestSmsPermission() {
        if (hasSmsPermissions()) {
            pendingSettingsUpdate(currentSettings().copy(monitorSms = true))
        } else {
            PermissionHelper.requestSmsPermissions(smsPermissionLauncher)
        }
    }

    fun requestNotificationPermission() {
        if (hasNotificationPermission()) {
            pendingSettingsUpdate(currentSettings().copy(enableNotifications = true))
        } else {
            PermissionHelper.requestNotificationPermission(notificationPermissionLauncher)
        }
    }

    fun requestLocationPermission() {
        if (hasLocationPermissions()) {
            pendingSettingsUpdate(currentSettings().copy(trackLocation = true))
        } else {
            PermissionHelper.requestLocationPermissions(locationPermissionLauncher)
        }
    }

    private fun redirectToAppSettings(permissionLabel: String) {
        Toast.makeText(
            this,
            "Permission $permissionLabel refusée définitivement.\nActivez-la dans Paramètres → Autorisations.",
            Toast.LENGTH_LONG
        ).show()
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = intent

        val repository = (application as TaxiApplication).repository
        val factory = TaxiViewModelFactory(repository)

        // Démarrage automatique des services de surveillance
        if (hasSmsPermissions()) {
            (application as? TaxiApplication)?.startSmsServiceIfEnabled()
        }
        (application as? TaxiApplication)?.startImapServiceIfEnabled()

        setContent {
            val viewModel: TaxiViewModel = viewModel(factory = factory)
            val settings by viewModel.settings.collectAsState()
            val validatedRides by viewModel.validatedRides.collectAsState()
            val pendingRides by viewModel.pendingRides.collectAsState()
            val allAbsences by viewModel.allAbsences.collectAsState()

            _currentSettings = { settings }
            pendingSettingsUpdate = { viewModel.updateSettings(it) }

            var activeTab by remember { mutableStateOf("home") }
            var settingsView by remember { mutableStateOf("main") }
            var selectedRide by remember { mutableStateOf<RideRequest?>(null) }
            var activeRide by remember { mutableStateOf<RideRequest?>(null) }
            var isCreatingRide by remember { mutableStateOf(false) }
            var creationText by remember { mutableStateOf("") }
            var editingRide by remember { mutableStateOf<RideRequest?>(null) }
            var showAgendaScreen by remember { mutableStateOf(false) }
            var showCompletionScreen by remember { mutableStateOf(false) }
            var completionRide by remember { mutableStateOf<RideRequest?>(null) }
            var showReturnHomeScreen by remember { mutableStateOf(false) }
            var showConfetti by remember { mutableStateOf(false) }

            val currentIntent by intentState
            LaunchedEffect(currentIntent) {
                currentIntent?.let { intent ->
                    if (intent.getBooleanExtra("open_pending", false) == true) {
                        activeTab = "message"
                    }
                    if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (sharedText != null) {
                            // Traiter le texte partagé avec l'IA
                            val aiResult = com.drawtaxi.app.logic.sms.AiSmsParser.parseWithAI(
                                this@MainActivity,
                                sharedText,
                                settings.aiEnabled
                            )
                            val ride = aiResult.toRideRequest(
                                "shared",
                                System.currentTimeMillis(),
                                settings
                            )
                            if (ride != null) {
                                // Course détectée par l'IA, l'ajouter directement
                                viewModel.addRide(ride)
                                activeTab = "message"
                                Toast.makeText(this@MainActivity, "Course détectée depuis le partage !", Toast.LENGTH_LONG).show()
                            } else {
                                // Pas de course détectée, ouvrir l'écran de création
                                creationText = sharedText
                                isCreatingRide = true
                            }
                            intentState.value = null
                        }
                    }
                }
            }

            // Redémarrer les services quand les paramètres changent
            LaunchedEffect(settings.monitorSms, settings.ovhImapEnabled) {
                if (settings.monitorSms && hasSmsPermissions()) {
                    (application as? TaxiApplication)?.startSmsServiceIfEnabled()
                }
                if (settings.ovhImapEnabled) {
                    (application as? TaxiApplication)?.startImapServiceIfEnabled()
                }
            }

             DrawTaxiTheme(brandColor = settings.brandColor, darkTheme = isSystemInDarkTheme()) {
                var onboardingStep by remember { mutableStateOf(0) }

                if (settings.isFirstLaunch) {
                    when (onboardingStep) {
                        0 -> OnboardingScreen(
                            settings = settings,
                            onUpdateSettings = { viewModel.updateSettings(it) },
                            onRequestSms = { requestSmsPermission() },
                            onRequestNotification = { requestNotificationPermission() },
                            onRequestLocation = { requestLocationPermission() },
                            onComplete = { onboardingStep = 1 }
                        )
                        1 -> {
                            BackHandler { onboardingStep = 0 }
                            AiModelDownloadScreen(
                                brandColor = settings.brandColor,
                                onSkip = {
                                    viewModel.updateSettings(settings.copy(isFirstLaunch = false))
                                },
                                onComplete = {
                                    viewModel.updateSettings(settings.copy(isFirstLaunch = false))
                                }
                            )
                        }
                    }
                } else if (showAgendaScreen) {
                    AgendaScreen(
                        absences = allAbsences,
                        settings = settings,
                        onAddAbsence = { viewModel.addAbsence(it) },
                        onDeleteAbsence = { viewModel.deleteAbsence(it) },
                        onSendMessage = { absence ->
                            viewModel.markAbsenceMessageSent(absence.id)
                            Toast.makeText(this@MainActivity, "Message d'absence envoyé", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { showAgendaScreen = false }
                    )
                } else if (showReturnHomeScreen) {
                    ReturnHomeScreen(
                        settings = settings,
                        onBack = {
                            showReturnHomeScreen = false
                            activeTab = "home"
                        }
                    )
                } else if (showCompletionScreen && completionRide != null) {
                    RideCompletionScreen(
                        ride = completionRide!!,
                        settings = settings,
                        onComplete = { ride ->
                            viewModel.completeRide(ride, this@MainActivity)
                            completionRide = null
                            showCompletionScreen = false
                            showReturnHomeScreen = true
                            showConfetti = true
                            Toast.makeText(this@MainActivity, "Course terminée !", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { completionRide = null; showCompletionScreen = false }
                    )
                } else if (isCreatingRide || editingRide != null) {
                    RideCreateScreen(
                        initialRide = editingRide,
                        sharedText = editingRide?.body ?: creationText,
                        onConfirm = { newRide ->
                            if (editingRide != null) {
                                viewModel.updateRide(newRide)
                            } else {
                                viewModel.addRide(newRide)
                            }
                            isCreatingRide = false
                            editingRide = null
                            activeTab = "message"
                            Toast.makeText(this@MainActivity, "Course enregistrée !", Toast.LENGTH_SHORT).show()
                        },
                        onCancel = {
                            isCreatingRide = false
                            editingRide = null
                        },
                        settings = settings
                    )
                 } else {
                     DrawTaxiScaffold(
                         bottomBar = {
                             BottomNavigationBar(
                                 activeTab = activeTab,
                                 onTabSelected = {
                                     activeTab = it
                                     settingsView = "main"
                                     selectedRide = null
                                     activeRide = null
                                     isCreatingRide = false
                                     editingRide = null
                                 },
                                 brandColor = settings.brandColor,
                                 hasPending = pendingRides.any { it.status == RideStatus.DRAFT || it.status == RideStatus.QUOTED }
                             )
                         }
                     ) { innerPadding ->
                         Box(modifier = Modifier.padding(innerPadding)) {
                             if (activeRide != null) {
                                RideNavigationScreen(
                                    ride = activeRide!!,
                                    settings = settings,
                                    brandColor = settings.brandColor,
                                    onBack = { activeRide = null },
                                    onComplete = { completedRide ->
                                        completionRide = completedRide
                                        showCompletionScreen = true
                                        activeRide = null
                                    },
                                    onEditRide = { updatedRide ->
                                        viewModel.updateRide(updatedRide)
                                        Toast.makeText(this@MainActivity, "Course modifiée", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else if (selectedRide != null) {
                                val isPendingRide = activeTab == "message"
                                RideDetailScreen(
                                    ride = selectedRide!!,
                                    onBack = { selectedRide = null },
                                    onDelete = {
                                        viewModel.deleteRide(it)
                                        selectedRide = null
                                    },

                                    onEdit = { ride ->
                                        editingRide = ride
                                        selectedRide = null
                                    },
                                    onStartRide = { ride ->
                                        activeRide = ride
                                        selectedRide = null
                                    },
                                    isPending = isPendingRide,
                                    settings = settings
                                )
                                BackHandler { selectedRide = null }
                            } else {
                                AnimatedContent(
                                    targetState = activeTab,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                                                fadeOut(animationSpec = tween(90))
                                    },
                                    label = "tabTransition"
                                ) { targetTab ->
                                    when (targetTab) {
                                    "home" -> {
                                        // Courses confirmées = depuis validatedRides (isPending=0) OU pendingRides avec statut CONFIRMED/IN_PROGRESS
                                        val confirmedFromValidated = validatedRides.filter {
                                            it.status == RideStatus.CONFIRMED || it.status == RideStatus.IN_PROGRESS
                                        }
                                        val confirmedFromPending = pendingRides.filter {
                                            it.status == RideStatus.CONFIRMED || it.status == RideStatus.IN_PROGRESS
                                        }
                                        val allConfirmedRides = confirmedFromValidated + confirmedFromPending
                                        
                                        // Courses terminées (COMPLETED)
                                        val completedRides = validatedRides.filter {
                                            it.status == RideStatus.COMPLETED
                                        }
                                        
                                        HomeScreen(
                                            validatedRides = completedRides,
                                            confirmedRides = allConfirmedRides,
                                            brandColor = settings.brandColor,
                                            onRideClick = { selectedRide = it },
                                            onCreateRide = {
                                                isCreatingRide = true
                                                creationText = ""
                                            },
                                            settings = settings
                                        )
                                    }
                                    "message" -> {
                                        // Filtrer pour ne garder que DRAFT et QUOTED (pas CONFIRMED/IN_PROGRESS)
                                        val messageRides = pendingRides.filter {
                                            it.status == RideStatus.DRAFT || it.status == RideStatus.QUOTED
                                        }
                                        ControlCenterScreen(
                                            pendingRides = messageRides,
                                            onValidate = { ride ->
                                                activeRide = ride
                                                selectedRide = null
                                            },
                                            onDelete = { viewModel.deleteRide(it) },
                                            onRideClick = { ride ->
                                                editingRide = ride
                                            },
                                            onUpdateRide = { viewModel.updateRide(it) },
                                            brandColor = settings.brandColor,
                                            settings = settings,
                                            onCreateRide = {
                                                isCreatingRide = true
                                                creationText = ""
                                            },
                                            onCheckSms = {
                                                viewModel.scanSmsNow(this@MainActivity)
                                                val emailIntent = android.content.Intent(
                                                    this@MainActivity,
                                                    com.drawtaxi.app.service.foreground.OvhImapService::class.java
                                                ).apply {
                                                    action = "com.drawtaxi.app.action.SCAN_NOW"
                                                }
                                                try {
                                                    startService(emailIntent)
                                                } catch (_: Exception) { }
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Vérification SMS + emails en cours...",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onSendQuote = { ride ->
                                                val priceBreakdown = com.drawtaxi.app.logic.pricing.PriceEngine.calculate(
                                                    distanceKm = ride.distanceKm,
                                                    dateTime = java.util.Calendar.getInstance(),
                                                    pricePerKm = settings.pricePerKm.toDoubleOrNull() ?: 2.50,
                                                    baseFare = settings.basePrice.toDoubleOrNull() ?: 9.00,
                                                    minDistanceKm = settings.minDistanceKm.toDoubleOrNull() ?: 3.6,
                                                    nightSurchargePercent = settings.nightSurchargePercent,
                                                    sundaySurchargePercent = settings.sundaySurchargePercent,
                                                    holidaySurchargePercent = settings.holidaySurchargePercent,
                                                    nightStartHour = settings.nightStartHour,
                                                    nightEndHour = settings.nightEndHour,
                                                    tvaTransportRate = settings.tvaTransportRate
                                                )
                                                val quoteMessage = settings.quoteTemplate
                                                    .replace("[DEPART]", ride.departure.ifBlank { "—" })
                                                    .replace("[ARRIVEE]", ride.arrival.ifBlank { "—" })
                                                    .replace("[DISTANCE]", String.format(Locale.getDefault(), "%.1f", ride.distanceKm))
                                                    .replace("[PRIX]", String.format(Locale.getDefault(), "%.2f", priceBreakdown.totalTTC))
                                                com.drawtaxi.app.logic.messaging.MessageSender.sendMessage(
                                                    this@MainActivity,
                                                    ride.messageChannel,
                                                    ride.sender,
                                                    ride.clientEmail,
                                                    quoteMessage
                                                )
                                                val quote = com.drawtaxi.app.data.Quote(
                                                    id = com.drawtaxi.app.data.Quote.createId(ride.id),
                                                    rideId = ride.id,
                                                    departure = ride.departure,
                                                    arrival = ride.arrival,
                                                    distanceKm = ride.distanceKm,
                                                    price = priceBreakdown.totalTTC,
                                                    messageChannel = ride.messageChannel,
                                                    status = com.drawtaxi.app.data.QuoteStatus.PENDING,
                                                    sentAt = System.currentTimeMillis()
                                                )
                                                viewModel.sendQuote(ride, quote, this@MainActivity)
                                                Toast.makeText(this@MainActivity, "Devis envoyé au client", Toast.LENGTH_SHORT).show()
                                            },
                                            onAcceptQuote = { ride ->
                                                // Quand une course est confirmée, elle passe isPending=false pour apparaître dans l'accueil
                                                val updatedRide = ride.copy(
                                                    status = RideStatus.CONFIRMED,
                                                    price = ride.price,
                                                    isPending = false
                                                )
                                                viewModel.updateRide(updatedRide)
                                                Toast.makeText(this@MainActivity, "Course confirmée", Toast.LENGTH_SHORT).show()
                                            },
                                            onRejectQuote = { ride ->
                                                val message = settings.rejectionTemplate
                                                com.drawtaxi.app.logic.messaging.MessageSender.sendMessage(
                                                    this@MainActivity,
                                                    ride.messageChannel,
                                                    ride.sender,
                                                    ride.clientEmail,
                                                    message
                                                )
                                                viewModel.deleteRide(ride)
                                                Toast.makeText(this@MainActivity, "Devis refusé - Course supprimée", Toast.LENGTH_SHORT).show()
                                            },
                                            onDeleteWithMessage = { ride, message ->
                                                viewModel.deleteRideWithMessage(ride, message, this@MainActivity)
                                            },
                                            onOpenAgenda = {
                                                showAgendaScreen = true
                                            },
                                            messageTemplates = settings.messageTemplates
                                        )
                                    }
                                    "dashboard" -> {
                                        DashboardScreen(
                                            validatedRides = validatedRides,
                                            pendingRides = pendingRides,
                                            brandColor = settings.brandColor,
                                            onNavigateToInvoices = {
                                                activeTab = "invoices"
                                            },
                                            onRideClick = { selectedRide = it }
                                        )
                                    }
                                    "invoices" -> {
                                        InvoiceScreen(
                                            validatedRides = validatedRides,
                                            brandColor = settings.brandColor,
                                            onRideSelected = { ride ->
                                                selectedRide = ride
                                            },
                                            onBack = {
                                                activeTab = "dashboard"
                                            },
                                            coutParKmDeplacement = settings.coutParKmDeplacement,
                                            driverName = settings.name,
                                            onRideUpdated = { updatedRide ->
                                                viewModel.updateRide(updatedRide)
                                            }
                                        )
                                    }
                                    "settings" -> {
                                        when (settingsView) {
                                            "proInfo" -> ProInfoSettings(settings, { viewModel.updateSettings(it) }, { settingsView = "main" })
                                            "pricing" -> PricingSettingsScreen(
                                                settings = settings,
                                                onUpdate = { viewModel.updateSettings(it) },
                                                onBack = { settingsView = "main" }
                                            )
                                            "backup" -> BackupSettingsScreen(
                                                settings = settings,
                                                allRides = validatedRides + pendingRides,
                                                onUpdateSettings = { viewModel.updateSettings(it) },
                                                onRestore = { restoredSettings, restoredRides ->
                                                    restoredSettings?.let { viewModel.updateSettings(it) }
                                                    restoredRides?.forEach { viewModel.addRide(it) }
                                                    settingsView = "main"
                                                },
                                                onBack = { settingsView = "main" }
                                            )
                                            "messageTemplates" -> MessageTemplatesScreen(
                                                settings = settings,
                                                onUpdateSettings = { viewModel.updateSettings(it) },
                                                onBack = { settingsView = "main" }
                                            )
                                            "ovhMail" -> OvhMailSettingsScreen(
                                                settings = settings,
                                                onUpdate = { viewModel.updateSettings(it) },
                                                onBack = { settingsView = "main" }
                                            )
                                            "aiDownload" -> AiModelDownloadScreen(
                                                brandColor = settings.brandColor,
                                                onSkip = { settingsView = "main" },
                                                onComplete = { settingsView = "main" }
                                            )
                                            "export" -> ExportScreen(
                                                rides = validatedRides,
                                                brandColor = settings.brandColor,
                                                onBack = { settingsView = "main" }
                                            )
                                            else -> {
                                                SettingsMain(
                                                    settings = settings,
                                                    onUpdate = { viewModel.updateSettings(it) },
                                                    onNavigate = { settingsView = it },
                                                    onRequestSmsPermission = { requestSmsPermission() },
                                                    onRequestNotificationPermission = { requestNotificationPermission() },
                                                    onRequestLocationPermission = { requestLocationPermission() }
                                                )
                                            }
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }
                }
                if (showConfetti) {
                    ConfettiEffect(
                        onFinished = { showConfetti = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intentState.value = intent
    }
}
