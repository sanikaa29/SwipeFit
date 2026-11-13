package com.swipefit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.swipefit.SwipeFitApp
import com.swipefit.ui.dashboard.DashboardScreen
import com.swipefit.ui.dashboard.DashboardViewModel
import com.swipefit.ui.leaderboard.LeaderboardScreen
import com.swipefit.ui.leaderboard.LeaderboardViewModel
import com.swipefit.ui.swipe.SwipeScreen
import com.swipefit.ui.swipe.SwipeViewModel
import com.swipefit.ui.upload.UploadScreen
import com.swipefit.ui.upload.UploadViewModel
import com.swipefit.ui.welcome.WelcomeScreen
import com.swipefit.ui.welcome.WelcomeViewModel

object Routes {
    const val ARG_GROUP_ID = "groupId"
    const val WELCOME = "welcome"
    const val DASHBOARD = "dashboard/{$ARG_GROUP_ID}"
    const val UPLOAD = "upload/{$ARG_GROUP_ID}"
    const val SWIPE = "swipe/{$ARG_GROUP_ID}"
    const val LEADERBOARD = "leaderboard/{$ARG_GROUP_ID}"

    fun dashboard(groupId: String) = "dashboard/$groupId"
    fun upload(groupId: String) = "upload/$groupId"
    fun swipe(groupId: String) = "swipe/$groupId"
    fun leaderboard(groupId: String) = "leaderboard/$groupId"
}

private val groupArgs = listOf(navArgument(Routes.ARG_GROUP_ID) { type = NavType.StringType })

@Composable
fun SwipeFitNavHost() {
    val navController = rememberNavController()
    val container = (LocalContext.current.applicationContext as SwipeFitApp).container

    NavHost(navController = navController, startDestination = Routes.WELCOME) {

        composable(Routes.WELCOME) {
            val vm: WelcomeViewModel = viewModel(
                factory = viewModelFactory { initializer { WelcomeViewModel(container.groups) } }
            )
            WelcomeScreen(
                viewModel = vm,
                onGroupReady = { groupId -> navController.navigate(Routes.dashboard(groupId)) }
            )
        }

        composable(Routes.DASHBOARD, arguments = groupArgs) { entry ->
            val groupId = entry.arguments?.getString(Routes.ARG_GROUP_ID).orEmpty()
            val vm: DashboardViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { DashboardViewModel(groupId, container.groups, container.outfits) }
                }
            )
            DashboardScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onUpload = { navController.navigate(Routes.upload(groupId)) },
                onSwipe = { navController.navigate(Routes.swipe(groupId)) },
                onLeaderboard = { navController.navigate(Routes.leaderboard(groupId)) }
            )
        }

        composable(Routes.UPLOAD, arguments = groupArgs) { entry ->
            val groupId = entry.arguments?.getString(Routes.ARG_GROUP_ID).orEmpty()
            val vm: UploadViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { UploadViewModel(groupId, container.app, container.outfits) }
                }
            )
            UploadScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SWIPE, arguments = groupArgs) { entry ->
            val groupId = entry.arguments?.getString(Routes.ARG_GROUP_ID).orEmpty()
            val vm: SwipeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { SwipeViewModel(groupId, container.auth, container.outfits, container.votes) }
                }
            )
            SwipeScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onLeaderboard = {
                    navController.navigate(Routes.leaderboard(groupId)) {
                        popUpTo(Routes.swipe(groupId)) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LEADERBOARD, arguments = groupArgs) { entry ->
            val groupId = entry.arguments?.getString(Routes.ARG_GROUP_ID).orEmpty()
            val vm: LeaderboardViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { LeaderboardViewModel(groupId, container.outfits, container.votes) }
                }
            )
            LeaderboardScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
