package com.voltbody.app.util

import com.voltbody.app.data.DietPlan
import com.voltbody.app.data.Meal
import java.text.Normalizer

/** Un producto de la lista de la compra: cantidades sumadas por unidad ("g", "ml", "ud" o "" si no se indico). */
data class ShoppingItem(val key: String, val name: String, val aisle: String, val amounts: Map<String, Double>, val uses: Int) {
    /** "840 g", "1,2 kg + 2 ud", o "x3" si el menu no daba cantidades. */
    fun amountLabel(): String {
        val parts = amounts.filterKeys { it.isNotEmpty() }.map { (u, v) -> formatAmount(v, u) }
        return if (parts.isEmpty()) "x$uses" else parts.joinToString(" + ")
    }
}

data class ShoppingAisle(val name: String, val emoji: String, val items: List<ShoppingItem>)

/** Pasillos en el orden habitual de un supermercado. */
private val AISLES = listOf(
    Triple("Frutas y verduras", "🥬", listOf(
        "brocoli", "espinaca", "lechuga", "tomate", "cebolla", "ajo", "pimiento", "calabacin", "berenjena", "zanahoria", "pepino",
        "champinon", "seta", "esparrago", "judia verde", "coliflor", "col ", "kale", "rucula", "canonigo", "aguacate", "limon",
        "platano", "manzana", "pera", "naranja", "fresa", "arandano", "frutos rojos", "kiwi", "pina", "mango", "uva", "melon",
        "sandia", "fruta", "verdura", "patata", "boniato", "calabaza", "puerro", "apio", "jengibre", "perejil", "cilantro", "albahaca",
    )),
    Triple("Carne y pescado", "🥩", listOf(
        "pollo", "pavo", "ternera", "cerdo", "lomo", "solomillo", "carne", "jamon", "salmon", "atun fresco", "merluza", "bacalao",
        "dorada", "lubina", "sardina", "caballa", "gamba", "langostino", "pescado", "calamar", "sepia", "mejillon", "hamburguesa",
    )),
    Triple("Huevos y lácteos", "🥚", listOf(
        "huevo", "clara", "leche", "yogur", "kefir", "skyr", "queso", "requeson", "mantequilla", "nata", "bebida de avena", "bebida vegetal",
    )),
    Triple("Despensa", "🫙", listOf(
        "arroz", "pasta", "espagueti", "macarron", "quinoa", "cuscus", "avena", "copos", "pan", "tortilla de trigo", "wrap", "harina",
        "lenteja", "garbanzo", "alubia", "judia blanca", "legumbre", "atun", "conserva", "tomate triturado", "frutos secos", "nuez",
        "nueces", "almendra", "cacahuete", "crema de", "semilla", "chia", "lino", "aceite", "aove", "vinagre", "sal", "pimienta",
        "especia", "oregano", "comino", "canela", "miel", "chocolate", "cacao", "proteina", "whey", "tortita", "galleta", "cereal",
    )),
)

private const val OTHER = "Otros"

