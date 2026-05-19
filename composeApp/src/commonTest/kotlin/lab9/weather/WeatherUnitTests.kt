package lab9.weather

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WeatherUnitTests {
    @Test
    fun cityValidationTrimsNonEmptyValue() {
        assertEquals("Minsk", validateCity("  Minsk  "))
    }

    @Test
    fun emptyCityValidationFails() {
        assertFailsWith<WeatherError.EmptyCity> { validateCity(" ") }
    }

    @Test
    fun parserReadsWeatherPayload() {
        val json = """{"name":"Brest","main":{"temp":7.6,"humidity":81},"weather":[{"description":"rain","icon":"10d"}],"wind":{"speed":2.5}}"""
        val weather = parseWeatherJson(json)
        assertEquals("Brest", weather.city)
        assertEquals("rain", weather.description)
        assertEquals(81, weather.humidity)
    }
}
