package Sakuin.Project.ui.screens.scan

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.remote.ReceiptItem
import Sakuin.Project.data.remote.ScanError
import Sakuin.Project.ui.components.AmountInput
import Sakuin.Project.ui.components.ButtonStyle
import Sakuin.Project.ui.components.DateTimePickerDialog
import Sakuin.Project.ui.components.ReceiptPhotoDialog
import Sakuin.Project.ui.components.ReceiptThumbnail
import Sakuin.Project.ui.components.SakuinButton
import Sakuin.Project.ui.components.SakuinChip
import Sakuin.Project.ui.components.SakuinPickerField
import Sakuin.Project.ui.components.SakuinTextField
import Sakuin.Project.ui.components.SakuinTopBar
import Sakuin.Project.ui.components.WalletPickerSheet
import Sakuin.Project.ui.components.categoryIcon
import Sakuin.Project.ui.components.formatDateTime
import Sakuin.Project.ui.components.icon
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.screens.addtransaction.FormError
import Sakuin.Project.ui.theme.Palette
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import Sakuin.Project.util.parseRupiahInput
import java.io.File

// The capture step looks like a camera in both themes (Figma 09).
private val CameraBar = Palette.Gray850
private val CameraBody = Palette.Gray800
private val CameraButton = Palette.Gray700
private val CameraContent = Palette.Gray0
private val CameraMuted = Palette.Gray400
/** Off-white paper in the receipt illustrations. */
private val Paper = Color(0xFFF8F8F4)

private const val CollapsedItems = 5

/**
 * Receipt scan (Figma 09–12). Photos come from the system camera or the photo picker, are read by
 * Gemini, and land on a review screen where everything can be corrected before saving the expense.
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onAddManually: () -> Unit,
    onSaved: () -> Unit,
    onAddWallet: () -> Unit,
) {
    val viewModel = sakuinViewModel {
        ScanViewModel(
            it.receiptScanner,
            it.receiptStore,
            it.networkMonitor,
            it.walletRepository,
            it.categoryRepository,
            it.transactionRepository,
        )
    }
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val step = viewModel.step
    val context = LocalContext.current

    // Survives the activity being recreated while the camera app is open.
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = cameraUri
        if (taken && uri != null) viewModel.onPhoto(uri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhoto(uri)
    }
    val takePhoto = {
        val uri = viewModel.cameraUri()
        cameraUri = uri
        try {
            camera.launch(uri)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.scan_no_camera, Toast.LENGTH_LONG).show()
        }
    }
    val pickPhoto = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    // Back steps out of reading/review/problems to the photo choice first.
    BackHandler(enabled = step != ScanStep.Capture, onBack = viewModel::retake)

    val offline = !online && step == ScanStep.Capture
    LightSystemBarIcons(enabled = step == ScanStep.Capture && !offline)

    when {
        offline -> ProblemContent(
            icon = R.drawable.ic_wifi_off,
            title = R.string.scan_offline_title,
            body = R.string.scan_offline_body,
            onBack = onBack,
            onAddManually = onAddManually,
            onTryAgain = viewModel::refreshConnection,
        )
        step == ScanStep.Capture -> CaptureContent(
            onClose = onBack,
            onTakePhoto = takePhoto,
            onPickPhoto = pickPhoto,
            onAddManually = onAddManually,
        )
        step == ScanStep.Reading -> ReadingContent(onCancel = viewModel::retake)
        step == ScanStep.Review -> ReviewContent(viewModel, onSaved = onSaved, onAddWallet = onAddWallet)
        step is ScanStep.Failed -> {
            val problem = step.error.problem()
            ProblemContent(
                icon = problem.icon,
                title = problem.title,
                body = problem.body,
                onBack = viewModel::retake,
                onAddManually = onAddManually,
                onTryAgain = if (step.error == ScanError.NotConfigured) null else viewModel::tryAgain,
            )
        }
    }
}

/** Light status/navigation bar icons while the dark capture step is shown. */
@Composable
private fun LightSystemBarIcons(enabled: Boolean) {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val lightStatus = controller.isAppearanceLightStatusBars
        val lightNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatus
            controller.isAppearanceLightNavigationBars = lightNavigation
        }
    }
}

@Composable
private fun CaptureContent(
    onClose: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onAddManually: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBar),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = SakuinTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close), tint = CameraContent)
            }
            Text(
                stringResource(R.string.title_scan),
                modifier = Modifier.weight(1f),
                style = SakuinTheme.typography.titleMedium,
                color = CameraContent,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(48.dp))
        }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CameraBody),
            contentAlignment = Alignment.Center,
        ) {
            val frameHeight = minOf(340.dp, maxHeight * 0.7f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xl),
            ) {
                ReceiptViewfinder(Modifier.size(width = frameHeight * (240f / 340f), height = frameHeight))
                Text(stringResource(R.string.scan_hint), style = SakuinTheme.typography.bodyMedium, color = CameraMuted)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 40.dp, vertical = SakuinTheme.spacing.xl),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraSideButton(R.drawable.ic_image, stringResource(R.string.scan_pick_photo), onPickPhoto)
            ShutterButton(onTakePhoto)
            CameraSideButton(R.drawable.ic_edit, stringResource(R.string.scan_add_manually), onAddManually)
        }
    }
}

