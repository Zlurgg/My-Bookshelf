package uk.co.zlurgg.mybookshelf.bookshelf.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import uk.co.zlurgg.mybookshelf.R

enum class ShelfMaterial(val shelfColor: Int, val shelfBackground: Color) {
    DarkWood(R.drawable.wood_textured_brown, Color(0xFF2B1F16)),
    SliverMetal(R.drawable.metal_textured_sliver, Color(0xFFCEC5C1)),
    WhiteMetal(R.drawable.metal_plain_white, Color(0xFFFAF0F0)),
    GreyMetal(R.drawable.metal_burnished_grey, Color(0xFF575050)),
    DarkGreyMetal(R.drawable.metal_burnished_dark_grey, Color(0xFF282424));
    @Composable
    fun painter(): Painter = painterResource(shelfColor)
}