private fun normalize(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()

private fun aisleOf(key: String): Pair<String, String> {
    val k = " $key "
    // "atun" en Despensa (lata) salvo que se diga fresco: se comprueba Carne y pescado primero solo con "atun fresco".
    for ((name, emoji, words) in AISLES) if (words.any { k.contains(it) }) return name to emoji
    return OTHER to "🛒"
}

private val UNIT_ALIASES = mapOf(
    "g" to "g", "gr" to "g", "grs" to "g", "gramos" to "g", "kg" to "kg", "ml" to "ml", "l" to "l", "litro" to "l", "litros" to "l",
    "ud" to "ud", "uds" to "ud", "unidad" to "ud", "unidades" to "ud", "u" to "ud",
    "cda" to "cda", "cdas" to "cda", "cucharada" to "cda", "cucharadas" to "cda", "cdta" to "cdta", "cucharadita" to "cdta", "cucharaditas" to "cdta",
    "rebanada" to "rebanada", "rebanadas" to "rebanada", "loncha" to "loncha", "lonchas" to "loncha", "lata" to "lata", "latas" to "lata",
    "punado" to "puñado", "punados" to "puñado", "scoop" to "scoop", "scoops" to "scoop", "cacito" to "scoop", "cacitos" to "scoop",
)

private val QTY = Regex("^(\\d+(?:[.,]\\d+)?)\\s*([a-zñáéíóú]+)?\\.?\\s+(?:de\\s+)?(.+)$")

/** Abreviaturas y palabras que no aportan al nombre del producto. */
private val NAME_FIXES = listOf(
    Regex("\\baove\\b") to "aceite de oliva virgen extra",
    Regex("\\b(en crudo|crudo|cruda|cocido|cocida|al natural|escurrido|picado|picada|troceado|troceada|fresco|fresca)\\b") to "",
)

/** Ingredientes de una comida a partir de su descripcion ("120 g pechuga de pollo, 70 g arroz basmati, 10 g AOVE. Plancha..."). */
internal fun parseIngredients(description: String): List<Triple<String, String, Double?>> {
    if (description.isBlank()) return emptyList()
    // La lista de ingredientes va antes de la primera frase de preparacion.
    val list = description.split(Regex("\\.\\s+(?=[A-ZÁÉÍÓÚÑ])")).first().trimEnd('.', ' ')
    return list.split(Regex(",|;|\\s+y\\s+|\\s+\\+\\s+")).mapNotNull { raw ->
        // Se conservan las tildes para mostrar; la clave y el pasillo se calculan sobre el texto normalizado.
        var frag = raw.lowercase().replace(Regex("\\(.*?\\)"), "").trim()
        if (frag.isEmpty() || frag.length > 60) return@mapNotNull null
        var unit = ""
        var qty: Double? = null
        QTY.find(frag)?.let { m ->
            val u = m.groupValues[2]
            val alias = UNIT_ALIASES[normalize(u)]
            qty = m.groupValues[1].replace(',', '.').toDoubleOrNull()
            frag = if (alias != null) m.groupValues[3] else listOf(u, m.groupValues[3]).filter { it.isNotEmpty() }.joinToString(" ")
            unit = alias ?: "ud"
        }
        for ((re, rep) in NAME_FIXES) frag = frag.replace(re, rep)
        frag = frag.replace(Regex("\\s+"), " ").trim().removePrefix("de ").trim()
        if (frag.length < 2) null else Triple(frag, unit, qty)
    }
}

/** Convierte kg->g y l->ml para poder sumar. */
private fun base(unit: String, v: Double): Pair<String, Double> = when (unit) {
    "kg" -> "g" to v * 1000
    "l" -> "ml" to v * 1000
    else -> unit to v
}

private fun formatAmount(v: Double, unit: String): String {
    fun n(x: Double) = if (x % 1.0 == 0.0) x.toLong().toString() else String.format(java.util.Locale("es", "ES"), "%.1f", x)
    return when {
        unit == "g" && v >= 1000 -> "${n(Math.round(v / 100.0) / 10.0)} kg"
        unit == "ml" && v >= 1000 -> "${n(Math.round(v / 100.0) / 10.0)} l"
        unit == "g" || unit == "ml" -> "${n((Math.round(v / 10.0) * 10).toDouble())} $unit"
        else -> "${n(v)} $unit"
    }
}

/**
 * Lista de la compra de los 7 dias del menu (o 7 veces el menu base si el plan no es semanal),
 * agrupada por pasillo y con las cantidades sumadas.
 */
fun buildShoppingList(diet: DietPlan): List<ShoppingAisle> {
    val days: List<List<Meal>> = if (diet.week.isNotEmpty()) diet.week.map { it.meals } else List(7) { diet.meals }
    val acc = LinkedHashMap<String, Triple<String, MutableMap<String, Double>, Int>>()
    for (meals in days) for (meal in meals) for ((name, unit, qty) in parseIngredients(meal.description)) {
        val key = normalize(name).removeSuffix("s").removeSuffix("e") // plurales sencillos: huevos/huevo, tomates/tomate
        val cur = acc[key] ?: Triple(name, mutableMapOf(), 0)
        if (qty != null) {
            val (u, v) = base(unit, qty)
            cur.second[u] = (cur.second[u] ?: 0.0) + v
        }
        acc[key] = Triple(cur.first, cur.second, cur.third + 1)
    }
    val byAisle = acc.map { (key, v) ->
        val (aisle, _) = aisleOf(normalize(v.first))
        ShoppingItem(key, v.first.replaceFirstChar { it.uppercase() }, aisle, v.second, v.third)
    }.groupBy { it.aisle }
    val order = AISLES.map { it.first to it.second } + (OTHER to "🛒")
    return order.mapNotNull { (name, emoji) ->
        byAisle[name]?.sortedBy { it.name }?.let { ShoppingAisle(name, emoji, it) }
    }
}

/** Texto plano para compartir la lista (WhatsApp, notas...). */
fun shoppingListText(aisles: List<ShoppingAisle>, done: Set<String>): String = buildString {
    append("🛒 Lista de la compra VoltBody\n")
    for (a in aisles) {
        val pending = a.items.filter { it.key !in done }
        if (pending.isEmpty()) continue
        append("\n${a.emoji} ${a.name}\n")
        pending.forEach { append("• ${it.name} — ${it.amountLabel()}\n") }
    }
}
