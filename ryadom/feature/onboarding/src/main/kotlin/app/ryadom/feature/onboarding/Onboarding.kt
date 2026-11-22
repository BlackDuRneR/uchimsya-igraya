package app.ryadom.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.ryadom.core.data.repository.FamilyRepository
import app.ryadom.core.data.repository.SessionRepository
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.NoticeBanner
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyIcon
import app.ryadom.core.designsystem.component.RyIllustration
import app.ryadom.core.designsystem.component.RyTextField
import app.ryadom.core.designsystem.component.RyTopBar
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.ui.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable data object WelcomeRoute
@Serializable data object RegisterRoute
@Serializable data object SignInRoute
@Serializable data object FamilySetupRoute
@Serializable data object CreateFamilyRoute
@Serializable data object JoinFamilyRoute

data class FormState(val busy: Boolean = false, val error: Throwable? = null)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val session: SessionRepository,
    private val family: FamilyRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FormState())
    val state = _state.asStateFlow()

    private fun run(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.value = FormState(busy = true)
        viewModelScope.launch {
            val r = runCatching { block() }
            _state.value = FormState(busy = false, error = r.exceptionOrNull())
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun register(name: String, email: String, password: String) = run { session.register(name, email, password) }
    fun signIn(email: String, password: String) = run { session.signIn(email, password) }
    fun demo() = run { session.startDemo() }
    fun createFamily(name: String) = run { family.createFamily(name) }
    fun join(code: String) = run { family.joinByCode(code) }
    fun signOut() = run { session.signOut() }
}

/** Граф входа. Переход в основное приложение делает сессия: как только семья готова, корневой экран меняется сам. */
fun NavGraphBuilder.onboardingGraph(nav: NavHostController) {
    composable<WelcomeRoute> {
        val vm: OnboardingViewModel = hiltViewModel()
        WelcomeScreen(onRegister = { nav.navigate(RegisterRoute) }, onSignIn = { nav.navigate(SignInRoute) }, onDemo = vm::demo)
    }
    composable<RegisterRoute> {
        val vm: OnboardingViewModel = hiltViewModel()
        val st by vm.state.collectAsStateWithLifecycle()
        RegisterScreen(st, onBack = nav::popBackStack, onSubmit = vm::register)
    }
    composable<SignInRoute> {
        val vm: OnboardingViewModel = hiltViewModel()
        val st by vm.state.collectAsStateWithLifecycle()
        SignInScreen(st, onBack = nav::popBackStack, onSubmit = vm::signIn, onRegister = { nav.navigate(RegisterRoute) { popUpTo(WelcomeRoute) } })
    }
    composable<FamilySetupRoute> {
        val vm: OnboardingViewModel = hiltViewModel()
        FamilySetupScreen(onCreate = { nav.navigate(CreateFamilyRoute) }, onJoin = { nav.navigate(JoinFamilyRoute) }, onSignOut = vm::signOut)
    }
    composable<CreateFamilyRoute> {
        val vm: OnboardingViewModel = hiltViewModel()
        val st by vm.state.collectAsStateWithLifecycle()
        CreateFamilyScreen(st, onBack = nav::popBackStack, onSubmit = vm::createFamily)
    }
    composable<JoinFamilyRoute> {
        val vm: OnboardingViewModel = hiltViewModel()
        val st by vm.state.collectAsStateWithLifecycle()
        JoinFamilyScreen(st, onBack = nav::popBackStack, onSubmit = vm::join)
    }
}

@Composable
private fun FormScaffold(
    title: String,
    onBack: (() -> Unit)?,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Ry.colors.bg).safeDrawingPadding().imePadding()) {
        RyTopBar("", navLabel = stringResource(R.string.common_back), onNav = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(title, style = Ry.type.display, color = Ry.colors.text)
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun WelcomeScreen(onRegister: () -> Unit, onSignIn: () -> Unit, onDemo: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ry.colors.bg).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.foundation.Image(painterResource(RyIcons.Logo), null, Modifier.size(32.dp))
                Text(stringResource(R.string.app_wordmark), style = Ry.type.headline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = Ry.colors.text)
            }
            RyIllustration(Illustration.FamilyScene, Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Text(stringResource(R.string.welcome_title), style = Ry.type.display.copy(fontSize = Ry.type.display.fontSize * 1.1f), color = Ry.colors.text)
            Text(stringResource(R.string.welcome_body), style = Ry.type.body, color = Ry.colors.textMuted)
            Spacer(Modifier.weight(1f, fill = false).height(8.dp))
            RyButton(stringResource(R.string.welcome_register), onRegister, Modifier.fillMaxWidth())
            RyButton(stringResource(R.string.welcome_sign_in), onSignIn, Modifier.fillMaxWidth(), style = ButtonStyle.Ghost)
            RyButton(stringResource(R.string.welcome_demo), onDemo, Modifier.align(Alignment.CenterHorizontally), style = ButtonStyle.Text)
        }
    }
}

