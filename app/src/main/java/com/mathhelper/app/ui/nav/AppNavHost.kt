package com.mathhelper.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mathhelper.app.ui.home.HomeScreen
import com.mathhelper.app.ui.home.KnowledgeTreeScreen
import com.mathhelper.app.ui.parent.MasteryMapScreen
import com.mathhelper.app.ui.parent.MistakeListScreen
import com.mathhelper.app.ui.parent.ParentHomeScreen
import com.mathhelper.app.ui.parent.PhotoEntryScreen
import com.mathhelper.app.ui.parent.SettingsScreen
import com.mathhelper.app.ui.student.LearnScreen
import com.mathhelper.app.ui.student.LevelScreen
import com.mathhelper.app.ui.student.RewardScreen
import com.mathhelper.app.ui.student.StudentHomeScreen
import com.mathhelper.app.util.PinManager

object Routes {
    const val HOME = "home"
    const val KNOWLEDGE_TREE = "knowledge_tree"
    const val PARENT = "parent"
    const val PHOTO_ENTRY = "photo_entry"
    const val MISTAKE_LIST = "mistake_list"
    const val MASTERY_MAP = "mastery_map"
    const val STUDENT = "student"
    const val STUDY_BROWSE = "study_browse"
    const val SETTINGS = "settings"
    const val REWARD = "reward"
    const val LEVEL = "level"
    const val LEARN = "learn/{knowledgePointId}"

    fun learn(knowledgePointId: String) = "learn/$knowledgePointId"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val context = LocalContext.current
            HomeScreen(
                onParent = { navController.navigate(Routes.PARENT) },
                onStudent = {
                    PinManager(context).clearUnlock()
                    navController.navigate(Routes.STUDENT)
                },
                onKnowledgeTree = { navController.navigate(Routes.KNOWLEDGE_TREE) }
            )
        }

        composable(Routes.KNOWLEDGE_TREE) {
            KnowledgeTreeScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PARENT) {
            ParentHomeScreen(
                onBack = { navController.popBackStack() },
                onPhotoEntry = { navController.navigate(Routes.PHOTO_ENTRY) },
                onMistakeList = { navController.navigate(Routes.MISTAKE_LIST) },
                onMasteryMap = { navController.navigate(Routes.MASTERY_MAP) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.PHOTO_ENTRY) {
            PhotoEntryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MISTAKE_LIST) {
            MistakeListScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MASTERY_MAP) {
            MasteryMapScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STUDENT) {
            StudentHomeScreen(
                onBack = { navController.popBackStack() },
                onLearn = { kpId -> navController.navigate(Routes.learn(kpId)) },
                onBrowse = { navController.navigate(Routes.STUDY_BROWSE) },
                onReward = { navController.navigate(Routes.REWARD) },
                onLevel = { navController.navigate(Routes.LEVEL) }
            )
        }

        composable(Routes.LEVEL) {
            LevelScreen(
                onBack = { navController.popBackStack() },
                onLearn = { kpId -> navController.navigate(Routes.learn(kpId)) }
            )
        }

        composable(Routes.STUDY_BROWSE) {
            KnowledgeTreeScreen(
                onBack = { navController.popBackStack() },
                title = "自己学",
                onTopicClick = { kpId -> navController.navigate(Routes.learn(kpId)) },
                largeText = true,
                showWeakness = true
            )
        }

        composable(Routes.REWARD) {
            RewardScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.LEARN,
            arguments = listOf(navArgument("knowledgePointId") { type = NavType.StringType })
        ) { entry ->
            val kpId = entry.arguments?.getString("knowledgePointId")
            if (kpId != null) {
                LearnScreen(
                    knowledgePointId = kpId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
