package app.ryadom.feature.family

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

/** Раздел «Семья»: вкладка и внутренние экраны. */
fun NavGraphBuilder.familyGraph(nav: FamilyNav, snackbar: SnackbarHostState) {
    composable<FamilyRoute> { entry -> FamilyScreen(nav, snackbar, openInviteInitially = entry.toRoute<FamilyRoute>().invite) }
    composable<MemberRoute> { MemberScreen(nav, snackbar) }
    composable<MemberEditRoute> { MemberEditScreen(onClose = nav.back) }
    composable<MessagesRoute> { MessagesScreen(onBack = nav.back) }
}
