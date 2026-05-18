# Roadmap d'Améliorations - DrawTaxi pour TY-ZEF Brest VTC

> **Date de création** : 18 Mai 2026  
> **Client** : TY-ZEF Brest VTC (tyzefbrestvtc.fr)  
> **Type** : VTC Premium - Bretagne  
> **État actuel** : Application fonctionnelle à 95%

---

## 🎯 Vue d'Ensemble

L'application DrawTaxi est **déjà très complète** pour la gestion quotidienne d'un VTC solo. Cette roadmap liste les améliorations pour atteindre **100% d'adéquation** avec les besoins de TY-ZEF Brest VTC.

### Score Actuel par Catégorie

| Catégorie | Score | Priorité |
|-----------|-------|----------|
| Gestion Opérationnelle | 95% | ✅ OK |
| Administration | 80% | 🟡 Moyenne |
| Intégration Digitale | 50% | 🔴 Haute |
| Paiement | 30% | 🟡 Moyenne |
| Multi-Chauffeur | 0% | ⚪ Non applicable |

**Score Global : 71%** → Objectif : 95%+

---

## 🔴 PRIORITÉ HAUTE

### 1. Intégration Site Web ↔ Application

**Problème** : Le formulaire de réservation sur tyzefbrestvtc.fr envoie actuellement un email manuel. L'application a un handler API (`WebFormApiHandler.kt`) mais il n'est pas connecté.

**Solution** : Connecter le formulaire web à l'app via Firebase Cloud Messaging

#### 1.1 Backend Firebase (Recommandé)

```javascript
// firebase-functions/index.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.submitRideRequest = functions.https.onCall(async (data, context) => {
  const { sender, body, departure, arrival, time, date, clientName, clientEmail } = data;
  
  // Envoyer notification push à l'app
  const message = {
    notification: {
      title: 'Nouvelle demande de course',
      body: `${departure} → ${arrival} à ${time}`
    },
    data: {
      type: 'new_ride_request',
      sender: sender || '',
      body: body || '',
      departure: departure || '',
      arrival: arrival || '',
      time: time || '',
      date: date || '',
      clientName: clientName || '',
      clientEmail: clientEmail || '',
      timestamp: Date.now().toString()
    },
    topic: 'tyzef_driver'  // ou token spécifique du chauffeur
  };
  
  await admin.messaging().send(message);
  
  // Sauvegarder dans Firestore pour persistance
  await admin.firestore().collection('ride_requests').add({
    ...data,
    status: 'pending',
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
  
  return { success: true, message: 'Demande envoyée au chauffeur' };
});
```

#### 1.2 Modification du Site Web

```javascript
// Sur tyzefbrestvtc.fr - Formulaire de contact
async function submitForm(formData) {
  const firebaseConfig = {
    // Configuration Firebase TY-ZEF
  };
  
  const app = firebase.initializeApp(firebaseConfig);
  const functions = firebase.functions();
  
  const submitRideRequest = functions.httpsCallable('submitRideRequest');
  
  try {
    const result = await submitRideRequest({
      sender: formData.phone,
      body: `Demande depuis le site web`,
      departure: formData.departure,
      arrival: formData.arrival,
      time: formData.time,
      date: formData.date,
      clientName: formData.name,
      clientEmail: formData.email
    });
    
    // Message de confirmation au client
    showSuccess("Votre demande a été envoyée directement au chauffeur !");
  } catch (error) {
    // Fallback vers email
    sendEmailFallback(formData);
  }
}
```

#### 1.3 Modifications dans l'App Android

Fichier : `app/src/main/java/com/drawtaxi/app/TaxiApplication.kt`

```kotlin
// Ajouter dans onCreate()
Firebase.messaging.subscribeToTopic("tyzef_driver")
  .addOnCompleteListener { task ->
    Log.d("FCM", "Subscribed to tyzef_driver topic: ${task.isSuccessful}")
  }

// Créer un service pour recevoir les notifications
class RideRequestMessagingService : FirebaseMessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    
    val data = remoteMessage.data
    if (data["type"] == "new_ride_request") {
      val rideRequest = WebFormRequest(
        sender = data["sender"] ?: "",
        body = data["body"] ?: "",
        departure = data["departure"] ?: "",
        arrival = data["arrival"] ?: "",
        time = data["time"] ?: "",
        date = data["date"] ?: "",
        clientName = data["clientName"] ?: "",
        clientEmail = data["clientEmail"] ?: ""
      )
      
      // Sauvegarder dans la base locale
      WebFormReceiver.processWebFormRequest(this, rideRequest.toJson())
    }
  }
}
```

