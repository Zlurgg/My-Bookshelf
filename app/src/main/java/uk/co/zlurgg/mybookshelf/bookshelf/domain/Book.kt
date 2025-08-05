package uk.co.zlurgg.mybookshelf.bookshelf.domain

import androidx.compose.ui.graphics.Color

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val spineImageUrl: String,
    val fullImageUrl: String,
    val blurb: String,
    val purchased: Boolean,
    val affiliateLink: String,
    val spineColor: Color
)
