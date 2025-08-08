package uk.co.zlurgg.mybookshelf.bookshelf.presenation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import uk.co.zlurgg.mybookshelf.R

enum class ShelfMaterial(val shelfColor: Int, val shelfBackground: Color) {
    Wood(R.drawable.wood_texture_brown, Color(0xFF2B1F16));
    @Composable
    fun painter(): Painter = painterResource(shelfColor)
}
