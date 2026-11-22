package com.yasnost.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasnost.app.data.Repository
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.stats
import com.yasnost.app.ui.components.AuroraBackground
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.HabitBadge
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.YasnostTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val repo = Repository.get(this)

        setContent {
            YasnostTheme {
                val state by repo.state.collectAsStateWithLifecycle(initialValue = null)
                val scope = rememberCoroutineScope()
                fun pick(habitId: String?) {
                    if (habitId != null) WidgetUpdater.prefs(this).edit().putString("w_$id", habitId).apply()
                    scope.launch {
                        WidgetUpdater.renderById(this@WidgetConfigActivity, id)
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                        finish()
                    }
                }
                AuroraBackground {
                    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
                        Text("ВИДЖЕТ", style = YType.Label, color = Y.Text3)
                        Text("Что показывать?", style = YType.Big, color = Y.Text, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
                        val s = state ?: return@Column
                        if (s.habits.isEmpty()) {
                            Text("Сначала добавь привычку в приложении.", style = YType.Body, color = Y.Text2)
                            Spacer(Modifier.height(16.dp))
                            Glass(Modifier.fillMaxWidth(), onClick = { pick(null) }) { Text("Готово", style = YType.Sub, color = Y.Text) }
                        }
                        val now = System.currentTimeMillis()
                        s.habits.forEach { h ->
                            Glass(Modifier.fillMaxWidth().padding(bottom = 12.dp), onClick = { pick(h.id) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HabitBadge(h.type, 44.dp)
                                    Column(Modifier.padding(start = 14.dp)) {
                                        Text(h.title, style = YType.Sub, color = Y.Text)
                                        val d = h.stats(now).days
                                        Text("$d ${Fmt.days(d)}", style = YType.Body, color = Y.Text2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
