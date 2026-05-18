# PLAN - NOUVELLES FONCTIONNALITÉS

## 1. GPS avec CoMaps (Intents)

### Analyse
CoMaps ne fournit pas de SDK embarqué mais fonctionne via **Intents Android** et **URI schemes**.

### Avantages par rapport à Osmdroid
- ✅ Plus simple à implémenter (pas de gestion de carte)
- ✅ Navigation complète et optimisée
- ✅ Hors ligne
- ✅ Pas de clé API nécessaire
- ✅ Respect de la vie privée

### Implémentation

#### 1.1 Vérifier si CoMaps est installé
```kotlin
fun isCoMapsInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("app.comaps", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
```

#### 1.2 Ouvrir une position
```kotlin
fun openLocationInCoMaps(context: Context, lat: Double, lon: Double, label: String) {
    val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("app.comaps")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback: Ouvrir dans le navigateur ou Play Store
        openCoMapsDownloadPage(context)
    }
}
```

#### 1.3 Navigation vers une destination
```kotlin
fun navigateWithCoMaps(context: Context, destLat: Double, destLon: Double, destName: String) {
    // CoMaps supporte les intents de navigation
    val uri = Uri.parse("https://omaps.app/route?end=$destLat,$destLon&end_name=$destName")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("app.comaps")
    context.startActivity(intent)
}
```

#### 1.4 Modifications à apporter
**Fichier :** `ActiveRideScreen.kt`
- Remplacer la carte osmdroid par un bouton "Ouvrir dans CoMaps"
- Afficher uniquement les infos essentielles (temps, distance)
- Garder le workflow des étapes (TO_PICKUP → TO_DESTINATION → etc.)

**Fichier :** `RideDetailScreen.kt`
- Ajouter bouton "Voir sur CoMaps" pour visualiser le trajet

---

## 2. Envoi de Mail via OVH (SMTP)

### Paramètres à ajouter dans AppSettings
```kotlin
data class AppSettings(
    // ... existant ...
    
    // Configuration OVH SMTP
    val ovhSmtpEnabled: Boolean = false,
    val ovhSmtpServer: String = "ssl0.ovh.net", // ou pro1.mail.ovh.net
    val ovhSmtpPort: Int = 587, // ou 465 pour SSL
    val ovhSmtpUsername: String = "", // email OVH complet
    val ovhSmtpPassword: String = "", // mot de passe OVH
    val ovhSmtpUseSsl: Boolean = true,
    val ovhFromEmail: String = "", // email expéditeur
    val ovhFromName: String = "DrawTaxi" // nom affiché
)
```

### Écran de configuration OVH
**Nouveau fichier :** `OvhMailSettingsScreen.kt`

Champs à configurer :
- ✅ Activer l'envoi par OVH (toggle)
- Serveur SMTP (défaut: ssl0.ovh.net)
- Port (défaut: 587)
- Nom d'utilisateur (email OVH)
- Mot de passe (masqué)
- Email expéditeur
- Nom affiché
- Bouton "Tester la connexion"

### Implémentation SMTP
**Nouveau fichier :** `OvhMailSender.kt`

```kotlin
object OvhMailSender {
    
    suspend fun sendEmail(
        context: Context,
        settings: AppSettings,
        toEmail: String,
        subject: String,
        body: String,
        attachment: File? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", settings.ovhSmtpUseSsl)
                put("mail.smtp.host", settings.ovhSmtpServer)
                put("mail.smtp.port", settings.ovhSmtpPort.toString())
                put("mail.smtp.ssl.trust", settings.ovhSmtpServer)
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        settings.ovhSmtpUsername,
                        settings.ovhSmtpPassword
                    )
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(settings.ovhFromEmail, settings.ovhFromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                setSubject(subject)
                
                if (attachment != null) {
                    // Créer un message multipart avec pièce jointe
                    val multipart = MimeMultipart()
                    
                    val textPart = MimeBodyPart()
                    textPart.setText(body)
                    multipart.addBodyPart(textPart)
                    
                    val attachmentPart = MimeBodyPart()
                    attachmentPart.attachFile(attachment)
                    multipart.addBodyPart(attachmentPart)
                    
                    setContent(multipart)
                } else {
                    setText(body)
                }
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            Log.e("OvhMailSender", "Erreur envoi mail", e)
            false
        }
    }
}
```

### Dépendance Gradle
```groovy
implementation 'com.sun.mail:android-mail:1.6.7'
implementation 'com.sun.mail:android-activation:1.6.7'
```

### Utilisation dans l'app
- Envoi de factures PDF par email
- Envoi de confirmations de course
- Envoi de devis

---

## 3. Surveillance des Mails OVH (IMAP)

### Principe
Connexion IMAP au serveur OVH pour lire les emails entrants et détecter les demandes de course.

### Paramètres supplémentaires
```kotlin
data class AppSettings(
    // ... existant ...
    
    // Configuration OVH IMAP
    val ovhImapEnabled: Boolean = false,
    val ovhImapServer: String = "ssl0.ovh.net",
    val ovhImapPort: Int = 993,
    val ovhImapCheckInterval: Int = 5, // minutes
    val ovhImapFolder: String = "INBOX" // dossier à surveiller
)
```

