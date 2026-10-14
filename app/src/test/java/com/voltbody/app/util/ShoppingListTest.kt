package com.voltbody.app.util

import com.voltbody.app.data.DayMenu
import com.voltbody.app.data.DietPlan
import com.voltbody.app.data.Meal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListTest {
    @Test fun parsesQuantitiesUnitsAndStopsAtPreparation() {
        val items = parseIngredients("120 g pechuga de pollo, 70 g arroz basmati, 2 huevos, 10 g AOVE. Plancha y arroz hervido.")
        assertEquals(
            listOf(
                Triple("pechuga de pollo", "g", 120.0),
                Triple("arroz basmati", "g", 70.0),
                Triple("huevos", "ud", 2.0),
                Triple("aceite de oliva virgen extra", "g", 10.0),
            ),
            items,
        )
    }

    @Test fun parsesItemsWithoutQuantities() {
        assertEquals(listOf("avena", "huevo", "plátano", "canela"), parseIngredients("Avena, huevo, plátano y canela.").map { it.first })
    }

    @Test fun sumsAcrossTheWeekAndGroupsByAisle() {
        fun day(d: String, desc: String) = DayMenu(d, listOf(Meal(id = "$d-1", time = "14:00", description = desc)))
        val plan = DietPlan(week = listOf(
            day("Lunes", "150 g pechuga de pollo, 1 kg patata"),
            day("Martes", "200 g pechuga de pollo, 2 huevos"),
        ))
        val aisles = buildShoppingList(plan)
        val all = aisles.flatMap { it.items }
        assertEquals("350 g", all.first { it.name == "Pechuga de pollo" }.amountLabel())
        assertEquals("1 kg", all.first { it.name == "Patata" }.amountLabel())
        assertEquals("Carne y pescado", all.first { it.name == "Pechuga de pollo" }.aisle)
        assertTrue(aisles.first().name == "Frutas y verduras")
    }
}
