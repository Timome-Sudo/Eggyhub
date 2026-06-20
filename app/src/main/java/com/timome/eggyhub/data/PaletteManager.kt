package com.timome.eggyhub.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedColors(
    val primary: Int? = null,
    val primaryDark: Int? = null,
    val primaryLight: Int? = null,
    val secondary: Int? = null,
    val tertiary: Int? = null
)

class PaletteManager(private val context: Context) {
    
    suspend fun extractColorsFromBitmap(bitmap: Bitmap): ExtractedColors = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()
        
        ExtractedColors(
            primary = palette.vibrantSwatch?.rgb,
            primaryDark = palette.darkVibrantSwatch?.rgb,
            primaryLight = palette.lightVibrantSwatch?.rgb,
            secondary = palette.mutedSwatch?.rgb,
            tertiary = palette.lightMutedSwatch?.rgb
        )
    }
    
    suspend fun extractColorsFromResource(resId: Int): ExtractedColors = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        extractColorsFromBitmap(bitmap)
    }
    
    fun getSwatchInfo(swatch: Swatch?): String {
        return if (swatch != null) {
            "RGB: ${String.format("#%06X", 0xFFFFFF and swatch.rgb)}, " +
            "Title Text: ${String.format("#%06X", 0xFFFFFF and swatch.titleTextColor)}, " +
            "Body Text: ${String.format("#%06X", 0xFFFFFF and swatch.bodyTextColor)}"
        } else {
            "No swatch available"
        }
    }
}
