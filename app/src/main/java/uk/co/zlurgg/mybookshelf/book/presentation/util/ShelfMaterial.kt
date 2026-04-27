package uk.co.zlurgg.mybookshelf.book.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle

enum class ShelfMaterial(
    val smallRes: Int,
    val mediumRes: Int,
    val largeRes: Int,
    val shelfBackground: Color
) {
    DarkWood(
        R.drawable.shelf_wood_textured_brown_small,
        R.drawable.shelf_wood_textured_brown_medium,
        R.drawable.shelf_wood_textured_brown_large,
        Color(0xFF2B1F16)
    ),
    SilverMetal(
        R.drawable.shelf_metal_textured_sliver_small,
        R.drawable.shelf_metal_textured_sliver_medium,
        R.drawable.shelf_metal_textured_sliver_large,
        Color(0xFFCEC5C1)
    ),
    WhiteMetal(
        R.drawable.shelf_metal_plain_white_small,
        R.drawable.shelf_metal_plain_white_medium,
        R.drawable.shelf_metal_plain_white_large,
        Color(0xFFFAF0F0)
    ),
    GreyMetal(
        R.drawable.shelf_metal_burnished_grey_small,
        R.drawable.shelf_metal_burnished_grey_medium,
        R.drawable.shelf_metal_burnished_grey_large,
        Color(0xFF575050)
    ),
    DarkGreyMetal(
        R.drawable.shelf_metal_burnished_dark_grey_small,
        R.drawable.shelf_metal_burnished_dark_grey_medium,
        R.drawable.shelf_metal_burnished_dark_grey_large,
        Color(0xFF282424)
    );

    @Composable
    fun painterSmall(): Painter = painterResource(smallRes)

    @Composable
    fun painterMedium(): Painter = painterResource(mediumRes)

    @Composable
    fun painterLarge(): Painter = painterResource(largeRes)

    companion object {
        fun fromShelfStyle(shelfStyle: ShelfStyle): ShelfMaterial {
            return when (shelfStyle) {
                ShelfStyle.DarkWood -> DarkWood
                ShelfStyle.SilverMetal -> SilverMetal
                ShelfStyle.WhiteMetal -> WhiteMetal
                ShelfStyle.GreyMetal -> GreyMetal
                ShelfStyle.DarkGreyMetal -> DarkGreyMetal
            }
        }
    }
}
