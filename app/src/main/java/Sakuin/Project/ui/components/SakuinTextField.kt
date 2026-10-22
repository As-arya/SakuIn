package Sakuin.Project.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.ui.theme.SakuinTheme

/** Labelled text input from the Figma `TextField` component (Default / Focused / Error). */
@Composable
fun SakuinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    placeholder: String? = null,
    helper: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = SakuinTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    FieldLayout(label = label, helper = helper, isError = isError, modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = SakuinTheme.typography.bodyMedium.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                FieldBox(focused = focused, isError = isError) {
                    LeadingIcon(leadingIcon)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = SakuinTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                        innerTextField()
                    }
                }
            },
        )
    }
}

/** Read-only field that opens a picker (sheet or dialog) when tapped. */
@Composable
fun SakuinPickerField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    showChevron: Boolean = true,
) {
    FieldLayout(label = label, helper = null, isError = false, modifier = modifier) {
        FieldBox(
            focused = false,
            isError = false,
            modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
        ) {
            LeadingIcon(leadingIcon)
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = SakuinTheme.typography.bodyMedium,
                color = SakuinTheme.colors.textPrimary,
                maxLines = 1,
            )
            if (showChevron) {
                Icon(
                    painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SakuinTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun FieldLayout(
    label: String,
    helper: String?,
    isError: Boolean,
    modifier: Modifier,
    field: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xs)) {
        Text(label, style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
        field()
        if (helper != null) {
            Text(
                text = helper,
                style = SakuinTheme.typography.caption,
                color = if (isError) SakuinTheme.colors.expense else SakuinTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun FieldBox(
    focused: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = SakuinTheme.colors
    val shape = RoundedCornerShape(SakuinTheme.radius.md)
    val (borderWidth, borderColor) = when {
        isError -> 2.dp to colors.expense
        focused -> 2.dp to colors.primary
        else -> 1.dp to colors.border
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(colors.bgSurface)
            .border(borderWidth, borderColor, shape)
            .then(modifier)
            .padding(horizontal = SakuinTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun LeadingIcon(@DrawableRes icon: Int?) {
    if (icon == null) return
    Icon(
        painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = SakuinTheme.colors.textSecondary,
    )
}
