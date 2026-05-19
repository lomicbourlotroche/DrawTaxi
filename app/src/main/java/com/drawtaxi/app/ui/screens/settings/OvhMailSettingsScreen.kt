package com.drawtaxi.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.drawtaxi.app.data.AppSettings
import com.drawtaxi.app.logic.messaging.OvhMailSender
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration Email OVH", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section SMTP (Envoi)
            SettingsCard(title = "Envoi d'emails (SMTP)", icon = Icons.Default.Send) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Toggle activation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Activer l'envoi via OVH",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Envoyer factures et confirmations",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = smtpEnabled,
                            onCheckedChange = { smtpEnabled = it }
                        )
                    }
                    
                    if (smtpEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        OutlinedTextField(
                            value = smtpServer,
                            onValueChange = { smtpServer = it },
                            label = { Text("Serveur SMTP") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = smtpPort,
                            onValueChange = { if (it.all { c -> c.isDigit() }) smtpPort = it },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = smtpUsername,
                            onValueChange = { smtpUsername = it },
                            label = { Text("Nom d'utilisateur (email OVH)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = smtpPassword,
                            onValueChange = { smtpPassword = it },
                            label = { Text("Mot de passe") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Masquer" else "Afficher"
                                    )
                                }
                            }
                        )
                        
                        OutlinedTextField(
                            value = fromEmail,
                            onValueChange = { fromEmail = it },
                            label = { Text("Email expéditeur") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = fromName,
                            onValueChange = { fromName = it },
                            label = { Text("Nom affiché") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Utiliser SSL/TLS")
                            Switch(
                                checked = smtpUseSsl,
                                onCheckedChange = { smtpUseSsl = it }
                            )
                        }
                    }
                }
            }
            
            // Section IMAP (Réception)
            SettingsCard(title = "Réception d'emails (IMAP)", icon = Icons.Default.MarkEmailRead) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Surveiller les emails entrants",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Créer automatiquement des courses",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = imapEnabled,
                            onCheckedChange = { imapEnabled = it }
                        )
                    }
                    
                    if (imapEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        OutlinedTextField(
                            value = imapServer,
                            onValueChange = { imapServer = it },
                            label = { Text("Serveur IMAP") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = imapPort,
                            onValueChange = { if (it.all { c -> c.isDigit() }) imapPort = it },
                            label = { Text("Port IMAP") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = imapInterval,
                            onValueChange = { if (it.all { c -> c.isDigit() }) imapInterval = it },
                            label = { Text("Intervalle de vérification (minutes)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
            
            // Test de connexion
            testStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (status) {
                            is TestStatus.Success -> Color(0xFFD1FAE5)
                            is TestStatus.Error -> Color(0xFFFFE4E6)
                            is TestStatus.Loading -> Color(0xFFF3F4F6)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (status) {
                            is TestStatus.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is TestStatus.Success -> {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981)
                                )
                            }
                            is TestStatus.Error -> {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFF43F5E)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = status.message,
                            color = when (status) {
                                is TestStatus.Success -> Color(0xFF065F46)
                                is TestStatus.Error -> Color(0xFF9F1239)
                                is TestStatus.Loading -> Color.Gray
                            }
                        )
                    }
                }
            }
            
            // Boutons d'action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
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
                    shape = RoundedCornerShape(12.dp),
                    enabled = smtpEnabled && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tester")
                }
                
                Button(
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sauvegarder")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

private sealed class TestStatus(val message: String) {
    class Loading(message: String) : TestStatus(message)
    class Success(message: String) : TestStatus(message)
    class Error(message: String) : TestStatus(message)
}
