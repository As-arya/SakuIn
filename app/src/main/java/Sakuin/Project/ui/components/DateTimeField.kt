package Sakuin.Project.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import Sakuin.Project.R
import Sakuin.Project.ui.theme.SakuinTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

fun formatDateTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormat.format(Instant.ofEpochMilli(millis).atZone(zone))

/** Date + time field: tapping opens a date picker, then a time picker. */
@Composable
fun DateTimeField(
    millis: Long,
    onChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }

    SakuinPickerField(
        value = formatDateTime(millis),
        label = label,
        onClick = { picking = true },
        modifier = modifier,
        leadingIcon = R.drawable.ic_calendar,
        showChevron = false,
    )

    if (picking) DateTimePickerDialog(millis = millis, onChange = onChange, onDismiss = { picking = false })
}

/** A date picker followed by a time picker; [onDismiss] runs once either is closed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(millis: Long, onChange: (Long) -> Unit, onDismiss: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val current = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    val date = pickedDate
    if (date == null) {
        // The Material date picker works with UTC midnight millis.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val selected = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    if (selected != null) pickedDate = selected else onDismiss()
                }) { Text(stringResource(R.string.action_next)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
            colors = DatePickerDefaults.colors(containerColor = SakuinTheme.colors.bgSurface),
        ) {
            DatePicker(state = state)
        }
    } else {
        val state = rememberTimePickerState(initialHour = current.hour, initialMinute = current.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val picked = LocalDateTime.of(date, LocalTime.of(state.hour, state.minute))
                    onChange(picked.atZone(zone).toInstant().toEpochMilli())
                    onDismiss()
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
            containerColor = SakuinTheme.colors.bgSurface,
            text = { TimePicker(state = state) },
        )
    }
}