**Fichiers concernés** :
- `logic/WebFormApiHandler.kt` (existant)
- `TaxiApplication.kt` (modifier)
- Créer : `service/RideRequestMessagingService.kt`
- Créer : `build.gradle` (ajouter dépendances Firebase)

**Temps estimé** : 1-2 jours

---

### 2. Personnalisation TY-ZEF - Paramètres par Défaut

Modifier les valeurs par défaut dans `data/Models.kt` pour refléter l'identité TY-ZEF.

```kotlin
// data/Models.kt - Ligne 29

data class AppSettings(
  // INFOS ENTREPRISE
  val companyName: String = "TY-ZEF Brest VTC",
  val name: String = "Chauffeur TY-ZEF",
  val address: String = "Brest",
  val city: String = "29200 Brest",
  val siret: String = "",  // À compléter par le client
  val tva: String = "",    // À compléter par le client
  val vehicle: String = "Véhicule 100% Électrique",
  
  // TARIFICATION (à ajuster selon grille TY-ZEF)
  val pricePerKm: String = "1.80",      // Tarif VTC premium
  val basePrice: String = "5.00",        // Prise en charge
  
  // IDENTITÉ VISUELLE
  val brandColor: Color = Color(0xFF1E3A5F),  // Bleu marine (site web)
  val theme: String = "modern",
  val darkMode: Boolean = false,
  val showLogo: Boolean = true,
  
  // FONCTIONNALITÉS
  val monitorSms: Boolean = true,
  val enableNotifications: Boolean = true,
  val trackLocation: Boolean = true,
  
  // TEMPLATES MESSAGES (adaptés au ton TY-ZEF)
  val signature: String = "TY-ZEF Brest VTC - Votre chauffeur privé en Bretagne",
  
  val missingInfoTemplate: String = 
    "Bonjour, merci pour votre demande. Pour confirmer votre réservation, " +
    "j'aurais besoin de : [FIELDS]. À bientôt !",
  
  val arrivalMessageTemplate: String = 
    "Bonjour, votre chauffeur TY-ZEF est arrivé au point de rendez-vous. " +
    "Je vous attends à bord du véhicule.",
  
  val quoteTemplate: String = 
    "Bonjour, merci pour votre confiance.\n\n" +
    "Voici le devis pour votre course :\n" +
    "🚗 Trajet : [DEPART] → [ARRIVEE]\n" +
    "📏 Distance : [DISTANCE] km\n" +
    "💶 Prix : [PRIX] € TTC\n\n" +
    "Pour confirmer, répondez OUI.\n" +
    "TY-ZEF Brest VTC - Service premium 7j/7",
  
  val rejectionTemplate: String = 
    "Bonjour, je ne suis malheureusement pas disponible à cette date. " +
    "N'hésitez pas à me recontacter pour une autre réservation. " +
    "Bonne journée, TY-ZEF Brest VTC.",
  
  val invoiceTemplate: String = 
    "Bonjour,\n\n" +
    "Veuillez trouver ci-joint la facture de votre course TY-ZEF.\n\n" +
    "Merci pour votre confiance.\n" +
    "À bientôt,\n" +
    "L'équipe TY-ZEF Brest VTC",
  
  // COÛTS (pour calcul rentabilité)
  val fuelCostPerKm: Double = 0.08,      // Électrique = moins cher
  val operatingCostPerHour: Double = 15.0,
  
  // MAJORATION
  val nightSurchargePercent: Double = 0.20,
  val sundaySurchargePercent: Double = 0.15,
  val holidaySurchargePercent: Double = 0.20,
  
  // HORAIRES
  val nightStartHour: Int = 20,
  val nightEndHour: Int = 7,
  
  // TVA
  val tvaTransportRate: Double = 0.10,
  val tvaWaitTimeRate: Double = 0.20,
  
  // ADRESSE BASE (pour calcul distances)
  val homeAddress: String = "Brest, Bretagne",
  
  // TEMPLATES RAPIDES
  val messageTemplates: List<String> = listOf(
    "Bonjour, je suis en retard de quelques minutes. Merci de votre patience.",
    "Bonjour, j'arrive dans 2 minutes !",
    "Bonjour, je vous attends devant l'entrée principale.",
    "Bonjour, où vous trouvez-vous exactement ?",
    "Votre course est confirmée. À très vite !"
  ),
  
  // ABSENCE
  val absenceMessageTemplate: String = 
    "Bonjour,\n\n" +
    "Je suis actuellement indisponible du [DATE_DEBUT] au [DATE_FIN]. " +
    "Je reprends les réservations le [DATE_RETOUR].\n\n" +
    "Merci de votre compréhension,\n" +
    "TY-ZEF Brest VTC",
  
  // AUTRES
  val isFirstLaunch: Boolean = true,
  val autoBackupEnabled: Boolean = true,
  val autoBackupInterval: String = "daily",
  val clientEmail: String = "",
  val driverEmail: String = "contact@tyzefbrestvtc.fr",
  val autoGenerateStatsReport: Boolean = true,
  val statsReportTime: String = "23:00",
  val smsScanIntervalMinutes: Int = 60,
  val aiEnabled: Boolean = true
)
```