@Composable
private fun CameraSideButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(CameraButton)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), label, Modifier.size(22.dp), tint = CameraContent)
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    val label = stringResource(R.string.scan_take_photo)
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(4.dp, CameraContent, CircleShape)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(CameraContent),
        )
    }
}

/** Corner brackets around a slightly tilted receipt, as in the Figma capture frame. */
@Composable
private fun ReceiptViewfinder(modifier: Modifier) {
    Canvas(modifier) {
        val arm = size.minDimension * 0.17f
        val radius = 14.dp.toPx()
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        // One bracket per corner: (corner, direction along x, direction along y).
        listOf(Triple(Offset(0f, 0f), 1f, 1f), Triple(Offset(w, 0f), -1f, 1f), Triple(Offset(0f, h), 1f, -1f), Triple(Offset(w, h), -1f, -1f))
            .forEach { (corner, dx, dy) ->
                val path = Path().apply {
                    moveTo(corner.x, corner.y + dy * arm)
                    lineTo(corner.x, corner.y + dy * radius)
                    quadraticTo(corner.x, corner.y, corner.x + dx * radius, corner.y)
                    lineTo(corner.x + dx * arm, corner.y)
                }
                drawPath(path, CameraContent, style = stroke)
            }
        rotate(degrees = 3f) {
            val paper = Size(w * 0.78f, h * 0.78f)
            val topLeft = Offset((w - paper.width) / 2, (h - paper.height) / 2)
            drawRoundRect(Paper, topLeft, paper, CornerRadius(6.dp.toPx()))
            drawReceiptLines(topLeft, paper)
        }
    }
}

/** A header line and a column of print lines inside a receipt drawn at [topLeft] with [paper] size. */
private fun DrawScope.drawReceiptLines(topLeft: Offset, paper: Size) {
    val lineHeight = paper.height * 0.024f
    val corner = CornerRadius(lineHeight / 2)
    fun line(y: Float, widthFraction: Float, color: Color) {
        val width = paper.width * widthFraction
        drawRoundRect(color, Offset(topLeft.x + (paper.width - width) / 2, topLeft.y + y), Size(width, lineHeight), corner)
    }
    line(paper.height * 0.07f, 0.42f, Palette.Gray400)
    line(paper.height * 0.13f, 0.28f, Palette.Gray200)
    listOf(0.84f, 0.68f, 0.78f, 0.58f, 0.86f, 0.72f, 0.52f, 0.8f, 0.42f).forEachIndexed { i, fraction ->
        line(paper.height * (0.21f + i * 0.065f), fraction, Palette.Gray400.copy(alpha = 0.45f))
    }
}

@Composable
private fun ReadingContent(onCancel: () -> Unit) {
    val colors = SakuinTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .systemBarsPadding(),
    ) {
        SakuinTopBar(title = stringResource(R.string.title_scan), onBack = onCancel)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = SakuinTheme.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm, Alignment.CenterVertically),
        ) {
            ScanningIllustration(Modifier.size(width = 180.dp, height = 240.dp))
            Spacer(Modifier.height(SakuinTheme.spacing.lg))
            Text(stringResource(R.string.scan_reading_title), style = SakuinTheme.typography.titleMedium, color = colors.textPrimary)
            Text(
                stringResource(R.string.scan_reading_body),
                style = SakuinTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        SakuinButton(
            text = stringResource(R.string.action_cancel),
            onClick = onCancel,
            style = ButtonStyle.Outline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
        )
    }
}

/** A receipt with a scan line sweeping over it while Gemini reads the photo. */
@Composable
private fun ScanningIllustration(modifier: Modifier) {
    val colors = SakuinTheme.colors
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scanLine",
    )
    Canvas(modifier) {
        drawRoundRect(colors.bgSurfaceVariant, cornerRadius = CornerRadius(24.dp.toPx()))
        val paper = Size(size.width * 0.78f, size.height - 32.dp.toPx())
        val topLeft = Offset((size.width - paper.width) / 2, 16.dp.toPx())
        drawRoundRect(Paper, topLeft, paper, CornerRadius(4.dp.toPx()))
        drawReceiptLines(topLeft, paper)
        val lineHeight = 3.dp.toPx()
        drawRect(colors.primary, Offset(0f, size.height * progress - lineHeight / 2), Size(size.width, lineHeight))
    }
}

