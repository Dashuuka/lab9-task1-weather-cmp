package lab9.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeatherUiStateTests {
    @Test
    fun initialStateUsesDemoModeAndNoResults() {
        val state = WeatherUiState()
        assertTrue(state.demoMode)
        assertTrue(state.items.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun emptyCityShowsValidationErrorState() {
        val state = WeatherUiState(city = " ").validateForSearch()
        assertFalse(state.loading)
        assertEquals("empty_city", state.error)
    }

    @Test
    fun successfulSearchPrependsWeatherCardState() {
        val weather = Weather("Minsk", 18.0, "clear", "01d", 55, 3.0)
        val state = WeatherUiState(city = "Minsk").searchStarted().searchSucceeded(weather)
        assertFalse(state.loading)
        assertEquals("Minsk", state.items.first().city)
        assertNull(state.error)
    }
}
