# PLAN - AMÉLIORATIONS GPS ET RENTABILITÉ

## 1. Calcul de Rentabilité Détaillé (Avant envoi devis)

### Objectif
Afficher un résumé complet de la rentabilité avant d'envoyer le devis au client.

### Données à afficher
- **Prix total TTC** (calculé avec les majorations)
- **Coût carburant** (distance × coût/km)
- **Coût opérationnel** (temps estimé × coût/heure)
- **Bénéfice net** (prix - coûts)
- **Rentabilité %** (bénéfice / prix × 100)
- **Temps estimé** du trajet
- **Distance réelle** (via OSRM)

### Implémentation
**Fichier :** `RideDetailScreen.kt` ou `ControlCenterScreen.kt`

Ajouter une carte "Analyse de rentabilité" avec :
- Barre de progression colorée (rouge < 30%, orange 30-50%, vert > 50%)
- Détail des coûts
- Recommandation ("Course rentable" / "Course à éviter")

---

## 2. Vrai GPS Navigation avec MapLibre + OSRM

### Objectif
Remplacer l'affichage "trait direct" par un vrai itinéraire routier avec guidage.

### Architecture

#### 2.1 Service OSRM (Backend)
**Fichier :** `OsrmRoutingService.kt`

Utiliser l'API publique OSRM :
```
http://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=full&geometries=geojson&steps=true
```

Retourne :
- Geometry complète (points GPS du trajet)
- Durée estimée
- Distance
- Instructions étape par étape

#### 2.2 MapLibre Navigation SDK
**Dépendances :**
```gradle
implementation 'org.maplibre.gl:maplibre-gl-android-sdk:11.0.0'
implementation 'org.maplibre.gl:maplibre-navigation-android:2.0.0'
```

**Fichier :** `NavigationMapView.kt`

Fonctionnalités :
- Affichage carte vectorielle
- Tracé itinéraire réel (suivant les routes)
- Instructions turn-by-turn
- Zoom automatique sur la position
- Marqueurs départ/arrivée

#### 2.3 Intégration dans ActiveRideScreen
**Modifications :**
- Remplacer la carte simple par NavigationMapView
- Afficher l'itinéraire calculé via OSRM
- Boutons "Démarrer navigation" / "Itinéraire suivant"
- Instructions vocales (optionnel)

---

## 3. Correction Problème Téléchargement IA

### Problèmes identifiés
1. Pas de vérification de connexion Internet
2. Pas de reprise après échec
3. Pas de timeout approprié
4. Téléchargement bloque l'UI

### Solutions

#### 3.1 Gestion erreurs réseau
**Fichier :** `AiModelDownloadScreen.kt` / `LlamaModelManager.kt`

- Vérifier connexion avant téléchargement
- Retry automatique (3 tentatives)
- Timeout augmenté (5 min)
- Téléchargement en arrière-plan (WorkManager)
- Notification de progression

#### 3.2 Alternative : HuggingFace API
Si le modèle local ne fonctionne pas, utiliser l'API cloud :
- Endpoint : `https://api-inference.huggingface.co/models/microsoft/Phi-3-mini-4k-instruct`
- Clé API gratuite (rate limité)
- Fallback automatique vers regex si API indisponible

---

## 4. Fichiers à modifier/créer

### Nouveaux fichiers
1. `OsrmRoutingService.kt` - Service routing OSRM
2. `NavigationMapView.kt` - Composant carte navigation
3. `ProfitabilityCard.kt` - Carte rentabilité
4. `AiInferenceService.kt` - Service IA cloud (fallback)

### Fichiers modifiés
1. `ActiveRideScreen.kt` - Intégration vrai GPS
2. `ControlCenterScreen.kt` - Ajout rentabilité avant devis
3. `RideDetailScreen.kt` - Détails rentabilité
4. `AiModelDownloadScreen.kt` - Gestion erreurs réseau
5. `LlamaModelManager.kt` - Retry et fallback
6. `build.gradle.kts` - Dépendances MapLibre Navigation

---

## 5. Ordre d'implémentation

1. **Rentabilité** (rapide, impact immédiat)
2. **OSRM Service** (fondation pour le GPS)
3. **MapLibre Navigation** (interface utilisateur)
4. **Correction IA** (amélioration robustesse)

---

## 6. Notes techniques

### OSRM vs Valhalla
- **OSRM** : Plus rapide, meilleur pour voiture
- **Valhalla** : Multimodal, plus précis pour piéton/vélo

Choix : OSRM (usage taxi = voiture)

### Limite API OSRM publique
- 100 requêtes/minute gratuites
- Pour usage commercial intense : auto-héberger ou utiliser MapTiler

### MapLibre Navigation SDK
- Fork de l'ancien Mapbox Navigation SDK v0.x
- Documentation limitée mais fonctionnel
- Alternative : utiliser MapLibre GL + Directions API OSRM manuellement

---

*Plan créé le : 18/05/2026*
*Objectif : GPS professionnel + Rentabilité détaillée + IA robuste*
