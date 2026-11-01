package com.koala.vinoala

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Ce qu'Open Food Facts sait d'un code-barres. */
data class OffInfo(val bottle: Bottle, val imageUrl: String?)

object Off {
    private const val UA = "Vinoala/1.0 (Android; thomasdar-lgtm)"
    private const val FIELDS = "product_name_fr,product_name,generic_name_fr,brands,quantity,image_front_url," +
        "categories_tags,origins,manufacturing_places,labels,nutriments"

    /** null = inconnu d'Open Food Facts. Lève une exception si hors ligne. */
    suspend fun lookup(code: String): OffInfo? = withContext(Dispatchers.IO) {
        val c = URL("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=$FIELDS")
            .openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 10000
        c.setRequestProperty("User-Agent", UA)
        try {
            if (c.responseCode == 404) return@withContext null
            if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
            parse(code, c.inputStream.bufferedReader().readText())
        } finally {
            c.disconnect()
        }
    }

    fun parse(code: String, json: String): OffInfo? {
        val o = JSONObject(json)
        if (o.optInt("status", 0) != 1) return null
        val p = o.optJSONObject("product") ?: return null
        val name = p.optString("product_name_fr").ifBlank { p.optString("product_name") }.trim()
        val brand = p.optString("brands").split(",").firstOrNull()?.trim() ?: ""
        if (name.isBlank() && brand.isBlank()) return null
        val tags = buildList {
            val a = p.optJSONArray("categories_tags")
            if (a != null) for (i in 0 until a.length()) add(a.optString(i))
        }
        val couleur = when {
            tags.any { "sparkling" in it || "champagne" in it || "cremant" in it || "mousseux" in it } -> "Effervescent"
            tags.any { "rose-wine" in it || "vins-roses" in it } -> "Rosé"
            tags.any { "sweet-wine" in it || "moelleux" in it || "liquoreux" in it } -> "Moelleux"
            tags.any { "white-wine" in it || "vins-blancs" in it } -> "Blanc"
            tags.any { "red-wine" in it || "vins-rouges" in it } -> "Rouge"
            else -> ""
        }
        val abv = p.optJSONObject("nutriments")?.optDouble("alcohol_100g")?.takeIf { !it.isNaN() && it > 0 }
        val text = "$name ${p.optString("generic_name_fr")}"
        val b = Bottle(
            name = name.ifBlank { brand },
            domaine = brand,
            couleur = couleur,
            millesime = vintage(text),
            appellation = appellation(tags),
            pays = p.optString("origins").split(",").firstOrNull()?.trim() ?: "",
            degre = abv?.let { fmtNote(it.toString()) } ?: "",
            contenance = p.optString("quantity").trim(),
            codeBarres = code,
        )
        val img = p.optString("image_front_url").takeIf { it.startsWith("http") }
        return OffInfo(b, img)
    }

    fun vintage(text: String): String =
        Regex("""\b(19[5-9]\d|20[0-4]\d)\b""").find(text)?.value ?: ""

    /** Les catégories « fr:bordeaux-superieur » donnent souvent l'appellation. */
    private fun appellation(tags: List<String>): String {
        val generic = setOf("vins", "boissons", "vins-francais", "vins-rouges", "vins-blancs", "vins-roses",
            "boissons-alcoolisees", "vins-de-france", "aliments-et-boissons-a-base-de-vegetaux")
        val t = tags.lastOrNull { it.startsWith("fr:") && it.removePrefix("fr:") !in generic } ?: return ""
        return t.removePrefix("fr:").split("-").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }
}
