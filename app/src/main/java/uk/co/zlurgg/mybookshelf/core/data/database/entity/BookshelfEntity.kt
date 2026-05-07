package uk.co.zlurgg.mybookshelf.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(value = ["clubCode"])]
)
data class BookshelfEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shelfMaterial: String,
    val position: Int = 0,
    val isTidyMode: Boolean = false,

    // Owner identity: null = personal, "__system_tutorial__" = system, userId = club shelf
    val ownerId: String? = null,

    // Book Club metadata (collaborative sharing)
    val isBookClub: Boolean = false,
    val clubCode: String? = null,
    val clubCreatorId: String? = null,
)
