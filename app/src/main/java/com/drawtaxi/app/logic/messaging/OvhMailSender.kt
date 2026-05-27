package com.drawtaxi.app.logic.messaging

import android.content.Context
import android.util.Log
import com.drawtaxi.app.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.mail.*
import javax.mail.internet.*

object OvhMailSender {
    private const val TAG = "OvhMailSender"

    suspend fun sendEmail(
        context: Context,
        settings: AppSettings,
        toEmail: String,
        subject: String,
        body: String,
        attachment: File? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!settings.ovhSmtpEnabled) {
                return@withContext Result.failure(Exception("SMTP OVH non activé"))
            }

            if (settings.ovhSmtpUsername.isBlank() || settings.ovhSmtpPassword.isBlank()) {
                return@withContext Result.failure(Exception("Identifiants OVH non configurés"))
            }

            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.host", settings.ovhSmtpServer)
                put("mail.smtp.port", settings.ovhSmtpPort.toString())
                put("mail.smtp.ssl.trust", settings.ovhSmtpServer)
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout", "10000")
                if (settings.ovhSmtpPort == 465) {
                    put("mail.smtp.ssl.enable", "true")
                } else {
                    put("mail.smtp.starttls.enable", settings.ovhSmtpUseSsl.toString())
                }
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        settings.ovhSmtpUsername,
                        settings.ovhSmtpPassword
                    )
                }
            }).apply {
                debug = false
            }

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(
                    settings.ovhFromEmail.ifBlank { settings.ovhSmtpUsername },
                    settings.ovhFromName
                ))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                setSubject(subject)
                sentDate = Date()

                if (attachment != null && attachment.exists()) {
                    val multipart = MimeMultipart()

                    val textPart = MimeBodyPart()
                    textPart.setContent(body, "text/plain; charset=utf-8")
                    multipart.addBodyPart(textPart)

                    val attachmentPart = MimeBodyPart()
                    attachmentPart.attachFile(attachment)
                    attachmentPart.fileName = attachment.name
                    multipart.addBodyPart(attachmentPart)

                    setContent(multipart)
                } else {
                    setContent(body, "text/plain; charset=utf-8")
                }
            }

            Transport.send(message)
            Log.d(TAG, "Email envoyé avec succès à $toEmail")
            Result.success(Unit)
        } catch (e: AuthenticationFailedException) {
            Log.e(TAG, "Échec authentification SMTP", e)
            Result.failure(Exception("Identifiants OVH incorrects"))
        } catch (e: MessagingException) {
            Log.e(TAG, "Erreur envoi email", e)
            Result.failure(Exception("Erreur lors de l'envoi: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue", e)
            Result.failure(Exception("Erreur: ${e.message}"))
        }
    }

    suspend fun testImapConnection(settings: AppSettings): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (settings.ovhSmtpUsername.isBlank() || settings.ovhSmtpPassword.isBlank()) {
                return@withContext Result.failure(Exception("Identifiants non configurés"))
            }

            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", settings.ovhImapServer)
                put("mail.imaps.port", settings.ovhImapPort.toString())
                put("mail.imaps.connectiontimeout", "10000")
                put("mail.imaps.timeout", "10000")
            }

            val session = Session.getInstance(props)
            val store = session.getStore("imaps")
            store.connect(
                settings.ovhImapServer,
                settings.ovhImapPort,
                settings.ovhSmtpUsername,
                settings.ovhSmtpPassword
            )
            val inbox = store.getFolder(settings.ovhImapFolder)
            inbox.open(Folder.READ_ONLY)
            inbox.close(false)
            store.close()

            Result.success("Connexion IMAP réussie (dossier : ${settings.ovhImapFolder})")
        } catch (e: Exception) {
            Result.failure(Exception("Test IMAP échoué: ${e.message}"))
        }
    }

    suspend fun testConnection(settings: AppSettings): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (settings.ovhSmtpUsername.isBlank() || settings.ovhSmtpPassword.isBlank()) {
                return@withContext Result.failure(Exception("Identifiants non configurés"))
            }

            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.host", settings.ovhSmtpServer)
                put("mail.smtp.port", settings.ovhSmtpPort.toString())
                put("mail.smtp.ssl.trust", settings.ovhSmtpServer)
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout", "10000")
                if (settings.ovhSmtpPort == 465) {
                    put("mail.smtp.ssl.enable", "true")
                } else {
                    put("mail.smtp.starttls.enable", settings.ovhSmtpUseSsl.toString())
                }
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        settings.ovhSmtpUsername,
                        settings.ovhSmtpPassword
                    )
                }
            })

            val transport = session.getTransport("smtp")
            transport.connect(
                settings.ovhSmtpServer,
                settings.ovhSmtpPort,
                settings.ovhSmtpUsername,
                settings.ovhSmtpPassword
            )
            transport.close()

            Result.success("Connexion SMTP réussie")
        } catch (e: Exception) {
            Result.failure(Exception("Test connexion échoué: ${e.message}"))
        }
    }

    /**
     * Envoie un email de confirmation de course.
     * Fonction suspend à appeler depuis un CoroutineScope (ViewModel, Service, etc.).
     */
    suspend fun sendRideConfirmation(
        context: Context,
        settings: AppSettings,
        toEmail: String,
        ride: com.drawtaxi.app.data.RideRequest
    ): Result<Unit> {
        val subject = "Confirmation de votre course - ${settings.companyName}"
        val body = """
            Bonjour,

            Votre course a été confirmée :
            
            Départ : ${ride.departure}
            Destination : ${ride.arrival}
            Date : ${ride.date} à ${ride.time}
            Prix estimé : ${String.format("%.2f", ride.price)} €
            
            Merci de votre confiance !
            
            ${settings.name}
            ${settings.companyName}
        """.trimIndent()

        return sendEmail(context, settings, toEmail, subject, body)
    }

    /**
     * Envoie une facture par email avec pièce jointe.
     * Fonction suspend à appeler depuis un CoroutineScope (ViewModel, Service, etc.).
     */
    suspend fun sendInvoiceByEmail(
        context: Context,
        settings: AppSettings,
        toEmail: String,
        ride: com.drawtaxi.app.data.RideRequest,
        invoiceFile: File
    ): Result<Unit> {
        val subject = "Votre facture - ${settings.companyName}"
        val body = """
            Bonjour,

            Veuillez trouver ci-joint la facture de votre course.
            
            Détail de la course :
            - Départ : ${ride.departure}
            - Destination : ${ride.arrival}
            - Date : ${ride.date}
            - Montant : ${String.format("%.2f", ride.price)} €
            
            Cordialement,
            ${settings.name}
            ${settings.companyName}
        """.trimIndent()

        return sendEmail(context, settings, toEmail, subject, body, invoiceFile)
    }
}
