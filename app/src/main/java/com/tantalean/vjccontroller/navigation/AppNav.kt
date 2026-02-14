package com.tantalean.vjccontroller.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tantalean.vjccontroller.ui.screen.ConfigScreen
import com.tantalean.vjccontroller.ui.screen.ControlScreen
import com.tantalean.vjccontroller.viewmodel.MainViewModel

/**
 * Rutas de navegación de la app.
 */
object Routes {
    const val CONFIG = "config"
    const val CONTROL = "control"
}

@Composable
fun AppNav(
    vm: MainViewModel,
    nav: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = nav,
        startDestination = Routes.CONFIG,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(350))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(350))
        }
    ) {
        composable(Routes.CONFIG) {
            ConfigScreen(
                vm = vm,
                onNavigateToControl = {
                    nav.navigate(Routes.CONTROL) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.CONTROL) {
            ControlScreen(
                vm = vm,
                onNavigateBack = { nav.popBackStack() }
            )
        }
    }
}