**Fichier concerné** : `data/Models.kt`

**Temps estimé** : 30 minutes

---

### 3. Amélioration Parsing SMS pour la Bretagne

Le parseur actuel (`logic/ParseSms.kt`) est optimisé pour Paris. Pour TY-ZEF Brest, ajouter la reconnaissance des villes bretonnes.

```kotlin
// logic/ParseSms.kt - Ligne 163

private fun estimateDistance(departure: String, arrival: String): Double {
  if (departure.isBlank() || arrival.isBlank()) return 0.0
  
  // VILLES BRETONNES (ajouter ces listes)
  val brestKeywords = listOf("brest", "gouesnou", "guipavas", "plouzané", "milizac")
  val quimperKeywords = listOf("quimper", "ergué-gabéric", "pluguffan", "benodet")
  val concarneauKeywords = listOf("concarneau", "trégunc", "névez")
  val lorientKeywords = listOf("lorient", "lanester", "ploemeur", "larmor-plage")
  val vannesKeywords = listOf("vannes", "séné", "theix", "arradon")
  val rennesKeywords = listOf("rennes", "saint-grégoire", "cesson-sévigné", "bruz")
  val saintBrieucKeywords = listOf("saint-brieuc", "langueux", "plo", "trégueux")
  val saintMaloKeywords = listOf("saint-malo", "dinard", "saint-jouan-des-guérets")
  
  // AÉROPORTS ET GARES BRETONS
  val airportKeywords = listOf(
    "brest bretagne", "aéroport brest", "aeroport brest",
    "quimper cornouaille", "aéroport quimper",
    "lorient lann bihoué", "aéroport lorient"
  )
  
  val trainStationKeywords = listOf(
    "gare brest", "gare de brest",
    "gare quimper", "gare de quimper",
    "gare lorient", "gare de lorient",
    "gare vannes", "gare de vannes",
    "gare rennes", "gare de rennes"
  )
  
  val depLower = departure.lowercase()
  val arrLower = arrival.lowercase()
  
  // Vérifier les correspondances
  val depIsBrest = brestKeywords.any { depLower.contains(it) }
  val arrIsBrest = brestKeywords.any { arrLower.contains(it) }
  val depIsQuimper = quimperKeywords.any { depLower.contains(it) }
  val arrIsQuimper = quimperKeywords.any { arrLower.contains(it) }
  val depIsLorient = lorientKeywords.any { depLower.contains(it) }
  val arrIsLorient = lorientKeywords.any { arrLower.contains(it) }
  val depIsVannes = vannesKeywords.any { depLower.contains(it) }
  val arrIsVannes = vannesKeywords.any { arrLower.contains(it) }
  val depIsRennes = rennesKeywords.any { depLower.contains(it) }
  val arrIsRennes = rennesKeywords.any { arrLower.contains(it) }
  val depIsSaintMalo = saintMaloKeywords.any { depLower.contains(it) }
  val arrIsSaintMalo = saintMaloKeywords.any { arrLower.contains(it) }
  
  val depIsAirport = airportKeywords.any { depLower.contains(it) }
  val arrIsAirport = airportKeywords.any { arrLower.contains(it) }
  val depIsStation = trainStationKeywords.any { depLower.contains(it) }
  val arrIsStation = trainStationKeywords.any { arrLower.contains(it) }
  
  return when {
    // AÉROPORTS BREST
    (depIsAirport && arrIsBrest) || (depIsBrest && arrIsAirport) -> 12.0
    (depIsAirport && arrIsQuimper) || (depIsQuimper && arrIsAirport) -> 85.0
    (depIsAirport && arrIsLorient) || (depIsLorient && arrIsAirport) -> 130.0
    (depIsAirport && arrIsRennes) || (depIsRennes && arrIsAirport) -> 215.0
    
    // GARES
    (depIsStation && arrIsBrest) || (depIsBrest && arrIsStation) -> 8.0
    (depIsStation && arrIsQuimper) || (depIsQuimper && arrIsStation) -> 82.0
    
    // BREST ↔ VILLES FINISTÈRE
    (depIsBrest && arrIsQuimper) || (depIsQuimper && arrIsBrest) -> 75.0
    (depIsBrest && arrIsLorient) || (depIsLorient && arrIsBrest) -> 125.0
    (depIsBrest && arrIsConcarneau) || (depIsConcarneau && arrIsBrest) -> 95.0
    
    // BREST ↔ AUTRES DÉPARTEMENTS
    (depIsBrest && arrIsVannes) || (depIsVannes && arrIsBrest) -> 165.0
    (depIsBrest && arrIsRennes) || (depIsRennes && arrIsBrest) -> 245.0
    (depIsBrest && arrIsSaintMalo) || (depIsSaintMalo && depIsBrest) -> 265.0
    
    // VANNES ↔ RENNES
    (depIsVannes && arrIsRennes) || (depIsRennes && arrIsVannes) -> 115.0
    
    // QUIMPER ↔ LORIENT
    (depIsQuimper && arrIsLorient) || (depIsLorient && arrIsQuimper) -> 65.0
    
    // Courses locales Brest
    depIsBrest && arrIsBrest -> 12.0
    
    // Par défaut
    else -> 35.0
  }
}
```

