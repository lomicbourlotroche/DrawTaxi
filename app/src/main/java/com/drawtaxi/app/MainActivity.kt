package com.drawtaxi.app

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.data.RideRequest
import com.drawtaxi.app.data.RideStatus
import com.drawtaxi.app.data.Quote
import com.drawtaxi.app.ui.TaxiViewModel
import com.drawtaxi.app.ui.TaxiViewModelFactory
import com.drawtaxi.app.ui.components.BottomNavigationBar
import com.drawtaxi.app.ui.screens.*
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
        if (com.drawtaxi.app.logic.PermissionHelper.hasSmsPermissions(this)) {
            pendingSettingsUpdate(currentSettings().copy(monitorSms = true))
        } else {
            com.drawtaxi.app.logic.PermissionHelper.requestSmsPermissions(smsPermissionLauncher)
        }
    }

    fun requestNotificationPermission() {
        if (com.drawtaxi.app.logic.PermissionHelper.hasNotificationPermission(this)) {
            pendingSettingsUpdate(currentSettings().copy(enableNotifications = true))
        } else {
            com.drawtaxi.app.logic.PermissionHelper.requestNotificationPermission(notificationPermissionLauncher)
        }
    }

    fun requestLocationPermission() {
        if (com.drawtaxi.app.logic.PermissionHelper.hasLocationPermissions(this)) {
            pendingSettingsUpdate(currentSettings().copy(trackLocation = true))
        } else {
            com.drawtaxi.app.logic.PermissionHelper.requestLocationPermissions(locationPermissionLauncher)
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

        setContent {
            val viewModel: TaxiViewModel = viewModel(factory = factory)
            val settings by viewModel.settings.collectAsState()
            val validatedRides by viewModel.validatedRides.collectAsState()
            val pendingRides by viewModel.pendingRides.collectAsState()
            val allAbsences by viewModel.allAbsences.collectAsState()
            val allQuotes by viewModel.allQuotes.collectAsState()

            _currentSettings = { settings }
            pendingSettingsUpdate = { viewModel.updateSettings(it) }

            var activeTab by remember { mutableStateOf("home") }
            var settingsView by remember { mutableStateOf("main") }
            var selectedRide by remember { mutableStateOf<RideRequest?>(null) }
            var activeRide by remember { mutableStateOf<RideRequest?>(null) }
            var isCreatingRide by remember { mutableStateOf(false) }
            var creationText by remember { mutableStateOf("") }
            var editingRide by remember { mutableStateOf<RideRequest?>(null) }
            var showQuoteScreen by remember { mutableStateOf(false) }
            var quoteRide by remember { mutableStateOf<RideRequest?>(null) }
            var showAgendaScreen by remember { mutableStateOf(false) }
            var showCompletionScreen by remember { mutableStateOf(false) }
            var completionRide by remember { mutableStateOf<RideRequest?>(null) }

            val currentIntent by intentState
            LaunchedEffect(currentIntent) {
                currentIntent?.let { intent ->
                    if (intent.getBooleanExtra("open_pending", false) == true) {
                        activeTab = "message"
                    }
                    if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (sharedText != null) {
                            creationText = sharedText
                            isCreatingRide = true
                            intentState.value = null
                        }
                    }
                }
            }

            DrawTaxiTheme(brandColor = settings.brandColor, darkTheme = settings.darkMode) {
                if (settings.isFirstLaunch) {
                    OnboardingScreen(
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onRequestSms = { requestSmsPermission() },
                        onRequestNotification = { requestNotificationPermission() },
                        onRequestLocation = { requestLocationPermission() },
                        onComplete = { }
                    )
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
                } else if (showQuoteScreen && quoteRide != null) {
                    val currentQuote = quoteRide?.let { ride ->
                        pendingRides.find { it.id == ride.id }?.let { updatedRide ->
                            if (updatedRide.quoteId.isNotBlank()) {
                                allQuotes.find { it.id == updatedRide.quoteId }
                            } else null
                        }
                    }
                    QuoteScreen(
                        ride = quoteRide!!,
                        quote = currentQuote,
                        settings = settings,
                        onCreateQuote = { dist, price, channel ->
                            val quote = com.drawtaxi.app.data.Quote(
                                id = com.drawtaxi.app.data.Quote.createId(quoteRide!!.id),
                                rideId = quoteRide!!.id,
                                departure = quoteRide!!.departure,
                                arrival = quoteRide!!.arrival,
                                distanceKm = dist,
                                price = price,
                                messageChannel = channel
                            )
                            viewModel.sendQuote(quoteRide!!, quote, this@MainActivity)
                            Toast.makeText(this@MainActivity, "Devis envoyé au client", Toast.LENGTH_SHORT).show()
                            showQuoteScreen = false
                            quoteRide = null
                        },
                        onSendQuote = { },
                        onAcceptQuote = { quote ->
                            viewModel.acceptQuote(quote)
                            Toast.makeText(this@MainActivity, "Devis accepté - Course confirmée", Toast.LENGTH_SHORT).show()
                            showQuoteScreen = false
                            quoteRide = null
                        },
                        onRejectQuote = { quote ->
                            viewModel.rejectQuote(quote)
                            val message = settings.rejectionTemplate
                            com.drawtaxi.app.logic.MessageSender.sendMessage(
                                this@MainActivity,
                                quoteRide!!.messageChannel,
                                quoteRide!!.sender,
                                quoteRide!!.clientEmail,
                                message
                            )
                            Toast.makeText(this@MainActivity, "Devis refusé - Course supprimée", Toast.LENGTH_SHORT).show()
                            showQuoteScreen = false
                            quoteRide = null
                        },
                        onBack = {
                            showQuoteScreen = false
                            quoteRide = null
                        }
                    )
                } else if (showCompletionScreen && completionRide != null) {
                    RideCompletionScreen(
                        ride = completionRide!!,
                        settings = settings,
                        onComplete = { updatedRide, sendEmail, sendSms, sendWhatsApp ->
                            viewModel.completeRideWithReceipt(
                                updatedRide,
                                this@MainActivity,
                                sendEmail,
                                sendSms,
                                sendWhatsApp
                            )
                            showCompletionScreen = false
                            completionRide = null
                            Toast.makeText(this@MainActivity, "Course terminée !", Toast.LENGTH_SHORT).show()
                        },
                        onBack = {
                            showCompletionScreen = false
                            completionRide = null
                        }
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
                    Scaffold(
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
                                hasPending = pendingRides.isNotEmpty()
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            if (activeRide != null) {
                                ActiveRideScreen(
                                    ride = activeRide!!,
                                    settings = settings,
                                    brandColor = settings.brandColor,
                                    onBack = { activeRide = null },
                                    onComplete = { completedRide ->
                                        completionRide = completedRide
                                        showCompletionScreen = true
                                        activeRide = null
                                    },
                                    onCancel = {
                                        viewModel.deleteRide(activeRide!!)
                                        activeRide = null
                                        Toast.makeText(this@MainActivity, "Course annulée", Toast.LENGTH_SHORT).show()
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
                                    onShareReceipt = { ride ->
                                        if (isPendingRide) {
                                            viewModel.validateRide(ride.id)
                                            com.drawtaxi.app.logic.ShareUtils.shareReceipt(this@MainActivity, ride, settings)
                                            selectedRide = null
                                        } else {
                                            com.drawtaxi.app.logic.ShareUtils.shareReceipt(this@MainActivity, ride, settings)
                                        }
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
                                when (activeTab) {
                                    "home" -> {
                                        HomeScreen(
                                            validatedRides = validatedRides,
                                            pendingRides = pendingRides,
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
                                        ControlCenterScreen(
                                            pendingRides = pendingRides,
                                            onValidate = { ride ->
                                                activeRide = ride
                                                selectedRide = null
                                            },
                                            onDelete = { viewModel.deleteRide(it) },
                                            onRideClick = { selectedRide = it },
                                            brandColor = settings.brandColor,
                                            onCreateRide = {
                                                isCreatingRide = true
                                                creationText = ""
                                            },
                                            onCheckSms = {
                                                viewModel.scanSmsNow(this@MainActivity)
                                                Toast.makeText(this@MainActivity, "Vérification des SMS en cours...", Toast.LENGTH_SHORT).show()
                                            },
                                            onCreateQuote = { ride ->
                                                quoteRide = ride
                                                showQuoteScreen = true
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
                                            }
                                        )
                                    }
                                    "settings" -> {
                                        when (settingsView) {
                                            "proInfo" -> ProInfoSettings(settings, { viewModel.updateSettings(it) }, { settingsView = "main" })
                                            "branding" -> BrandingSettings(settings, { viewModel.updateSettings(it) }, { settingsView = "main" })
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
                                            "clients" -> ClientDirectoryScreen(
                                                rides = validatedRides + pendingRides,
                                                brandColor = settings.brandColor,
                                                onBack = { settingsView = "main" }
                                            )
                                            "export" -> ExportScreen(
                                                rides = validatedRides,
                                                brandColor = settings.brandColor,
                                                onBack = { settingsView = "main" }
                                            )
                                            else -> {
                                                val context = LocalContext.current
                                                SettingsMain(
                                                    settings = settings,
                                                    onUpdate = { viewModel.updateSettings(it) },
                                                    onNavigate = { settingsView = it },
                                                    onTestNotification = { viewModel.testNotification(context) },
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
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intentState.value = intent
    }
}
