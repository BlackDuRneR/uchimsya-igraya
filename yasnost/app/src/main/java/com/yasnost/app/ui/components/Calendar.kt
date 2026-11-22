package com.yasnost.app.ui.components

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"))

/** Месячный календарь в стеклянной карточке. */
@Composable
fun MonthCalendar(month: YearMonth, onMonth: (YearMonth) -> Unit, footer: @Composable () -> Unit = {}, cell: @Composable (LocalDate) -> Unit) {
    Glass(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonth(month.minusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Y.Text2) }
            Text(
                monthFmt.format(month).replaceFirstChar { it.uppercase() },
                style = YType.Sub, color = Y.Text, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onMonth(month.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Y.Text2) }
        }
        Row(Modifier.padding(top = 6.dp, bottom = 6.dp)) {
            listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс").forEach {
                Text(it, style = YType.Label, color = Y.Text3, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val offset = month.atDay(1).dayOfWeek.value - 1
        val total = month.lengthOfMonth()
        val cells = ((offset + total + 6) / 7) * 7
        for (week in 0 until cells / 7) {
            Row {
                for (d in 0 until 7) {
                    val idx = week * 7 + d - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (idx in 1..total) cell(month.atDay(idx))
                    }
                }
            }
        }
        footer()
    }
}

/** Переключатель вкладок-«таблеток». */
@Composable
fun Segmented(items: List<String>, selected: Int, accent: Accent, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0x12FFFFFF))
            .padding(4.dp)
    ) {
        items.forEachIndexed { i, t ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .then(if (sel) Modifier.background(accent.brush) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(t, style = YType.Sub.copy(fontSize = YType.Body.fontSize), color = if (sel) Color(0xFF0B0C14) else Y.Text2)
            }
        }
    }
}

/** Набор чипов с множественным выбором. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsMulti(options: List<String>, selected: Set<String>, accent: Accent, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> Chip(o, o in selected, accent) { onToggle(o) } }
    }
}

object Session {
    var intimacyUnlocked = false
}

/** Отпечаток / лицо / PIN устройства. Если защита не настроена — пропускаем. */
fun authenticate(activity: FragmentActivity, title: String, onOk: () -> Unit) {
    val auth = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    if (BiometricManager.from(activity).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
        Toast.makeText(activity, "На телефоне не настроена блокировка экрана — раздел открыт без проверки", Toast.LENGTH_LONG).show()
        onOk(); return
    }
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Подтверди, что это ты")
            .setAllowedAuthenticators(auth)
            .build()
    )
}
