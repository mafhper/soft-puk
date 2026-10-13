package dev.mafhper.softpuk.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.mafhper.softpuk.state.PuzzleState
import dev.mafhper.softpuk.ui.screens.ConfirmScreen
import dev.mafhper.softpuk.ui.screens.ScannerScreen
import dev.mafhper.softpuk.ui.screens.SolutionScreen

object Routes {
    const val SCANNER = "scanner"
    const val CONFIRM = "confirm"
    const val SOLUTION = "solution"
}

@Composable
fun SoftPukApp() {
    val nav = rememberNavController()
    val state = remember { PuzzleState() }

    NavHost(navController = nav, startDestination = Routes.SCANNER) {
        composable(Routes.SCANNER) {
            ScannerScreen(
                onPuzzleReady = {
                    state.loadDemo()
                    nav.navigate(Routes.CONFIRM)
                },
                onPuzzleParsed = { matrix, daemons ->
                    state.loadParsed(matrix, daemons)
                    nav.navigate(Routes.CONFIRM)
                },
            )
        }
        composable(Routes.CONFIRM) {
            ConfirmScreen(
                state = state,
                onBack = { nav.popBackStack() },
                onSolve = {
                    state.solve()
                    nav.navigate(Routes.SOLUTION)
                },
            )
        }
        composable(Routes.SOLUTION) {
            SolutionScreen(
                state = state,
                onBack = { nav.popBackStack() },
                onNewScan = { nav.popBackStack(Routes.SCANNER, inclusive = false) },
            )
        }
    }
}
