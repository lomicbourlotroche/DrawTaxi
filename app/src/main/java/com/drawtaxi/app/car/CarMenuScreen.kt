package com.drawtaxi.app.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat

class CarMenuScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Accueil")
                .addText("Vue d'ensemble des courses")
                .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, android.R.drawable.ic_dialog_map)).build())
                .setOnClickListener { /* Future: Navigate to Home */ }
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Tableau de bord")
                .addText("Statistiques et revenus")
                .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_info_details)).build())
                .setOnClickListener { /* Future: Navigate to Dashboard */ }
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Paramètres")
                .addText("Configuration de l'application")
                .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, android.R.drawable.ic_menu_preferences)).build())
                .setOnClickListener { /* Future: Navigate to Settings */ }
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("Menu Principal")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }
}