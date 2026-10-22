package Sakuin.Project.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import Sakuin.Project.R
import Sakuin.Project.data.local.sampleSizeFor
import Sakuin.Project.ui.theme.SakuinTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes [file] off the main thread, subsampled so its long side stays near [maxSidePx]. */
@Composable
fun rememberReceiptBitmap(file: File?, maxSidePx: Int): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file, maxSidePx) {
        value = file?.let { withContext(Dispatchers.IO) { decodeSampled(it, maxSidePx) } }
    }
    return bitmap
}

private fun decodeSampled(file: File, maxSidePx: Int): ImageBitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0) return null
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSidePx) }
    return BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
}

/** Small cropped preview of a receipt photo; shows a receipt icon until (or unless) it loads. */
@Composable
fun ReceiptThumbnail(file: File?, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val bitmap = rememberReceiptBitmap(file, maxSidePx = 256)
    val shape = RoundedCornerShape(SakuinTheme.radius.sm)
    Box(
        modifier = modifier
            .clip(shape)
            .background(SakuinTheme.colors.bgSurfaceVariant)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.receipt_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(painterResource(R.drawable.ic_receipt), null, Modifier.size(20.dp), tint = SakuinTheme.colors.textSecondary)
        }
    }
}

/** Full-screen receipt photo; tap anywhere or the close button to dismiss. */
@Composable
fun ReceiptPhotoDialog(file: File, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        // Edge to edge, so the dark backdrop also covers the status and navigation bars.
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val bitmap = rememberReceiptBitmap(file, maxSidePx = 2048)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss)
                .safeDrawingPadding(),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.receipt_photo),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(SakuinTheme.spacing.lg),
                    contentScale = ContentScale.Fit,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close), tint = Color.White)
            }
        }
    }
}
