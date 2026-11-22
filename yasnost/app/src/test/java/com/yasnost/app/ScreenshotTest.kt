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
import androidx.compose.ui.test.performScrollTo
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
@Config(sdk = [35], qualifiers = "w412dp-h2600dp-xhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    companion object {
        val out = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }
        init {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
            seed(ctx)
        }

        fun cycle(today: Long, female: Boolean): List<com.yasnost.app.data.CycleDay> {
            val starts = if (female) listOf(today - 59, today - 31, today - 3) else listOf(today - 62, today - 34, today - 6)
            val syms = listOf(listOf("Спазмы", "Усталость"), listOf("Спазмы", "Боль в пояснице"), listOf("Головная боль"), listOf("Вздутие"), emptyList())
            return starts.flatMap { st ->
                (0 until 5).map { i ->
                    com.yasnost.app.data.CycleDay(st + i, listOf(com.yasnost.app.data.Flow.MEDIUM, com.yasnost.app.data.Flow.HEAVY, com.yasnost.app.data.Flow.MEDIUM, com.yasnost.app.data.Flow.LIGHT, com.yasnost.app.data.Flow.LIGHT)[i],
                        pain = listOf(6, 7, 4, 2, 1)[i], symptoms = syms[i], mood = listOf(2, 2, 3, 4, 4)[i], meds = if (i < 2) listOf("Ибупрофен", "Грелка") else emptyList())
                }.filter { it.day <= today }
            } + listOf(com.yasnost.app.data.CycleDay(today - 8, null, 0, listOf("Тяга к сладкому", "Раздражительность"), 3))
        }

        fun seedProfile(ctx: android.content.Context, female: Boolean, done: Boolean = true) = runBlocking {
            val today = LocalDate.now().toEpochDay()
            Repository.get(ctx).update(refreshWidgets = false) {
                it.copy(
                    profile = com.yasnost.app.data.Profile(if (female) "Аня" else "Арман", if (female) com.yasnost.app.data.Gender.FEMALE else com.yasnost.app.data.Gender.MALE, if (female) "" else "Аня", done),
                    cycle = cycle(today, female),
                    periodClosedStart = if (female) -1 else today - 6,
                )
            }
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
                            triggers = listOf(listOf("Стресс"), listOf("Компания", "Выходные"), listOf("Усталость"), listOf("Стресс", "Скука"))[d % 4],
                            note = if (d == 1) "Вечер прошёл спокойно, сходил в зал" else if (d == 5) "Друзья звали в бар — отказался" else "")
                    },
                    reasons = listOf("Ясная голова по утрам", "Хочу видеть, как растут сбережения", "Здоровое сердце"),
                    cravingsBeaten = 14,
                    profile = com.yasnost.app.data.Profile("Арман", com.yasnost.app.data.Gender.MALE, "Аня", true),
                    cycle = cycle(today, false),
                    periodClosedStart = today - 6,
                    goals = listOf(
                        com.yasnost.app.data.Goal("g1", "Наушники", 90_000.0, now - 50 * DAY),
                        com.yasnost.app.data.Goal("g2", "Отпуск в Турции", 500_000.0, now - 40 * DAY),
                    ),
                    letter = "Привет. Если ты читаешь это — сейчас трудно. Вспомни, как болела голова по утрам и как стыдно было перед собой. Ты уже прошёл больше, чем думаешь.",
                    intimacy = (0..40).mapNotNull { d ->
                        val day = today - d
                        when {
                            d % 9 == 4 -> com.yasnost.app.data.IntimacyEntry(day, com.yasnost.app.data.SexStatus.UNPROTECTED, 1, false, 4, alcohol = d > 30)
                            d % 4 == 1 -> com.yasnost.app.data.IntimacyEntry(day, com.yasnost.app.data.SexStatus.PROTECTED, 1, false, 4)
                            d % 5 == 2 -> com.yasnost.app.data.IntimacyEntry(day, com.yasnost.app.data.SexStatus.NONE, 0, true, 3)
                            d % 3 == 0 -> com.yasnost.app.data.IntimacyEntry(day, com.yasnost.app.data.SexStatus.NONE, 0, false, 2 + d % 3)
                            else -> null
                        }
                    },
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

    private fun settle() { rule.mainClock.autoAdvance = false; repeat(15) { rule.mainClock.advanceTimeBy(100) } }

    private fun scrollShot(name: String, text: String) {
        rule.onNodeWithText(text, substring = true).performScrollTo(); shot(name)
    }

    @Test fun screens() {
        seedProfile(ApplicationProvider.getApplicationContext(), female = false); settle()
        shot("01_home")
        rule.onNodeWithText("Отметки").performClick(); settle(); shot("03_diary")
        rule.onNodeWithText("Близость").performClick(); settle(); shot("04_intimacy")
        rule.onNodeWithText("Награды").performClick(); settle(); shot("07_awards")
        rule.onNodeWithText("Особые", substring = true).performClick(); settle(); shot("08_awards_special")
        rule.onNodeWithText("Ещё").performClick(); settle(); shot("09_settings")
        rule.onNodeWithText("Деньги и копилка мечты").performClick(); settle(); shot("10_money")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }; settle()
        rule.onNodeWithText("Главная").performClick(); settle()
        rule.onNodeWithText("Не пью").performClick(); settle(); shot("12_habit")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }; settle()
        rule.onNodeWithText("SOS").performClick(); settle(); shot("15_sos")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }; settle()
        rule.onNodeWithText("Отметки").performClick(); settle()
        rule.onNodeWithText("Цикл").performClick(); settle(); shot("16_cycle_male")
    }

    @Test fun female() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        seedProfile(ctx, female = true, done = false); settle(); shot("20_welcome")
        rule.onNodeWithText("Женский").performClick(); settle(); rule.onNodeWithText("Далее").performClick(); settle(); shot("21_welcome_cycle")
        seedProfile(ctx, female = true, done = true); settle(); shot("22_home_female")
        rule.onNodeWithText("Отметки").performClick(); settle()
        rule.onNodeWithText("Цикл").performClick(); settle(); shot("23_cycle_female")
        rule.onNodeWithText("Обезболивающие (НПВП)").performClick(); settle(); shot("24_cycle_guide")
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