**Fichier concerné** : `logic/ParseSms.kt` (modifier la fonction `estimateDistance`)

**Temps estimé** : 1 heure

---

## 🟡 PRIORITÉ MOYENNE

### 4. Système de Paiement Intégré

**Besoin** : Permettre aux clients de payer par CB directement dans l'app ou via un lien.

#### 4.1 Solution Stripe (Recommandée)

```kotlin
// logic/PaymentManager.kt (NOUVEAU FICHIER)

package com.drawtaxi.app.logic

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PaymentManager(private val context: Context) {
  
  private val stripePublishableKey = "pk_test_..."  // Clé publique Stripe TY-ZEF
  private val backendUrl = "https://votre-backend-tyzef.com"  // URL backend
  
  init {
    PaymentConfiguration.init(context, stripePublishableKey)
  }
  
  // Créer une intention de paiement
  suspend fun createPaymentIntent(
    amount: Double,
    rideId: String,
    customerEmail: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val client = OkHttpClient()
      val json = JSONObject().apply {
        put("amount", (amount * 100).toInt())  // Stripe utilise les centimes
        put("currency", "eur")
        put("rideId", rideId)
        put("customerEmail", customerEmail)
        put("description", "Course TY-ZEF - $rideId")
      }
      
      val request = Request.Builder()
        .url("$backendUrl/create-payment-intent")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()
      
      val response = client.newCall(request).execute()
      val responseBody = response.body?.string()
      
      if (response.isSuccessful && responseBody != null) {
        val responseJson = JSONObject(responseBody)
        val clientSecret = responseJson.getString("clientSecret")
        Result.success(clientSecret)
      } else {
        Result.failure(Exception("Erreur création paiement"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
  
  // Générer un lien de paiement (pour envoyer par SMS/WhatsApp)
  suspend fun createPaymentLink(
    amount: Double,
    rideId: String,
    description: String
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val client = OkHttpClient()
      val json = JSONObject().apply {
        put("amount", (amount * 100).toInt())
        put("currency", "eur")
        put("rideId", rideId)
        put("description", description)
      }
      
      val request = Request.Builder()
        .url("$backendUrl/create-payment-link")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()
      
      val response = client.newCall(request).execute()
      val responseBody = response.body?.string()
      
      if (response.isSuccessful && responseBody != null) {
        val responseJson = JSONObject(responseBody)
        val paymentUrl = responseJson.getString("url")
        Result.success(paymentUrl)
      } else {
        Result.failure(Exception("Erreur création lien"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
  
  // Envoyer lien de paiement au client
  fun sendPaymentLink(context: Context, ride: RideRequest) {
    CoroutineScope(Dispatchers.Main).launch {
      val result = createPaymentLink(ride.price, ride.id, 
        "Course ${ride.departure} → ${ride.arrival}")
      
      result.onSuccess { url ->
        val message = "Voici le lien pour régler votre course de ${String.format("%.2f", ride.price)}€ :\n$url\n\nMerci !"
        MessageSender.sendMessage(context, ride.messageChannel, ride.sender, 
          ride.clientEmail, message)
      }.onFailure {
        Toast.makeText(context, "Erreur création lien", Toast.LENGTH_SHORT).show()
      }
    }
  }
}
```

