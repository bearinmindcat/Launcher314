package com.bearinmind.launcher314.helpers

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.bearinmind.launcher314.R
import com.bearinmind.launcher314.data.getSelectedFont
import com.bearinmind.launcher314.data.setSelectedFont
import com.bearinmind.launcher314.data.getImportedFontPaths
import com.bearinmind.launcher314.data.addImportedFontPath
import com.bearinmind.launcher314.data.removeImportedFontPath
import java.io.File

object FontManager {

    data class FontItem(
        val id: String,
        val displayName: String,
        val fontFamily: FontFamily,
        val isBundled: Boolean
    )

    val bundledFonts: List<FontItem> = listOf(
        FontItem("abel", "Abel", FontFamily(Font(R.font.abel)), true),
        FontItem("abril_fatface", "Abril Fatface", FontFamily(Font(R.font.abril_fatface)), true),
        FontItem("alegreya", "Alegreya", FontFamily(Font(R.font.alegreya)), true),
        FontItem("alegreya_sans", "Alegreya Sans", FontFamily(Font(R.font.alegreya_sans)), true),
        FontItem("alfa_slab_one", "Alfa Slab One", FontFamily(Font(R.font.alfa_slab_one)), true),
        FontItem("alice", "Alice", FontFamily(Font(R.font.alice)), true),
        FontItem("amatic_sc", "Amatic SC", FontFamily(Font(R.font.amatic_sc)), true),
        FontItem("amiri", "Amiri", FontFamily(Font(R.font.amiri)), true),
        FontItem("antic_slab", "Antic Slab", FontFamily(Font(R.font.antic_slab)), true),
        FontItem("anton", "Anton", FontFamily(Font(R.font.anton)), true),
        FontItem("archivo", "Archivo", FontFamily(Font(R.font.archivo)), true),
        FontItem("archivo_narrow", "Archivo Narrow", FontFamily(Font(R.font.archivo_narrow)), true),
        FontItem("arimo", "Arimo", FontFamily(Font(R.font.arimo)), true),
        FontItem("arvo", "Arvo", FontFamily(Font(R.font.arvo)), true),
        FontItem("asap", "Asap", FontFamily(Font(R.font.asap)), true),
        FontItem("assistant", "Assistant", FontFamily(Font(R.font.assistant)), true),
        FontItem("barlow", "Barlow", FontFamily(Font(R.font.barlow)), true),
        FontItem("barlow_condensed", "Barlow Condensed", FontFamily(Font(R.font.barlow_condensed)), true),
        FontItem("bebas_neue", "Bebas Neue", FontFamily(Font(R.font.bebas_neue)), true),
        FontItem("bitter", "Bitter", FontFamily(Font(R.font.bitter)), true),
        FontItem("bodoni_moda", "Bodoni Moda", FontFamily(Font(R.font.bodoni_moda)), true),
        FontItem("bree_serif", "Bree Serif", FontFamily(Font(R.font.bree_serif)), true),
        FontItem("cabin", "Cabin", FontFamily(Font(R.font.cabin)), true),
        FontItem("cairo", "Cairo", FontFamily(Font(R.font.cairo)), true),
        FontItem("cantarell", "Cantarell", FontFamily(Font(R.font.cantarell)), true),
        FontItem("cardo", "Cardo", FontFamily(Font(R.font.cardo)), true),
        FontItem("catamaran", "Catamaran", FontFamily(Font(R.font.catamaran)), true),
        FontItem("caveat", "Caveat", FontFamily(Font(R.font.caveat)), true),
        FontItem("cinzel", "Cinzel", FontFamily(Font(R.font.cinzel)), true),
        FontItem("comfortaa", "Comfortaa", FontFamily(Font(R.font.comfortaa)), true),
        FontItem("concert_one", "Concert One", FontFamily(Font(R.font.concert_one)), true),
        FontItem("cormorant_garamond", "Cormorant Garamond", FontFamily(Font(R.font.cormorant_garamond)), true),
        FontItem("courgette", "Courgette", FontFamily(Font(R.font.courgette)), true),
        FontItem("crimson_text", "Crimson Text", FontFamily(Font(R.font.crimson_text)), true),
        FontItem("cuprum", "Cuprum", FontFamily(Font(R.font.cuprum)), true),
        FontItem("dancing_script", "Dancing Script", FontFamily(Font(R.font.dancing_script)), true),
        FontItem("dm_sans", "DM Sans", FontFamily(Font(R.font.dm_sans)), true),
        FontItem("dm_serif_display", "DM Serif Display", FontFamily(Font(R.font.dm_serif_display)), true),
        FontItem("dosis", "Dosis", FontFamily(Font(R.font.dosis)), true),
        FontItem("eb_garamond", "EB Garamond", FontFamily(Font(R.font.eb_garamond)), true),
        FontItem("encode_sans", "Encode Sans", FontFamily(Font(R.font.encode_sans)), true),
        FontItem("exo_2", "Exo 2", FontFamily(Font(R.font.exo_2)), true),
        FontItem("fira_code", "Fira Code", FontFamily(Font(R.font.fira_code)), true),
        FontItem("fira_sans", "Fira Sans", FontFamily(Font(R.font.fira_sans)), true),
        FontItem("fjalla_one", "Fjalla One", FontFamily(Font(R.font.fjalla_one)), true),
        FontItem("francois_one", "Francois One", FontFamily(Font(R.font.francois_one)), true),
        FontItem("fredoka", "Fredoka", FontFamily(Font(R.font.fredoka)), true),
        FontItem("gelasio", "Gelasio", FontFamily(Font(R.font.gelasio)), true),
        FontItem("gloria_hallelujah", "Gloria Hallelujah", FontFamily(Font(R.font.gloria_hallelujah)), true),
        FontItem("great_vibes", "Great Vibes", FontFamily(Font(R.font.great_vibes)), true),
        FontItem("heebo", "Heebo", FontFamily(Font(R.font.heebo)), true),
        FontItem("hind", "Hind", FontFamily(Font(R.font.hind)), true),
        FontItem("hind_siliguri", "Hind Siliguri", FontFamily(Font(R.font.hind_siliguri)), true),
        FontItem("ibm_plex_mono", "IBM Plex Mono", FontFamily(Font(R.font.ibm_plex_mono)), true),
        FontItem("ibm_plex_sans", "IBM Plex Sans", FontFamily(Font(R.font.ibm_plex_sans)), true),
        FontItem("ibm_plex_serif", "IBM Plex Serif", FontFamily(Font(R.font.ibm_plex_serif)), true),
        FontItem("inconsolata", "Inconsolata", FontFamily(Font(R.font.inconsolata)), true),
        FontItem("indie_flower", "Indie Flower", FontFamily(Font(R.font.indie_flower)), true),
        FontItem("inter", "Inter", FontFamily(Font(R.font.inter)), true),
        FontItem("josefin_sans", "Josefin Sans", FontFamily(Font(R.font.josefin_sans)), true),
        FontItem("jost", "Jost", FontFamily(Font(R.font.jost)), true),
        FontItem("kalam", "Kalam", FontFamily(Font(R.font.kalam)), true),
        FontItem("kanit", "Kanit", FontFamily(Font(R.font.kanit)), true),
        FontItem("karla", "Karla", FontFamily(Font(R.font.karla)), true),
        FontItem("kaushan_script", "Kaushan Script", FontFamily(Font(R.font.kaushan_script)), true),
        FontItem("lato", "Lato", FontFamily(Font(R.font.lato)), true),
        FontItem("libre_baskerville", "Libre Baskerville", FontFamily(Font(R.font.libre_baskerville)), true),
        FontItem("libre_franklin", "Libre Franklin", FontFamily(Font(R.font.libre_franklin)), true),
        FontItem("lilita_one", "Lilita One", FontFamily(Font(R.font.lilita_one)), true),
        FontItem("lobster", "Lobster", FontFamily(Font(R.font.lobster)), true),
        FontItem("lobster_two", "Lobster Two", FontFamily(Font(R.font.lobster_two)), true),
        FontItem("lora", "Lora", FontFamily(Font(R.font.lora)), true),
        FontItem("lusitana", "Lusitana", FontFamily(Font(R.font.lusitana)), true),
        FontItem("manrope", "Manrope", FontFamily(Font(R.font.manrope)), true),
        FontItem("maven_pro", "Maven Pro", FontFamily(Font(R.font.maven_pro)), true),
        FontItem("merriweather", "Merriweather", FontFamily(Font(R.font.merriweather)), true),
        FontItem("merriweather_sans", "Merriweather Sans", FontFamily(Font(R.font.merriweather_sans)), true),
        FontItem("montserrat", "Montserrat", FontFamily(Font(R.font.montserrat)), true),
        FontItem("mulish", "Mulish", FontFamily(Font(R.font.mulish)), true),
        FontItem("nanum_gothic", "Nanum Gothic", FontFamily(Font(R.font.nanum_gothic)), true),
        FontItem("neuton", "Neuton", FontFamily(Font(R.font.neuton)), true),
        FontItem("noticia_text", "Noticia Text", FontFamily(Font(R.font.noticia_text)), true),
        FontItem("noto_sans", "Noto Sans", FontFamily(Font(R.font.noto_sans)), true),
        FontItem("noto_serif", "Noto Serif", FontFamily(Font(R.font.noto_serif)), true),
        FontItem("nunito", "Nunito", FontFamily(Font(R.font.nunito)), true),
        FontItem("nunito_sans", "Nunito Sans", FontFamily(Font(R.font.nunito_sans)), true),
        FontItem("old_standard_tt", "Old Standard TT", FontFamily(Font(R.font.old_standard_tt)), true),
        FontItem("open_sans", "Open Sans", FontFamily(Font(R.font.open_sans)), true),
        FontItem("oswald", "Oswald", FontFamily(Font(R.font.oswald)), true),
        FontItem("outfit", "Outfit", FontFamily(Font(R.font.outfit)), true),
        FontItem("overpass", "Overpass", FontFamily(Font(R.font.overpass)), true),
        FontItem("oxygen", "Oxygen", FontFamily(Font(R.font.oxygen)), true),
        FontItem("pacifico", "Pacifico", FontFamily(Font(R.font.pacifico)), true),
        FontItem("passion_one", "Passion One", FontFamily(Font(R.font.passion_one)), true),
        FontItem("pathway_gothic_one", "Pathway Gothic One", FontFamily(Font(R.font.pathway_gothic_one)), true),
        FontItem("patrick_hand", "Patrick Hand", FontFamily(Font(R.font.patrick_hand)), true),
        FontItem("permanent_marker", "Permanent Marker", FontFamily(Font(R.font.permanent_marker)), true),
        FontItem("philosopher", "Philosopher", FontFamily(Font(R.font.philosopher)), true),
        FontItem("play", "Play", FontFamily(Font(R.font.play)), true),
        FontItem("playfair_display", "Playfair Display", FontFamily(Font(R.font.playfair_display)), true),
        FontItem("playfair_display_sc", "Playfair Display SC", FontFamily(Font(R.font.playfair_display_sc)), true),
        FontItem("plus_jakarta_sans", "Plus Jakarta Sans", FontFamily(Font(R.font.plus_jakarta_sans)), true),
        FontItem("poppins", "Poppins", FontFamily(Font(R.font.poppins)), true),
        FontItem("prata", "Prata", FontFamily(Font(R.font.prata)), true),
        FontItem("prompt", "Prompt", FontFamily(Font(R.font.prompt)), true),
        FontItem("pt_sans", "PT Sans", FontFamily(Font(R.font.pt_sans)), true),
        FontItem("pt_sans_narrow", "PT Sans Narrow", FontFamily(Font(R.font.pt_sans_narrow)), true),
        FontItem("pt_serif", "PT Serif", FontFamily(Font(R.font.pt_serif)), true),
        FontItem("public_sans", "Public Sans", FontFamily(Font(R.font.public_sans)), true),
        FontItem("questrial", "Questrial", FontFamily(Font(R.font.questrial)), true),
        FontItem("quicksand", "Quicksand", FontFamily(Font(R.font.quicksand)), true),
        FontItem("rajdhani", "Rajdhani", FontFamily(Font(R.font.rajdhani)), true),
        FontItem("raleway", "Raleway", FontFamily(Font(R.font.raleway)), true),
        FontItem("red_hat_display", "Red Hat Display", FontFamily(Font(R.font.red_hat_display)), true),
        FontItem("righteous", "Righteous", FontFamily(Font(R.font.righteous)), true),
        FontItem("roboto", "Roboto", FontFamily(Font(R.font.roboto)), true),
        FontItem("roboto_condensed", "Roboto Condensed", FontFamily(Font(R.font.roboto_condensed)), true),
        FontItem("roboto_mono", "Roboto Mono", FontFamily(Font(R.font.roboto_mono)), true),
        FontItem("roboto_slab", "Roboto Slab", FontFamily(Font(R.font.roboto_slab)), true),
        FontItem("rokkitt", "Rokkitt", FontFamily(Font(R.font.rokkitt)), true),
        FontItem("rubik", "Rubik", FontFamily(Font(R.font.rubik)), true),
        FontItem("russo_one", "Russo One", FontFamily(Font(R.font.russo_one)), true),
        FontItem("sacramento", "Sacramento", FontFamily(Font(R.font.sacramento)), true),
        FontItem("saira_condensed", "Saira Condensed", FontFamily(Font(R.font.saira_condensed)), true),
        FontItem("satisfy", "Satisfy", FontFamily(Font(R.font.satisfy)), true),
        FontItem("secular_one", "Secular One", FontFamily(Font(R.font.secular_one)), true),
        FontItem("shadows_into_light", "Shadows Into Light", FontFamily(Font(R.font.shadows_into_light)), true),
        FontItem("signika", "Signika", FontFamily(Font(R.font.signika)), true),
        FontItem("slabo_27px", "Slabo 27px", FontFamily(Font(R.font.slabo_27px)), true),
        FontItem("sorts_mill_goudy", "Sorts Mill Goudy", FontFamily(Font(R.font.sorts_mill_goudy)), true),
        FontItem("source_code_pro", "Source Code Pro", FontFamily(Font(R.font.source_code_pro)), true),
        FontItem("source_sans_3", "Source Sans 3", FontFamily(Font(R.font.source_sans_3)), true),
        FontItem("source_serif_4", "Source Serif 4", FontFamily(Font(R.font.source_serif_4)), true),
        FontItem("space_grotesk", "Space Grotesk", FontFamily(Font(R.font.space_grotesk)), true),
        FontItem("space_mono", "Space Mono", FontFamily(Font(R.font.space_mono)), true),
        FontItem("spectral", "Spectral", FontFamily(Font(R.font.spectral)), true),
        FontItem("teko", "Teko", FontFamily(Font(R.font.teko)), true),
        FontItem("titillium_web", "Titillium Web", FontFamily(Font(R.font.titillium_web)), true),
        FontItem("ubuntu", "Ubuntu", FontFamily(Font(R.font.ubuntu)), true),
        FontItem("ubuntu_mono", "Ubuntu Mono", FontFamily(Font(R.font.ubuntu_mono)), true),
        FontItem("urbanist", "Urbanist", FontFamily(Font(R.font.urbanist)), true),
        FontItem("varela_round", "Varela Round", FontFamily(Font(R.font.varela_round)), true),
        FontItem("vollkorn", "Vollkorn", FontFamily(Font(R.font.vollkorn)), true),
        FontItem("work_sans", "Work Sans", FontFamily(Font(R.font.work_sans)), true),
        FontItem("yanone_kaffeesatz", "Yanone Kaffeesatz", FontFamily(Font(R.font.yanone_kaffeesatz)), true),
        FontItem("yellowtail", "Yellowtail", FontFamily(Font(R.font.yellowtail)), true),
        FontItem("zilla_slab", "Zilla Slab", FontFamily(Font(R.font.zilla_slab)), true)
    )

