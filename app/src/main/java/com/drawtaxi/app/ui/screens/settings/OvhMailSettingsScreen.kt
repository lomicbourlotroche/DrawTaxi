package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.drawtaxi.app.ui.components.core.*
import com.drawtaxi.app.ui.theme.*

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.logic.messaging.OvhMailSender
import kotlinx.coroutines.launch

@Composable
fun OvhMailSettingsScreen(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val brandColor = settings.brandColor
    
    // États locaux
    var smtpEnabled by remember { mutableStateOf(settings.ovhSmtpEnabled) }
    var smtpServer by remember { mutableStateOf(settings.ovhSmtpServer) }
    var smtpPort by remember { mutableStateOf(settings.ovhSmtpPort.toString()) }
    var smtpUsername by remember { mutableStateOf(settings.ovhSmtpUsername) }
    var smtpPassword by remember { mutableStateOf(settings.ovhSmtpPassword) }
    var smtpUseSsl by remember { mutableStateOf(settings.ovhSmtpUseSsl) }
    var fromEmail by remember { mutableStateOf(settings.ovhFromEmail) }
    var fromName by remember { mutableStateOf(settings.ovhFromName) }
    
    var imapEnabled by remember { mutableStateOf(settings.ovhImapEnabled) }
    var imapServer by remember { mutableStateOf(settings.ovhImapServer) }
    var imapPort by remember { mutableStateOf(settings.ovhImapPort.toString()) }
    var imapInterval by remember { mutableStateOf(settings.ovhImapCheckInterval.toString()) }
    
    var testStatus by remember { mutableStateOf<TestStatus?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    DrawTaxiScaffold(
        topBar = {
            DrawTaxiTopBar(
                title = { DrawTaxiTopBarTitle("Configuration Email OVH") },
                navigationIcon = {
                    DrawTaxiIconButton(onClick = onBack) {
                        DrawTaxiIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Slate700)
                    }
                },
                backgroundColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Slate50)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section SMTP (Envoi)
            SettingsCard(title = "Envoi d'emails (SMTP)", icon = Icons.Default.Send) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Toggle activation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            DrawTaxiText(
                                text = "Activer l'envoi via OVH",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            DrawTaxiText(
                                text = "Envoyer factures et confirmations",
                                color = Slate500,
                                fontSize = 12.sp
                            )
                        }
                        DrawTaxiSwitch(
                            checked = smtpEnabled,
                            onCheckedChange = { smtpEnabled = it }
                        )
                    }
                    
                    if (smtpEnabled) {
                        DrawTaxiDivider()
                        
                        DrawTaxiTextField(
                            value = smtpServer,
                            onValueChange = { smtpServer = it },
                            label = "Serveur SMTP",
                            placeholder = "ssl0.ovh.net"
                        )
                        
                        DrawTaxiTextField(
                            value = smtpPort,
                            onValueChange = { if (it.all { c -> c.isDigit() }) smtpPort = it },
                            label = "Port",
                            keyboardType = KeyboardType.Number
                        )
                        
                        DrawTaxiTextField(
                            value = smtpUsername,
                            onValueChange = { smtpUsername = it },
                            label = "Nom d'utilisateur (email OVH)",
                            keyboardType = KeyboardType.Email
                        )
                        
                        DrawTaxiTextField(
                            value = smtpPassword,
                            onValueChange = { smtpPassword = it },
                            label = "Mot de passe",
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardType = KeyboardType.Password,
                            trailingIcon = {
                                DrawTaxiIconButton(onClick = { showPassword = !showPassword }, size = 32.dp) {
                                    DrawTaxiIcon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Masquer" else "Afficher",
                                        tint = Slate400
                                    )
                                }
                            }
                        )
                        
                        DrawTaxiTextField(
                            value = fromEmail,
                            onValueChange = { fromEmail = it },
                            label = "Email expéditeur",
                            keyboardType = KeyboardType.Email
                        )
                        
                        DrawTaxiTextField(
                            value = fromName,
                            onValueChange = { fromName = it },
                            label = "Nom affiché"
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DrawTaxiText("Utiliser SSL/TLS", fontWeight = FontWeight.Medium)
                            DrawTaxiSwitch(
                                checked = smtpUseSsl,
                                onCheckedChange = { smtpUseSsl = it }
                            )
                        }
                    }
                }
            }
            
            // Section IMAP (Réception)
            SettingsCard(title = "Réception d'emails (IMAP)", icon = Icons.Default.MarkEmailRead) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            DrawTaxiText(
                                text = "Surveiller les emails entrants",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            DrawTaxiText(
                                text = "Créer automatiquement des courses",
                                color = Slate500,
                                fontSize = 12.sp
                            )
                        }
                        DrawTaxiSwitch(
                            checked = imapEnabled,
                            onCheckedChange = { imapEnabled = it }
                        )
                    }
                    
                    if (imapEnabled) {
                        DrawTaxiDivider()
                        
                        DrawTaxiTextField(
                            value = imapServer,
                            onValueChange = { imapServer = it },
                            label = "Serveur IMAP",
                            placeholder = "ssl0.ovh.net"
                        )
                        
                        DrawTaxiTextField(
                            value = imapPort,
                            onValueChange = { if (it.all { c -> c.isDigit() }) imapPort = it },
                            label = "Port IMAP",
                            keyboardType = KeyboardType.Number
                        )
                        
                        DrawTaxiTextField(
                            value = imapInterval,
                            onValueChange = { if (it.all { c -> c.isDigit() }) imapInterval = it },
                            label = "Intervalle de vérification (minutes)",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
            }
            
            // Test de connexion
            testStatus?.let { status ->
                DrawTaxiCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = when (status) {
                        is TestStatus.Success -> Color(0xFFD1FAE5)
                        is TestStatus.Error -> Color(0xFFFFE4E6)
                        is TestStatus.Loading -> Slate100
                    },
                    elevation = 0.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (status) {
                            is TestStatus.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = brandColor
                                )
                            }
                            is TestStatus.Success -> {
                                DrawTaxiIcon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981)
                                )
                            }
                            is TestStatus.Error -> {
                                DrawTaxiIcon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFF43F5E)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        DrawTaxiText(
                            text = status.message,
                            color = when (status) {
                                is TestStatus.Success -> Color(0xFF065F46)
                                is TestStatus.Error -> Color(0xFF9F1239)
                                is TestStatus.Loading -> Slate600
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Boutons d'action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DrawTaxiOutlinedButton(
                    onClick = {
                        scope.launch {
                            testStatus = TestStatus.Loading("Test en cours...")
                            val testSettings = settings.copy(
                                ovhSmtpServer = smtpServer,
                                ovhSmtpPort = smtpPort.toIntOrNull() ?: 587,
                                ovhSmtpUsername = smtpUsername,
                                ovhSmtpPassword = smtpPassword,
                                ovhSmtpUseSsl = smtpUseSsl
                            )
                            val result = OvhMailSender.testConnection(testSettings)
                            testStatus = if (result.isSuccess) {
                                TestStatus.Success(result.getOrDefault("Connecté !"))
                            } else {
                                TestStatus.Error(result.exceptionOrNull()?.message ?: "Erreur inconnue")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = smtpEnabled && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()
                ) {
                    DrawTaxiIcon(Icons.Default.NetworkCheck, contentDescription = null, tint = if (smtpEnabled && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()) brandColor else brandColor.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.width(8.dp))
                    DrawTaxiText("Tester", fontWeight = FontWeight.Bold, color = if (smtpEnabled && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()) brandColor else brandColor.copy(alpha = 0.4f))
                }
                
                DrawTaxiSolidButton(
                    onClick = {
                        onUpdate(
                            settings.copy(
                                ovhSmtpEnabled = smtpEnabled,
                                ovhSmtpServer = smtpServer,
                                ovhSmtpPort = smtpPort.toIntOrNull() ?: 587,
                                ovhSmtpUsername = smtpUsername,
                                ovhSmtpPassword = smtpPassword,
                                ovhSmtpUseSsl = smtpUseSsl,
                                ovhFromEmail = fromEmail,
                                ovhFromName = fromName,
                                ovhImapEnabled = imapEnabled,
                                ovhImapServer = imapServer,
                                ovhImapPort = imapPort.toIntOrNull() ?: 993,
                                ovhImapCheckInterval = imapInterval.toIntOrNull() ?: 5
                            )
                        )
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = brandColor
                ) {
                    DrawTaxiIcon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    DrawTaxiText("Sauvegarder", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    DrawTaxiCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawTaxiIcon(
                imageVector = icon,
                contentDescription = null,
                tint = drawTaxiColors().primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            DrawTaxiText(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

private sealed class TestStatus(val message: String) {
    class Loading(message: String) : TestStatus(message)
    class Success(message: String) : TestStatus(message)
    class Error(message: String) : TestStatus(message)
}

@Preview(showBackground = true)
@Composable
fun OvhMailSettingsScreenPreview() {
    val sampleSettings = AppSettings()
    DrawTaxiTheme {
        OvhMailSettingsScreen(
            settings = sampleSettings,
            onUpdate = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsCardPreview() {
    DrawTaxiTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            SettingsCard(
                title = "Configuration SMTP",
                icon = Icons.Default.Send,
                content = {
                    DrawTaxiText("Configuration du serveur d'envoi d'emails.")
                }
            )
        }
    }
}