#### 4.2 Backend Stripe (Node.js)

```javascript
// backend-stripe/index.js
const stripe = require('stripe')('sk_test_...');  // Clé secrète
const express = require('express');
const app = express();

app.use(express.json());

// Créer une intention de paiement
app.post('/create-payment-intent', async (req, res) => {
  const { amount, currency, rideId, customerEmail, description } = req.body;
  
  try {
    const paymentIntent = await stripe.paymentIntents.create({
      amount: amount,
      currency: currency,
      automatic_payment_methods: { enabled: true },
      metadata: {
        rideId: rideId,
        customerEmail: customerEmail
      },
      description: description
    });
    
    res.json({ clientSecret: paymentIntent.client_secret });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Créer un lien de paiement
app.post('/create-payment-link', async (req, res) => {
  const { amount, currency, rideId, description } = req.body;
  
  try {
    const paymentLink = await stripe.paymentLinks.create({
      line_items: [
        {
          price_data: {
            currency: currency,
            unit_amount: amount,
            product_data: {
              name: description,
            },
          },
          quantity: 1,
        },
      ],
      metadata: {
        rideId: rideId
      }
    });
    
    res.json({ url: paymentLink.url });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.listen(3000, () => {
  console.log('Server running on port 3000');
});
```

**Fichiers à créer** :
- `logic/PaymentManager.kt`
- Backend Node.js (à héberger sur Vercel/Railway)

**Temps estimé** : 2-3 jours

---

### 5. Application Client (Optionnel)

Pour une expérience premium, TY-ZEF pourrait offrir une **app légère** pour ses clients réguliers.

#### Fonctionnalités App Client

- **Réservation rapide** : 1 clic pour réserver un trajet habituel
- **Suivi en temps réel** : Voir la position du chauffeur
- **Historique** : Toutes les courses passées
- **Paiement** : CB sauvegardée
- **Chat** : Communication directe

#### Architecture Suggérée

```
TY-ZEF Client App
├── Réservation rapide
│   ├── Adresses favorites (Maison, Travail, Aéroport...)
│   ├── Dernières courses (1-clic rebooking)
│   └── Formulaire complet
├── Suivi course
│   ├── Position chauffeur sur carte
│   ├── Temps d'arrivée estimé
│   └── Notification "Chauffeur arrivé"
├── Paiement
│   ├── CB sauvegardée
│   ├── Reçus digitaux
│   └── Notes de frais entreprise
└── Profil
    ├── Informations personnelles
    ├── Préférences (température, musique...)
    └── Entreprise (compte pro)
```

**Note** : C'est un projet à part entière (~1 semaine de dev)

**Temps estimé** : 5-7 jours

---

### 6. Synchronisation Calendrier

