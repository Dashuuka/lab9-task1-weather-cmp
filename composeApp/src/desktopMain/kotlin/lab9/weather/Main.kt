package lab9.weather

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Weather CMP") {
        WeatherApp()
    }
}
