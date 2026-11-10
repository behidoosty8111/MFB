package com.example.mfb.navigation

sealed class MfbScreen(val route: String) {
    data object Splash : MfbScreen("splash")
    data object Dashboard : MfbScreen("dashboard")
    data object Projects : MfbScreen("projects")
    data object Templates : MfbScreen("templates")
    data object Settings : MfbScreen("settings")
    data object Import : MfbScreen("import")
    data object ImportZip : MfbScreen("import_zip")
    data object ImportHtml : MfbScreen("import_html")
    data object About : MfbScreen("about")
    data object ProjectDetails : MfbScreen("project_details/{projectId}") {
        fun createRoute(projectId: Long): String = "project_details/$projectId"
    }
}