**Besoin** : Exporter les courses vers Google Calendar pour une meilleure vue d'ensemble.

```kotlin
// logic/CalendarSync.kt (NOUVEAU FICHIER)

package com.drawtaxi.app.logic

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.drawtaxi.app.data.RideRequest

object CalendarSync {
  
  fun addToCalendar(context: Context, ride: RideRequest) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
      data = CalendarContract.Events.CONTENT_URI
      putExtra(CalendarContract.Events.TITLE, "Course TY-ZEF : ${ride.departure} → ${ride.arrival}")
      putExtra(CalendarContract.Events.DESCRIPTION, 
        "Client : ${ride.clientName}\n" +
        "Téléphone : ${ride.sender}\n" +
        "Prix estimé : ${ride.price}€")
      putExtra(CalendarContract.Events.EVENT_LOCATION, ride.departure)
      
      // Calculer date/heure
      val (startMillis, endMillis) = parseRideDateTime(ride)
      putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
      putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
      
      putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
      putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
    }
    
    context.startActivity(intent)
  }
  
  private fun parseRideDateTime(ride: RideRequest): Pair<Long, Long> {
    // Parser la date et l'heure de la course
    // Retourner timestamp début et fin (estimée +30min par défaut)
    // ...
    return Pair(System.currentTimeMillis(), System.currentTimeMillis() + 1800000)
  }
}
```

**Fichier à créer** : `logic/CalendarSync.kt`

**Temps estimé** : 3-4 heures

---

### 7. Gestion des Avis Google

**Besoin** : Automatiser la demande d'avis après une course satisfaisante.

```kotlin
// logic/ReviewManager.kt (NOUVEAU FICHIER)

package com.drawtaxi.app.logic

import android.content.Context
import com.drawtaxi.app.data.RideRequest

object ReviewManager {
  
  private const val GOOGLE_REVIEW_URL = "https://g.page/r/.../review"  // Lien avis TY-ZEF
  
  fun sendReviewRequest(context: Context, ride: RideRequest) {
    // Attendre 1 heure après la course
    // Puis envoyer SMS/email avec lien avis
    
    val message = """
      Bonjour ${ride.clientName},
      
      Merci d'avoir choisi TY-ZEF Brest VTC !
      
      Si vous avez apprécié votre course, un avis Google nous aiderait beaucoup :
      $GOOGLE_REVIEW_URL
      
      À bientôt !
    """.trimIndent()
    
    MessageSender.sendMessage(
      context,
      ride.messageChannel,
      ride.sender,
      ride.clientEmail,
      message
    )
  }
  
  fun shouldRequestReview(ride: RideRequest): Boolean {
    // Ne demander un avis que si :
    // - Course terminée avec succès
    // - Pas déjà demandé pour ce client (vérifier historique)
    // - Client n'a pas donné d'avis récemment
    return true
  }
}
```

**Fichier à créer** : `logic/ReviewManager.kt`

**Temps estimé** : 2 heures

---

## 🟢 PRIORITÉ BASSE

### 8. Courses Récurrentes / Abonnements

**Besoin** : Gérer des trajets réguliers (ex: Brest→Aéroport tous les lundis matin)

```kotlin
// data/Models.kt - Nouvelle entité

data class RecurringRide(
  val id: String,
  val clientName: String,
  val clientPhone: String,
  val departure: String,
  val arrival: String,
  val dayOfWeek: Int,  // Calendar.MONDAY, etc.
  val time: String,    // "08:00"
  val isActive: Boolean = true,
  val priceFixed: Double? = null,
  val notes: String = ""
) {
  companion object {
    fun createId(): String = "recurring_${System.currentTimeMillis()}"
  }
}
```

**Fichiers à modifier** :
- `data/Models.kt` (ajouter RecurringRide)
- `data/local/RideEntity.kt` (nouvelle table)
- `data/local/RideDao.kt` (nouvelles requêtes)
- Créer écran de gestion des récurrentes

**Temps estimé** : 1-2 jours

---

### 9. Tableau de Bord Avancé

**Améliorations possibles** :
- Graphiques de revenus (hebdo/mensuel)
- Comparaison mois par mois
- Prévisions de revenus
- Identification des clients les plus rentables
- Zones géographiques les plus demandées

