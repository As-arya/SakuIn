package Sakuin.Project.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import Sakuin.Project.R
import Sakuin.Project.util.MAX_AMOUNT_DIGITS
import Sakuin.Project.ui.theme.SakuinTheme

/** Shows a digits-only string with "." thousands separators: "1250000" → "1.250.000". */
object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = StringBuilder()
        val originalToTransformed = IntArray(digits.length + 1)
        digits.forEachIndexed { i, c ->
            if (i > 0 && (digits.length - i) % 3 == 0) out.append('.')
            originalToTransformed[i] = out.length
            out.append(c)
        }
        originalToTransformed[digits.length] = out.length
        val transformedToOriginal = IntArray(out.length + 1)
        var digitsSeen = 0
        for (t in 0..out.length) {
            transformedToOriginal[t] = digitsSeen
            if (t < out.length && out[t] != '.') digitsSeen++
        }
        return TransformedText(
            AnnotatedString(out.toString()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = originalToTransformed[offset.coerceIn(0, digits.length)]
                override fun transformedToOriginal(offset: Int) = transformedToOriginal[offset.coerceIn(0, out.length)]
            },
        )
    }
}

/** Keeps only digits (no leading zeros) and caps the length, for amount fields. */
fun sanitizeAmountInput(text: String): String =
    text.filter(Char::isDigit).trimStart('0').take(MAX_AMOUNT_DIGITS)

/** The big centered "Rp 18.000" amount entry at the top of transaction forms. */
@Composable
fun AmountInput(
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SakuinTheme.colors.textPrimary,
    label: String = stringResource(R.string.field_amount),
) {
    val colors = SakuinTheme.colors
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            // The field itself is only as wide as its text, so the whole block focuses it.
            .clickable(interactionSource = null, indication = null) { focusRequester.requestFocus() }
            .padding(vertical = SakuinTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xs),
    ) {
        Text(label, style = SakuinTheme.typography.labelMedium, color = colors.textSecondary)
        Row(
            horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Rp", style = SakuinTheme.typography.titleMedium, color = colors.textSecondary)
            Box(contentAlignment = Alignment.CenterStart) {
                if (digits.isEmpty()) {
                    Text("0", style = SakuinTheme.typography.displayAmount, color = colors.border)
                }
                BasicTextField(
                    value = digits,
                    onValueChange = { onDigitsChange(sanitizeAmountInput(it)) },
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = SakuinTheme.typography.displayAmount.copy(color = color),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    visualTransformation = ThousandsSeparatorTransformation,
                )
            }
        }
    }
}
