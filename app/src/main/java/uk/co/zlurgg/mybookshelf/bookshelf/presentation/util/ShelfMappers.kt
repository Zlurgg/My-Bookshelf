package uk.co.zlurgg.mybookshelf.bookshelf.presentation.util

import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

fun ShelfStyle.toMaterial(): ShelfMaterial = when (this) {
    ShelfStyle.DarkWood -> ShelfMaterial.DarkWood
    ShelfStyle.SliverMetal -> ShelfMaterial.SliverMetal
    ShelfStyle.WhiteMetal -> ShelfMaterial.WhiteMetal
    ShelfStyle.GreyMetal -> ShelfMaterial.GreyMetal
    ShelfStyle.DarkGreyMetal -> ShelfMaterial.DarkGreyMetal
}
