package app.ryadom

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import com.github.takahirom.roborazzi.captureRoboImage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Сквозной прогон настоящего приложения на JVM: Hilt, навигация, хранилище, демо-семья.
 * Каждый шаг сохраняет снимок экрана в design/screenshots — по ним проверяется вёрстка.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [35], qualifiers = "ru-w360dp-h800dp-xhdpi")
open class ScreensTest {
    private val hilt = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(hilt).around(compose)

    protected open val prefix = "light"

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("../design/screenshots/${prefix}_$name.png")
    }

    private fun waitText(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun click(text: String) {
        waitText(text)
        compose.onAllNodesWithText(text, substring = true).onFirst().performClick()
        compose.waitForIdle()
    }

    /** Кнопка-иконка: ищем по описанию или по подписи действия (как её озвучит TalkBack). */
    private fun clickDesc(desc: String) {
        val m = (hasContentDescription(desc) or SemanticsMatcher("onClickLabel=$desc") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == desc
        }) and hasClickAction()
        compose.waitUntil(10_000) { compose.onAllNodes(m).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(m).onFirst().performClick()
        compose.waitForIdle()
    }

    @Test
    fun walkThroughApp() {
        waitText("Создать аккаунт")
        shot("01_welcome")

        click("Создать аккаунт")
        shot("02_register")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        click("Посмотреть пример семьи")
        waitText("приёмов семьи сегодня")
        shot("03_home")

        click("Расписание")
        waitText("Неделя")
        shot("04_schedule_week")
        click("Месяц")
        shot("05_schedule_month")

        click("Семья")
        waitText("Сейткали")
        shot("06_family")
        click("Богдан")
        waitText("Препараты")
        shot("07_child_profile")
        click("Витамин D3")
        waitText("Дозировка")
        shot("08_medication_detail")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        click("История")
        waitText("приёмов отмечено")
        shot("09_history")
        click("Статистика")
        waitText("По людям")
        shot("10_stats")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        clickDesc("Добавить препарат")
        waitText("Новый препарат")
        shot("11_add_medication")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Напоминать о приёме"))
        shot("12_add_medication_reminders")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        click("Главная")
        clickDesc("Настройки")
        waitText("Приватность")
        shot("13_settings")
        click("Уведомления")
        shot("14_notification_settings")
    }
}

@HiltAndroidTest
@Config(application = HiltTestApplication::class, sdk = [35], qualifiers = "ru-w360dp-h800dp-night-xhdpi")
class ScreensDarkTest : ScreensTest() {
    override val prefix = "dark"
}