### Implémentation IMAP Service
**Nouveau fichier :** `OvhImapService.kt`

```kotlin
class OvhImapService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        return START_STICKY
    }
    
    private fun startMonitoring() {
        serviceScope.launch {
            while (isRunning) {
                checkEmails()
                delay(settings.ovhImapCheckInterval * 60 * 1000L)
            }
        }
    }
    
    private suspend fun checkEmails() {
        try {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", settings.ovhImapServer)
                put("mail.imaps.port", settings.ovhImapPort.toString())
            }
            
            val session = Session.getInstance(props)
            val store = session.getStore("imaps")
            store.connect(settings.ovhImapServer, settings.ovhImapUsername, settings.ovhImapPassword)
            
            val inbox = store.getFolder(settings.ovhImapFolder)
            inbox.open(Folder.READ_WRITE)
            
            val messages = inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
            
            messages.forEach { message ->
                processEmail(message)
                message.setFlag(Flags.Flag.SEEN, true)
            }
            
            inbox.close(false)
            store.close()
        } catch (e: Exception) {
            Log.e("OvhImapService", "Erreur lecture emails", e)
        }
    }
    
    private fun processEmail(message: Message) {
        val from = message.from[0].toString()
        val subject = message.subject ?: ""
        val body = message.content.toString()
        
        // Utiliser l'IA pour parser l'email comme un SMS
        val parsedResult = AiSmsParser.parseEmail(body, settings.aiEnabled)
        
        if (parsedResult.departure.isNotBlank() || parsedResult.arrival.isNotBlank()) {
            // Créer une course
            val ride = parsedResult.toRideRequest(from, System.currentTimeMillis(), settings)
            repository.saveRide(ride)
            
            // Notification
            NotificationHelper.showNewRideNotification(
                this,
                ride.id,
                ride.arrival,
                ride.time
            )
        }
    }
}
```

### Écran de configuration
Ajouter dans `SettingsMain.kt` :
- Toggle "Surveiller les emails OVH"
- Paramètres IMAP (serveur, port)
- Intervalle de vérification
- Bouton "Tester la connexion IMAP"

### Permissions nécessaires
- `INTERNET`
- `FOREGROUND_SERVICE`
- Notification pour le service persistant

---

## 4. Surveillance WhatsApp (Sans API Officielle)

### ⚠️ Analyse technique
WhatsApp ne permet pas la surveillance des messages sans :
1. **WhatsApp Business API** (payant, validation Meta nécessaire)
2. **WhatsApp Web** (QR code, pas d'API officielle)
3. **Accessibility Service** (solution de contournement)

### Solution recommandée : Accessibility Service (limitée)
**⚠️ Limites :**
- Nécessite que l'utilisateur active le service d'accessibilité
- Ne fonctionne que si WhatsApp est ouvert
- Peut être instable avec les mises à jour WhatsApp
- N'est pas approuvé par Google Play (risque de bannissement)

### Alternative recommandée : WhatsApp Web Bridge
**Nouveau fichier :** `WhatsAppWebBridge.kt`

Principe :
1. Scanner le QR code WhatsApp Web une fois
2. Maintenir une connexion websocket
3. Recevoir les messages en temps réel

```kotlin
// Utiliser une bibliothèque comme whatsapp-web.js via WebView
// ou baileys (Kotlin/Java port)
```

**Dépendances possibles :**
- `com.github.openwhatsapp:whatsapp-web-reveng` (non maintenu)
- Solution custom avec WebView et JavaScript injection

### ⚠️ Recommandation finale
**NE PAS implémenter** la surveillance WhatsApp pour les raisons suivantes :
1. Violation des CGU WhatsApp
2. Risque de blocage du compte utilisateur
3. Complexité technique élevée
4. Pas de solution fiable et maintenable

**Alternative proposée :**
Demander aux clients d'utiliser :
- SMS (déjà implémenté)
- Email (nouveau avec OVH)
- Formulaire web (déjà implémenté)

---

## 5. Résumé des modifications

### Fichiers à créer
1. `OvhMailSettingsScreen.kt` - Configuration mail OVH
2. `OvhMailSender.kt` - Envoi SMTP
3. `OvhImapService.kt` - Surveillance IMAP
4. `CoMapsNavigation.kt` - Helper intents CoMaps

### Fichiers à modifier
1. `AppSettings.kt` - Ajouter paramètres OVH
2. `SettingsMain.kt` - Ajouter entrées menu OVH
3. `ActiveRideScreen.kt` - Remplacer carte par intents CoMaps
4. `SettingsManager.kt` - Persister nouveaux paramètres
5. `build.gradle` - Ajouter dépendances JavaMail

### Permissions à ajouter (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

### Dépendances Gradle
```groovy
// JavaMail pour SMTP/IMAP
implementation 'com.sun.mail:android-mail:1.6.7'
implementation 'com.sun.mail:android-activation:1.6.7'

// Cryptage mot de passe (optionnel)
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
```

---

## 6. Ordre d'implémentation recommandé

1. **CoMaps** (plus simple, remplace osmdroid immédiatement)
2. **OVH SMTP** (envoi de mails pour factures)
3. **OVH IMAP** (surveillance mails)
4. **WhatsApp** - NE PAS FAIRE (trop risqué)

---

*Plan créé le : 18/05/2026*
