package com.koala.vinoala

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Une bouteille. Champs texte pour une saisie libre ; vides = non renseigné. */
data class Bottle(
    val id: String = UUID.randomUUID().toString(),
    val status: String = CAVE,
    // Le vin
    val name: String = "",
    val domaine: String = "",
    val couleur: String = "",
    val millesime: String = "",
    val appellation: String = "",
    val region: String = "",
    val pays: String = "",
    val cepages: String = "",
    val degre: String = "",
    val contenance: String = "",
    val codeBarres: String = "",
    // Garde et service
    val gardeDe: String = "",
    val gardeA: String = "",
    val temperature: String = "",
    val accords: String = "",
    // Provenance : achat (vide) ou bue ailleurs (restaurant, chez des amis)
    val origine: String = "",
    // Achat (ou, si bue ailleurs : prix payé et lieu)
    val prix: String = "",
    val lieu: String = "",
    val dateAchat: String = "",
    // Dégustation
    val dateBue: String = "",
    val note: String = "",
    val commentaire: String = "",
    val noteThomas: String = "",
    val comThomas: String = "",
    val noteCindy: String = "",
    val comCindy: String = "",
    // Photo : miniature dans la fiche, grande photo dans la collection « photos »
    val thumb: String = "",
    val hasPhoto: Boolean = false,
    val creeLe: Long = System.currentTimeMillis(),
    val majLe: Long = System.currentTimeMillis(),
) {
    val drunk get() = status == BUE
    val ailleurs get() = origine == AILLEURS
    val title get() = name.ifBlank { domaine.ifBlank { "Vin sans nom" } }

    fun subtitle(): String = listOf(domaine.takeIf { it != name }, appellation, millesime)
        .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")

    /** Remet à zéro achat, dégustation et photo : sert à racheter le même vin. */
    fun asNewPurchase() = copy(
        id = UUID.randomUUID().toString(), status = CAVE, origine = "", prix = "", lieu = "", dateAchat = today(),
        dateBue = "", note = "", commentaire = "", noteThomas = "", comThomas = "", noteCindy = "", comCindy = "",
        thumb = "", hasPhoto = false, creeLe = System.currentTimeMillis(), majLe = System.currentTimeMillis(),
    )

    /** État d'apogée selon l'année en cours. */
    fun apogee(year: Int = Calendar.getInstance().get(Calendar.YEAR)): Apogee? {
        if (drunk) return null
        val de = gardeDe.trim().toIntOrNull()
        val a = gardeA.trim().toIntOrNull()
        return when {
            a != null && year > a -> Apogee.PASSE
            a != null && year == a -> Apogee.VITE
            de != null && year >= de -> Apogee.PRET
            de != null -> Apogee.ATTENDRE
            a != null -> Apogee.PRET
            else -> null
        }
    }

    fun toMap(): Map<String, Any> = mapOf(
        "status" to status, "name" to name, "domaine" to domaine, "couleur" to couleur, "millesime" to millesime,
        "appellation" to appellation, "region" to region, "pays" to pays, "cepages" to cepages, "degre" to degre,
        "contenance" to contenance, "codeBarres" to codeBarres, "gardeDe" to gardeDe, "gardeA" to gardeA,
        "temperature" to temperature, "accords" to accords, "origine" to origine, "prix" to prix, "lieu" to lieu, "dateAchat" to dateAchat,
        "dateBue" to dateBue, "note" to note, "commentaire" to commentaire, "noteThomas" to noteThomas,
        "comThomas" to comThomas, "noteCindy" to noteCindy, "comCindy" to comCindy, "thumb" to thumb,
        "hasPhoto" to hasPhoto, "creeLe" to creeLe, "majLe" to majLe,
    )

    companion object {
        const val CAVE = "cave"
        const val BUE = "bue"
        const val AILLEURS = "ailleurs"
        val COULEURS = listOf("Rouge", "Blanc", "Rosé", "Effervescent", "Moelleux", "Autre")

        fun fromMap(id: String, m: Map<String, Any?>): Bottle {
            fun s(k: String) = (m[k] as? String) ?: ""
            fun l(k: String) = (m[k] as? Number)?.toLong() ?: 0L
            return Bottle(
                id = id, status = s("status").ifBlank { CAVE }, name = s("name"), domaine = s("domaine"),
                couleur = s("couleur"), millesime = s("millesime"), appellation = s("appellation"),
                region = s("region"), pays = s("pays"), cepages = s("cepages"), degre = s("degre"),
                contenance = s("contenance"), codeBarres = s("codeBarres"), gardeDe = s("gardeDe"),
                gardeA = s("gardeA"), temperature = s("temperature"), accords = s("accords"), origine = s("origine"), prix = s("prix"),
                lieu = s("lieu"), dateAchat = s("dateAchat"), dateBue = s("dateBue"), note = s("note"),
                commentaire = s("commentaire"), noteThomas = s("noteThomas"), comThomas = s("comThomas"),
                noteCindy = s("noteCindy"), comCindy = s("comCindy"), thumb = s("thumb"),
                hasPhoto = m["hasPhoto"] as? Boolean ?: false, creeLe = l("creeLe"), majLe = l("majLe"),
            )
        }

        fun today(): String = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())
    }
}

enum class Apogee(val label: String) {
    ATTENDRE("À garder"), PRET("Prêt à boire"), VITE("À boire cette année"), PASSE("Apogée passée")
}

/** Note affichée : "7,5" ; vide si non notée. */
fun fmtNote(n: String): String = n.toDoubleOrNull()?.let {
    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString().replace('.', ',')
} ?: ""

/** Note moyenne de la bouteille : commune, sinon moyenne des notes individuelles. */
fun Bottle.score(): Double? = note.toDoubleOrNull()
    ?: listOfNotNull(noteThomas.toDoubleOrNull(), noteCindy.toDoubleOrNull()).takeIf { it.isNotEmpty() }?.average()
