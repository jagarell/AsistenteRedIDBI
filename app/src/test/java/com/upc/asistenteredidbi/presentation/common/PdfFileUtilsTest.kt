package com.upc.asistenteredidbi.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfFileUtilsTest {
    private val dia = "2026-11-16"

    @Test fun nombreDelLocalYFecha() =
        assertEquals("Tentaciones_Gourmet_2026-11-16.pdf", PdfFileUtils.nombreArchivo("Tentaciones Gourmet", fecha = dia))

    @Test fun quitaLoQueUnNombreDeArchivoNoAdmite() =
        assertEquals("Cevichería_El_Pez_2026-11-16.pdf", PdfFileUtils.nombreArchivo("  Cevichería  \"El/Pez\"? ", fecha = dia))

    @Test fun sinNombreUsaMinuta() =
        assertEquals("Minuta_2026-11-16.pdf", PdfFileUtils.nombreArchivo("   ", fecha = dia))

    @Test fun vistaPreviaYPropuestaAntigua() {
        assertEquals("Local_2026-11-16_vista_previa.pdf", PdfFileUtils.nombreArchivo("Local", sufijo = "vista previa", fecha = dia))
        assertEquals("Propuesta_Local_2026-11-16.pdf", PdfFileUtils.nombreArchivo("Local", prefijo = "Propuesta", fecha = dia))
    }

    @Test fun nombreMuyLargoSeCorta() {
        val n = PdfFileUtils.nombreArchivo("A".repeat(200), fecha = dia)
        assertEquals(60 + "_2026-11-16.pdf".length, n.length)
    }
}