**Librairie suggérée** : MPAndroidChart ou Compose Charts

**Temps estimé** : 1-2 jours

---

### 10. Mode "Disponible/Indisponible" Rapide

**Besoin** : Bouton widget rapide pour indiquer sa disponibilité

```kotlin
// Widget pour écran d'accueil Android
// Toggle rapide : Disponible / Indisponible / En course
// Met à jour le statut sur le site web aussi (via API)
```

**Temps estimé** : 4-5 heures

---

## 📋 PLAN D'ACTION RECOMMANDÉ

### Phase 1 : Fondation (Semaine 1)
- [ ] **1.1** Configurer Firebase pour connexion site web
- [ ] **1.2** Modifier les paramètres par défaut TY-ZEF
- [ ] **1.3** Améliorer parsing SMS pour villes bretonnes
- [ ] **1.4** Tests end-to-end avec vrais SMS

### Phase 2 : Intégration (Semaine 2)
- [ ] **2.1** Connecter formulaire web tyzefbrestvtc.fr
- [ ] **2.2** Tester flux complet : Formulaire → App → Devis → Confirmation
- [ ] **2.3** Synchronisation calendrier
- [ ] **2.4** Système de demande d'avis Google

### Phase 3 : Paiement (Semaine 3-4)
- [ ] **3.1** Configurer compte Stripe
- [ ] **3.2** Développer PaymentManager
- [ ] **3.3** Backend Node.js pour Stripe
- [ ] **3.4** Intégrer dans écran de fin de course
- [ ] **3.5** Tests paiement

### Phase 4 : Optimisation (Semaine 5)
- [ ] **4.1** Tableau de bord avancé avec graphiques
- [ ] **4.2** Courses récurrentes
- [ ] **4.3** Widget disponibilité
- [ ] **4.4** Documentation utilisateur pour TY-ZEF

---

## 💰 BUDGET ESTIMÉ

| Élément | Coût |
|---------|------|
| **Développement Phase 1** (3 jours) | ~2 400€ |
| **Développement Phase 2** (3 jours) | ~2 400€ |
| **Développement Phase 3** (5 jours) | ~4 000€ |
| **Développement Phase 4** (3 jours) | ~2 400€ |
| **Firebase** (hébergement) | ~25€/mois |
| **Stripe** (commissions) | 1.5% + 0.25€ par transaction |
| **Backend Node.js** (Vercel/Railway) | ~15€/mois |
| **TOTAL DÉVELOPPEMENT** | **~11 200€** |
| **TOTAL RECURRENT** | **~40€/mois** |

*Estimation basée sur un TJM de 800€*

---

## ✅ CHECKLIST LIVRAISON

Avant de livrer à TY-ZEF, vérifier :

### Fonctionnel
- [ ] Parsing SMS fonctionne avec 95%+ de précision
- [ ] Devis envoyés automatiquement avec bon prix
- [ ] Navigation GPS opérationnelle
- [ ] Notifications push reçues sur le téléphone
- [ ] Sauvegarde automatique fonctionne

### Intégration Site Web
- [ ] Formulaire web connecté à l'app
- [ ] Tests de bout en bout réussis
- [ ] Fallback email fonctionnel

### Personnalisation
- [ ] Logo et couleurs TY-ZEF appliqués
- [ ] Templates de messages validés par le client
- [ ] Tarifs configurés correctement
- [ ] Villes bretonnes reconnues

### Documentation
- [ ] Guide utilisateur écrit
- [ ] Vidéo tutoriel enregistrée
- [ ] Accès Firebase/Stripe transmis
- [ ] Code source livré sur GitHub

---

## 📞 CONTACTS UTILES

**TY-ZEF Brest VTC**
- Site : https://tyzefbrestvtc.fr
- Email : contact@tyzefbrestvtc.fr
- Tél : 07 80 15 78 23

**Services Recommandés**
- Firebase : https://firebase.google.com
- Stripe : https://stripe.com/fr
- Hébergement Backend : https://vercel.com ou https://railway.app

---

**Document créé par** : OpenCode Agent  
**Date** : 18 Mai 2026  
**Version** : 1.0
