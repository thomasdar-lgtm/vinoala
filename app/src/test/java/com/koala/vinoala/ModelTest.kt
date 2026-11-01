package com.koala.vinoala

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelTest {
    @Test fun roundTrip() {
        val b = Bottle(name = "Pomerol", millesime = "2018", note = "7.5", hasPhoto = true, thumb = "abc")
        assertEquals(b, Bottle.fromMap(b.id, b.toMap()))
    }

    @Test fun apogee() {
        assertEquals(Apogee.ATTENDRE, Bottle(gardeDe = "2030").apogee(2026))
        assertEquals(Apogee.PRET, Bottle(gardeDe = "2024", gardeA = "2030").apogee(2026))
        assertEquals(Apogee.VITE, Bottle(gardeA = "2026").apogee(2026))
        assertEquals(Apogee.PASSE, Bottle(gardeA = "2025").apogee(2026))
        assertNull(Bottle().apogee(2026))
        assertNull(Bottle(status = Bottle.BUE, gardeA = "2025").apogee(2026))
    }

    @Test fun notes() {
        assertEquals("7,5", fmtNote("7.5"))
        assertEquals("8", fmtNote("8.0"))
        assertEquals("", fmtNote(""))
        assertEquals(6.5, Bottle(noteThomas = "6", noteCindy = "7").score()!!, 0.001)
        assertEquals(9.0, Bottle(note = "9", noteThomas = "6").score()!!, 0.001)
    }

    @Test fun ean() {
        org.junit.Assert.assertTrue(Ean.valid("3017620422003"))
        org.junit.Assert.assertTrue(Ean.valid("96385074"))
        org.junit.Assert.assertTrue(Ean.valid("036000291452"))
        org.junit.Assert.assertFalse(Ean.valid("3017620422004"))
        org.junit.Assert.assertFalse(Ean.valid("12345"))
        org.junit.Assert.assertFalse(Ean.valid("ABCDEFGH"))
    }

    @Test fun offParse() {
        val json = """{"status":1,"product":{"product_name_fr":"Château Machin 2019","brands":"Château Machin, Vignobles X",
            "quantity":"75 cl","categories_tags":["en:wines","en:red-wines","fr:saint-emilion-grand-cru"],
            "origins":"France","nutriments":{"alcohol_100g":13.5},"image_front_url":"https://images.openfoodfacts.org/x.jpg"}}"""
        val r = Off.parse("3760000000000", json)!!
        assertEquals("Château Machin 2019", r.bottle.name)
        assertEquals("Château Machin", r.bottle.domaine)
        assertEquals("Rouge", r.bottle.couleur)
        assertEquals("2019", r.bottle.millesime)
        assertEquals("Saint Emilion Grand Cru", r.bottle.appellation)
        assertEquals("13,5", r.bottle.degre)
        assertEquals("75 cl", r.bottle.contenance)
        assertEquals("https://images.openfoodfacts.org/x.jpg", r.imageUrl)
        assertNull(Off.parse("1", """{"status":0}"""))
    }
}
