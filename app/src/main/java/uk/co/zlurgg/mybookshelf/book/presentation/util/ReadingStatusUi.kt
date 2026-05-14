package uk.co.zlurgg.mybookshelf.book.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus

@Composable
fun ReadingStatus.toDisplayString(): String = when (this) {
    ReadingStatus.NOT_READ -> stringResource(R.string.reading_status_not_read)
    ReadingStatus.READING -> stringResource(R.string.reading_status_reading)
    ReadingStatus.FINISHED -> stringResource(R.string.reading_status_finished)
}
