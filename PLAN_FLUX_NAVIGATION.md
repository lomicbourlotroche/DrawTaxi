# PLAN - Correction du flux de navigation des courses

## 📊 FLUX ACTUEL (Problématique)

```
HomeScreen → Clic sur course confirmée → RideDetailScreen (isPending=false)
                                                    ↓
                                        Bouton "Envoyer Reçu" (non fonctionnel)
                                                    ↓
                                        Pas d'accès direct à la navigation
```

**Problèmes identifiés :**
1. ❌ Deux écrans de navigation possibles (doublon)
2. ❌ Bouton "Envoyer Reçu" ne lance pas la navigation
3. ❌ RideDetailScreen pour courses confirmées n'est pas optimisé
4. ❌ La nav bar s'affiche sur RideNavigationScreen

---

## 🎯 OBJECTIF

```
HomeScreen → Clic sur course confirmée → RideNavigationScreen (sans nav bar)
                                                    ↓
                                        Navigation complète avec 3 phases
                                        (Rouge → Bleu → Vert)
```

---

## ✅ SOLUTION PROPOSÉE

### Étape 1 : Modifier le flux dans MainActivity
**Fichier :** `MainActivity.kt`

**Changement :**
- Quand on clique sur une course **confirmée** dans HomeScreen → Ouvrir directement `RideNavigationScreen`
- Quand on clique sur une course **en attente** (DRAFT/QUOTED) → Ouvrir `RideDetailScreen`

```kotlin
// Dans HomeScreen onRideClick :
onRideClick = { ride ->
    if (ride.status == RideStatus.CONFIRMED || ride.status == RideStatus.IN_PROGRESS) {
        // Course confirmée → Navigation directe
        activeRide = ride
    } else {
        // Course en attente → Détail
        selectedRide = ride
    }
}
```

---

### Étape 2 : Modifier RideDetailScreen pour les courses confirmées
**Fichier :** `RideDetailScreen.kt`

**Changement :**
- Remplacer le bouton "Envoyer Reçu" par "Démarrer la navigation"
- Le bouton appelle `onStartRide(ride)` au lieu de `onShareReceipt(ride)`

```kotlin
// Avant (ligne 121-130)
Button(onClick = { onShareReceipt(ride) }) {
    Icon(Icons.Default.Share, ...)
    Text("Envoyer Reçu")
}

// Après
Button(onClick = { onStartRide(ride) }) {
    Icon(Icons.Default.Navigation, ...)
    Text("Démarrer la navigation")
}
```

---

### Étape 3 : Retirer la nav bar de RideNavigationScreen
**Fichier :** `RideNavigationScreen.kt`

**Changement :**
- Utiliser `Scaffold` sans `bottomBar`
- La navigation est gérée par les boutons intégrés dans l'écran

```kotlin
Scaffold(
    topBar = { ... },
    // bottomBar = { ... } ← SUPPRIMER
) { padding ->
    // Contenu plein écran
}
```

---

### Étape 4 : Simplifier le flux des écrans
**Architecture finale :**

| Type de course | Écran affiché | Actions disponibles |
|----------------|---------------|---------------------|
| **DRAFT / QUOTED** (en attente) | `RideDetailScreen` | Modifier, Supprimer, Valider, Envoyer devis |
| **CONFIRMED / IN_PROGRESS** | `RideNavigationScreen` | Navigation, Appeler client, Message, Terminer |
| **COMPLETED** (terminée) | `RideDetailScreen` | Voir détails, Envoyer reçu, Statistiques |

---

## 📝 FICHIERS À MODIFIER

1. **`MainActivity.kt`** (ligne ~348)
   - Modifier `onRideClick` dans HomeScreen pour discriminer par statut

2. **`RideDetailScreen.kt`** (ligne ~121-130)
   - Remplacer bouton "Envoyer Reçu" par "Démarrer la navigation"
   - Condition : `if (!isPending && ride.status != RideStatus.COMPLETED)`

3. **`RideNavigationScreen.kt`** (ligne ~140-180)
   - Supprimer la `bottomBar` du Scaffold
   - Intégrer les contrôles de navigation dans le contenu principal

---

## 🔄 FLUX APRÈS CORRECTION

```
┌─────────────────────────────────────────────────────────────┐
│                    HomeScreen                               │
│  (Liste des courses confirmées)                             │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Clic sur course
                       ▼
        ┌──────────────┴──────────────┐
        │                             │
  Course confirmée?            Course terminée?
        │                             │
        ▼                             ▼
┌───────────────────┐    ┌───────────────────────┐
│ RideNavigationScreen│    │ RideDetailScreen      │
│ (Navigation GPS)    │    │ (Synthèse + Reçu)     │
│ - 3 phases colorées │    │ - Envoyer reçu        │
│ - Appeler client    │    │ - Voir détails        │
│ - Message client    │    │ - Statistiques        │
│ - Terminer          │    └───────────────────────┘
└───────────────────┘
```

---

## ⚠️ POINTS DE VIGILANCE

1. **Ne pas casser le flux des courses en attente** (DRAFT/QUOTED)
2. **Conserver le bouton "Envoyer Reçu"** pour les courses COMPLETED
3. **RideNavigationScreen doit être plein écran** (pas de nav bar)
4. **Le bouton "Terminer"** dans RideNavigationScreen doit marquer la course comme COMPLETED

---

## 🎯 RÉSULTAT ATTENDU

- ✅ Un seul écran de navigation (RideNavigationScreen)
- ✅ Navigation directe depuis HomeScreen pour les courses confirmées
- ✅ RideNavigationScreen sans nav bar (plein écran)
- ✅ Bouton "Démarrer la navigation" dans RideDetailScreen pour les courses confirmées
- ✅ Bouton "Envoyer Reçu" conservé uniquement pour les courses terminées

---

*Plan créé le : 18/05/2026*
*Objectif : Simplifier le flux de navigation et corriger les doublons*
