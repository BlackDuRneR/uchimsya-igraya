package com.yasnost.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.yasnost.app.data.AppState
import com.yasnost.app.data.CheckIn
import com.yasnost.app.data.Habit
import com.yasnost.app.data.HabitType
import com.yasnost.app.data.Relapse
import com.yasnost.app.data.Repository
import com.yasnost.app.domain.DAY
import com.yasnost.app.domain.HOUR
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h900dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    companion object {
        val out = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }
        init {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
            seed(ctx)
        }

        fun seed(ctx: android.content.Context) = runBlocking {
            val now = System.currentTimeMillis()
            val today = LocalDate.now().toEpochDay()
            Repository.get(ctx).update(refreshWidgets = false) {
                AppState(
                    habits = listOf(
                        Habit("a", HabitType.ALCOHOL, "Не пью", now - 98 * DAY - 20 * HOUR - 24 * 60_000, 1700.0, 0.1, "л", now - 98 * DAY),
                        Habit("s", HabitType.SMOKING, "Не курю", now - 41 * DAY - 5 * HOUR, 1200.0, 20.0, "сиг.", now - 60 * DAY),
                    ),
                    relapses = listOf(Relapse("s", now - 41 * DAY - 5 * HOUR, 12 * DAY, "стресс на работе")),
                    checkIns = (0..20).filter { it % 7 != 3 }.map { d ->
                        CheckIn(today - d - 1, mood = 3 + (d % 3).coerceAtMost(2), craving = (1 + d / 3 + d % 2).coerceAtMost(8), cleanHabits = listOf("a", "s"),
                            note = if (d == 1) "Вечер прошёл спокойно, сходил в зал" else if (d == 5) "Друзья звали в бар — отказался" else "")
                    },
                    reasons = listOf("Ясная голова по утрам", "Хочу видеть, как растут сбережения", "Здоровое сердце"),
                    cravingsBeaten = 14,
                )
            }
        }
    }

    private fun shot(name: String) {
        rule.mainClock.autoAdvance = false
        repeat(15) { rule.mainClock.advanceTimeBy(100); org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle() }
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(Canvas(bmp)) }
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun screens() {
        shot("1_home")
        rule.onNodeWithText("Отметки").performClick(); shot("3_calendar")
        rule.onNodeWithText("Награды").performClick(); shot("4_awards")
        rule.onNodeWithText("Ещё").performClick(); shot("5_settings")
        rule.onNodeWithText("Главная").performClick()
        rule.mainClock.autoAdvance = false; repeat(15) { rule.mainClock.advanceTimeBy(100) }
        rule.onNodeWithText("Не пью").performClick(); shot("2_habit")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        repeat(10) { rule.mainClock.advanceTimeBy(100) }
        rule.onNodeWithText("SOS").performClick(); shot("6_sos")
    }

    @Test fun widgets() {
        val ctx = rule.activity
        val m = com.yasnost.app.widget.WidgetUpdater
        runBlocking {
            val s = Repository.get(ctx).current()
            val now = System.currentTimeMillis()
            val method = m.javaClass.declaredMethods
            fun render(name: String, views: RemoteViews, wDp: Int, hDp: Int) {
                val d = ctx.resources.displayMetrics.density
                val w = (wDp * d).toInt(); val h = (hDp * d).toInt()
                val parent = FrameLayout(ctx)
                val v: View = views.apply(ctx, parent)
                parent.addView(v, ViewGroup.LayoutParams(w, h))
                parent.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                parent.layout(0, 0, w, h)
                val pad = (16 * d).toInt()
                val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.drawColor(0xFF1B2440.toInt())
                c.translate(pad.toFloat(), pad.toFloat())
                parent.draw(c)
                File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            val counterLayout = method.first { it.name == "counterLayout" }.apply { isAccessible = true }
            val summary = method.first { it.name == "summaryViews" }.apply { isAccessible = true }
            render("w_counter_small_a", counterLayout.invoke(m, ctx, R.layout.widget_counter_small, 1, s.habits[0], s, now) as RemoteViews, 170, 170)
            render("w_counter_small_s", counterLayout.invoke(m, ctx, R.layout.widget_counter_small, 1, s.habits[1], s, now) as RemoteViews, 170, 170)
            render("w_counter_wide", counterLayout.invoke(m, ctx, R.layout.widget_counter_wide, 1, s.habits[0], s, now) as RemoteViews, 350, 170)
            render("w_summary", summary.invoke(m, ctx, 1, s, now) as RemoteViews, 350, 170)
        }
    }
}
