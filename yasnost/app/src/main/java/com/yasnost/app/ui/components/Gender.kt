package com.yasnost.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalFemale = staticCompositionLocalOf { false }

/** Текст в мужском или женском роде. */
@Composable
fun g(male: String, female: String) = if (LocalFemale.current) female else male
