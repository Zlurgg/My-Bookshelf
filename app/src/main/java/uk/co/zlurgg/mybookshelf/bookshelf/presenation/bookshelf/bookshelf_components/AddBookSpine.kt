package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf.bookshelf_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R

@Composable
fun AddBookSpine(
    onClick: () -> Unit,
) {
    val cd = stringResource(id = R.string.cd_add_book_to_shelf)
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .semantics { contentDescription = cd }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Keep same size as BookSpine: 60x150 box with top icon
            Box(
                modifier = Modifier
                    .height(150.dp)
                    .width(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary, 
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}

@Preview
@Composable
private fun AddBookSpinePreview() {
    MaterialTheme {
        AddBookSpine(onClick = {})
    }
}