@Composable
fun RegisterScreen(state: FormState, onBack: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    FormScaffold(stringResource(R.string.register_title), onBack) {
        Text(stringResource(R.string.register_body), style = Ry.type.body, color = Ry.colors.textMuted)
        RyTextField(name, { name = it }, stringResource(R.string.field_your_name),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
        RyTextField(email, { email = it.trim() }, stringResource(R.string.field_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
        RyTextField(password, { password = it }, stringResource(R.string.field_password), password = true,
            placeholder = stringResource(R.string.field_password_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(name, email, password) }))
        state.error?.let { NoticeBanner(it.userMessage(), RyIcons.Info, warn = true) }
        RyButton(stringResource(R.string.common_continue), { onSubmit(name, email, password) }, Modifier.fillMaxWidth(), enabled = !state.busy)
        PrivacyNote()
    }
}

@Composable
fun SignInScreen(state: FormState, onBack: () -> Unit, onSubmit: (String, String) -> Unit, onRegister: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    FormScaffold(stringResource(R.string.sign_in_title), onBack) {
        RyTextField(email, { email = it.trim() }, stringResource(R.string.field_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
        RyTextField(password, { password = it }, stringResource(R.string.field_password), password = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(email, password) }))
        state.error?.let { NoticeBanner(it.userMessage(), RyIcons.Info, warn = true) }
        RyButton(stringResource(R.string.welcome_sign_in), { onSubmit(email, password) }, Modifier.fillMaxWidth(), enabled = !state.busy)
        RyButton(stringResource(R.string.sign_in_no_account), onRegister, Modifier.align(Alignment.CenterHorizontally), style = ButtonStyle.Text)
    }
}

@Composable
private fun PrivacyNote() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RyIcon(RyIcons.Lock, null, tint = Ry.colors.textMuted, size = 18.dp)
        Text(stringResource(R.string.privacy_short), style = Ry.type.caption, color = Ry.colors.textMuted)
    }
}

@Composable
fun FamilySetupScreen(onCreate: () -> Unit, onJoin: () -> Unit, onSignOut: () -> Unit) {
    FormScaffold(stringResource(R.string.family_setup_title), null) {
        Text(stringResource(R.string.family_setup_body), style = Ry.type.body, color = Ry.colors.textMuted)
        ChoiceCard(RyIcons.Family, stringResource(R.string.family_setup_create), stringResource(R.string.family_setup_create_hint), onCreate)
        ChoiceCard(RyIcons.Link, stringResource(R.string.family_setup_join), stringResource(R.string.family_setup_join_hint), onJoin)
        RyIllustration(Illustration.FamilyCare, Modifier.fillMaxWidth().padding(top = 8.dp))
        RyButton(stringResource(R.string.settings_sign_out), onSignOut, Modifier.align(Alignment.CenterHorizontally), style = ButtonStyle.Text)
    }
}

@Composable
private fun ChoiceCard(icon: Int, title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Ry.shapes.l).background(Ry.colors.surface).border(1.dp, Ry.colors.border, Ry.shapes.l)
            .clickable(role = Role.Button, onClick = onClick).padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(Ry.shapes.m).background(Ry.colors.primaryContainer), contentAlignment = Alignment.Center) {
            RyIcon(icon, null, tint = Ry.colors.primaryDark)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Ry.type.subtitle, color = Ry.colors.text)
            Text(body, style = Ry.type.bodySmall, color = Ry.colors.textMuted)
        }
        RyIcon(RyIcons.ChevronRight, null, tint = Ry.colors.textSecondary, size = 20.dp)
    }
}

@Composable
fun CreateFamilyScreen(state: FormState, onBack: () -> Unit, onSubmit: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    FormScaffold(stringResource(R.string.create_family_title), onBack) {
        Text(stringResource(R.string.create_family_body), style = Ry.type.body, color = Ry.colors.textMuted)
        RyTextField(name, { name = it }, stringResource(R.string.field_family_name), placeholder = stringResource(R.string.field_family_name_hint),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(name) }))
        state.error?.let { NoticeBanner(it.userMessage(), RyIcons.Info, warn = true) }
        RyButton(stringResource(R.string.create_family_submit), { onSubmit(name) }, Modifier.fillMaxWidth(), enabled = !state.busy)
        PrivacyNote()
    }
}

@Composable
fun JoinFamilyScreen(state: FormState, onBack: () -> Unit, onSubmit: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    FormScaffold(stringResource(R.string.join_family_title), onBack) {
        Text(stringResource(R.string.join_family_body), style = Ry.type.body, color = Ry.colors.textMuted)
        RyTextField(code, { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6) }, stringResource(R.string.field_invite_code),
            placeholder = "A7K2QX",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(code) }))
        state.error?.let { NoticeBanner(it.userMessage(), RyIcons.Info, warn = true) }
        RyButton(stringResource(R.string.join_family_submit), { onSubmit(code) }, Modifier.fillMaxWidth(), enabled = !state.busy && code.length == 6)
        Text(stringResource(R.string.join_family_hint), style = Ry.type.caption, color = Ry.colors.textMuted, textAlign = TextAlign.Start)
    }
}
