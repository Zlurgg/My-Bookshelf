package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import uk.co.zlurgg.mybookshelf.R

@Composable
fun LoadImage(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    SubcomposeAsyncImage(
        modifier = modifier,
        model = imageUrl,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    contentDescription = stringResource(R.string.error_image),
                    painter = painterResource(id = R.drawable.ic_error)
                )
            }
        },
        contentDescription = title,
    )
}