private data class Problem(@param:DrawableRes val icon: Int, @param:StringRes val title: Int, @param:StringRes val body: Int)

private fun ScanError.problem(): Problem = when (this) {
    ScanError.NotConfigured -> Problem(R.drawable.ic_settings, R.string.scan_error_setup_title, R.string.scan_error_setup_body)
    ScanError.Image -> Problem(R.drawable.ic_image, R.string.scan_error_image_title, R.string.scan_error_image_body)
    ScanError.Network -> Problem(R.drawable.ic_wifi_off, R.string.scan_error_network_title, R.string.scan_error_network_body)
    ScanError.Timeout -> Problem(R.drawable.ic_scan, R.string.scan_error_timeout_title, R.string.scan_error_timeout_body)
    ScanError.Busy -> Problem(R.drawable.ic_scan, R.string.scan_error_busy_title, R.string.scan_error_busy_body)
    ScanError.Server -> Problem(R.drawable.ic_scan, R.string.scan_error_server_title, R.string.scan_error_server_body)
    ScanError.NotReceipt -> Problem(R.drawable.ic_receipt, R.string.scan_error_not_receipt_title, R.string.scan_error_not_receipt_body)
    ScanError.Unreadable -> Problem(R.drawable.ic_receipt, R.string.scan_error_unreadable_title, R.string.scan_error_unreadable_body)
}