    private fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, "fonts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getImportedFonts(context: Context): List<FontItem> {
        val paths = getImportedFontPaths(context)
        return paths.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) {
                try {
                    val typeface = Typeface.createFromFile(file)
                    val fontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
                    FontItem(
                        id = "custom_${file.name}",
                        displayName = file.nameWithoutExtension,
                        fontFamily = fontFamily,
                        isBundled = false
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                // Clean up stale path
                removeImportedFontPath(context, path)
                null
            }
        }
    }

    fun importFont(context: Context, uri: Uri): FontItem? {
        return try {
            val contentResolver = context.contentResolver
            val fileName = getFileNameFromUri(context, uri) ?: "custom_font.ttf"
            val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fontsDir = getFontsDir(context)
            val destFile = File(fontsDir, sanitizedName)

            // Copy the font file
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            // Verify it's a valid font
            val typeface = Typeface.createFromFile(destFile)
            val fontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(typeface))

            // Save path
            addImportedFontPath(context, destFile.absolutePath)

            FontItem(
                id = "custom_${sanitizedName}",
                displayName = destFile.nameWithoutExtension,
                fontFamily = fontFamily,
                isBundled = false
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deleteImportedFont(context: Context, fontItem: FontItem): Boolean {
        val paths = getImportedFontPaths(context)
        val matchingPath = paths.find { "custom_${File(it).name}" == fontItem.id }
        if (matchingPath != null) {
            File(matchingPath).delete()
            removeImportedFontPath(context, matchingPath)
            // If this was the selected font, reset to default
            if (getSelectedFont(context) == fontItem.id) {
                setSelectedFont(context, "default")
            }
            return true
        }
        return false
    }

    fun getSelectedFontFamily(context: Context): FontFamily? {
        val fontId = getSelectedFont(context)
        if (fontId == "default") return null

        // Check bundled fonts
        bundledFonts.find { it.id == fontId }?.let { return it.fontFamily }

        // Check imported fonts
        getImportedFonts(context).find { it.id == fontId }?.let { return it.fontFamily }

        // Font not found, reset to default
        setSelectedFont(context, "default")
        return null
    }

    fun getSelectedFontName(context: Context): String {
        val fontId = getSelectedFont(context)
        if (fontId == "default") return "System Font (Default)"

        bundledFonts.find { it.id == fontId }?.let { return it.displayName }
        getImportedFonts(context).find { it.id == fontId }?.let { return it.displayName }

        return "System Font (Default)"
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
