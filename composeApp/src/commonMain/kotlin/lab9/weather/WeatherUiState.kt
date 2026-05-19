package lab9.weather

data class WeatherUiState(
    val city: String = "",
    val apiKey: String = "",
    val demoMode: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<Weather> = emptyList(),
)

fun WeatherUiState.cityChanged(value: String): WeatherUiState =
    copy(city = value, error = null)

fun WeatherUiState.searchStarted(): WeatherUiState =
    copy(loading = true, error = null)

fun WeatherUiState.searchSucceeded(weather: Weather): WeatherUiState =
    copy(loading = false, error = null, items = listOf(weather) + items)

fun WeatherUiState.searchFailed(errorKey: String): WeatherUiState =
    copy(loading = false, error = errorKey)

fun WeatherUiState.validateForSearch(): WeatherUiState =
    try {
        validateCity(city)
        copy(error = null)
    } catch (e: WeatherError) {
        searchFailed(e.message ?: "error")
    }