/** Offline and error states (Figma 12): what happened, then manual entry or another try. */
@Composable
private fun ProblemContent(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes body: Int,
    onBack: () -> Unit,
    onAddManually: () -> Unit,
    onTryAgain: (() -> Unit)?,
) {
    val colors = SakuinTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .systemBarsPadding(),
    ) {
        SakuinTopBar(title = stringResource(R.string.title_scan), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.xxl, vertical = SakuinTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(SakuinTheme.radius.lg))
                    .background(colors.bgSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, Modifier.size(44.dp), tint = colors.textPrimary)
            }
            Text(
                stringResource(title),
                style = SakuinTheme.typography.headlineLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(body),
                style = SakuinTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(SakuinTheme.spacing.xs))
            SakuinButton(
                text = stringResource(R.string.scan_add_manually),
                onClick = onAddManually,
                modifier = Modifier.fillMaxWidth(),
            )
            if (onTryAgain != null) {
                SakuinButton(
                    text = stringResource(R.string.scan_try_again),
                    onClick = onTryAgain,
                    style = ButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Review Receipt (Figma 11): the scanned values, all editable, then Save as an expense. */
@Composable
private fun ReviewContent(viewModel: ScanViewModel, onSaved: () -> Unit, onAddWallet: () -> Unit) {
    val colors = SakuinTheme.colors
    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var pickingWallet by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var viewingPhoto by remember { mutableStateOf(false) }
    val photo = viewModel.photo
    val wallet = wallets.firstOrNull { it.wallet.id == viewModel.walletId }
    val amount = parseRupiahInput(viewModel.amountDigits) ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgSurface)
            .systemBarsPadding()
            .imePadding(),
    ) {
        SakuinTopBar(
            title = stringResource(R.string.title_review_receipt),
            onBack = viewModel::retake,
            actions = {
                IconButton(onClick = viewModel::retake) {
                    Icon(painterResource(R.drawable.ic_camera), stringResource(R.string.scan_retake), tint = colors.textPrimary)
                }
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg),
        ) {
            ReceiptHeader(
                photo = photo,
                merchant = viewModel.merchant,
                occurredAt = viewModel.occurredAt,
                onOpenPhoto = { viewingPhoto = true },
                onChangeDate = { pickingDate = true },
            )
            Column {
                AmountInput(
                    digits = viewModel.amountDigits,
                    onDigitsChange = viewModel::onAmountChange,
                    color = colors.expense,
                    label = stringResource(R.string.scan_total),
                )
                if (viewModel.error == FormError.AmountRequired) {
                    ErrorText(R.string.error_amount_required, Modifier.fillMaxWidth(), center = true)
                }
            }
            if (viewModel.items.isNotEmpty()) ItemsCard(viewModel.items)
            CategoryRow(
                categories = categories,
                selectedId = viewModel.categoryId,
                suggestedId = viewModel.suggestedCategoryId,
                onSelect = viewModel::selectCategory,
            )
            if (viewModel.error == FormError.CategoryRequired) ErrorText(R.string.error_category_required)
            SakuinTextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                label = stringResource(R.string.field_item_name),
                leadingIcon = R.drawable.ic_tag,
                placeholder = stringResource(R.string.field_item_name_placeholder),
                isError = viewModel.error == FormError.TitleRequired,
                helper = if (viewModel.error == FormError.TitleRequired) stringResource(R.string.error_item_name_required) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            )
            Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
                SakuinPickerField(
                    value = wallet?.wallet?.name.orEmpty(),
                    label = stringResource(R.string.field_pay_from),
                    onClick = { pickingWallet = true },
                    leadingIcon = wallet?.wallet?.type?.icon ?: R.drawable.ic_wallet,
                )
                if (viewModel.error == FormError.WalletRequired) {
                    ErrorText(R.string.need_wallet)
                } else if (wallet != null && amount > 0 && wallet.balance - amount < 0) {
                    Text(
                        stringResource(R.string.transfer_negative_warning, wallet.wallet.name),
                        style = SakuinTheme.typography.caption,
                        color = colors.warning,
                    )
                }
            }
            Spacer(Modifier.height(SakuinTheme.spacing.xs))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        ) {
            SakuinButton(
                text = stringResource(R.string.scan_retake),
                onClick = viewModel::retake,
                style = ButtonStyle.Outline,
                modifier = Modifier.weight(1f),
            )
            SakuinButton(
                text = stringResource(R.string.scan_save),
                onClick = { viewModel.save(onSaved) },
                enabled = !viewModel.saving,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (pickingWallet) {
        WalletPickerSheet(
            wallets = wallets,
            selectedId = viewModel.walletId,
            onSelect = { id ->
                viewModel.selectWallet(id)
                pickingWallet = false
            },
            onDismiss = { pickingWallet = false },
            onAddWallet = {
                pickingWallet = false
                onAddWallet()
            },
        )
    }
    if (pickingDate) {
        DateTimePickerDialog(
            millis = viewModel.occurredAt,
            onChange = { viewModel.occurredAt = it },
            onDismiss = { pickingDate = false },
        )
    }
    if (viewingPhoto && photo != null) {
        ReceiptPhotoDialog(file = photo, onDismiss = { viewingPhoto = false })
    }
}

@Composable
private fun ReceiptHeader(
    photo: File?,
    merchant: String?,
    occurredAt: Long,
    onOpenPhoto: () -> Unit,
    onChangeDate: () -> Unit,
) {
    val colors = SakuinTheme.colors
    Row(
        modifier = Modifier.padding(top = SakuinTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReceiptThumbnail(
            file = photo,
            onClick = if (photo != null) onOpenPhoto else null,
            modifier = Modifier.size(width = 52.dp, height = 60.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                merchant ?: stringResource(R.string.scan_receipt),
                style = SakuinTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(SakuinTheme.radius.sm))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.scan_change_date), onClick = onChangeDate)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatDateTime(occurredAt), style = SakuinTheme.typography.labelMedium, color = colors.textSecondary)
                Icon(painterResource(R.drawable.ic_edit), null, Modifier.size(14.dp), tint = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ItemsCard(items: List<ReceiptItem>) {
    val colors = SakuinTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (expanded) items else items.take(CollapsedItems)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SakuinTheme.radius.md))
            .background(colors.bgApp)
            .padding(horizontal = 14.dp, vertical = SakuinTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
    ) {
        shown.forEach { item ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md)) {
                Text(
                    if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name,
                    modifier = Modifier.weight(1f),
                    style = SakuinTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                item.price?.let { Text(formatRupiah(it), style = SakuinTheme.typography.bodyMedium, color = colors.textSecondary) }
            }
        }
        if (!expanded && items.size > CollapsedItems) {
            Text(
                stringResource(R.string.scan_show_all_items, items.size),
                modifier = Modifier
                    .clickable(role = Role.Button) { expanded = true }
                    .padding(vertical = SakuinTheme.spacing.xs),
                style = SakuinTheme.typography.labelMedium,
                color = colors.textBrand,
            )
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    suggestedId: Long?,
    onSelect: (Long) -> Unit,
) {
    // The AI pick comes first; the order stays put while the user changes the selection.
    val ordered = remember(categories, suggestedId) { categories.sortedByDescending { it.id == suggestedId } }
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.field_category), style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
            if (selectedId != null && selectedId == suggestedId) {
                Icon(
                    painterResource(R.drawable.ic_sparkles),
                    stringResource(R.string.scan_ai_suggested),
                    Modifier.size(14.dp),
                    tint = SakuinTheme.colors.primary,
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
        ) {
            ordered.forEach { category ->
                SakuinChip(
                    label = category.name,
                    selected = category.id == selectedId,
                    onClick = { onSelect(category.id) },
                    icon = categoryIcon(category.iconKey),
                )
            }
        }
    }
}

@Composable
private fun ErrorText(@StringRes text: Int, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        stringResource(text),
        modifier = modifier,
        style = SakuinTheme.typography.caption,
        color = SakuinTheme.colors.expense,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
    )
}
