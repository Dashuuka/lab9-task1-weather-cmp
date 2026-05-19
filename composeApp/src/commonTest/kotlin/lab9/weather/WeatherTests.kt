package lab9.weather

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WeatherTests {
    @Test
    fun validatesCity() {
        assertEquals("Minsk", validateCity(" Minsk "))
        assertFailsWith<WeatherError.EmptyCity> { validateCity(" ") }
    }

    @Test
    fun parsesOpenWeatherJson() {
        val json = """{"name":"Minsk","main":{"temp":12.4,"humidity":70},"weather":[{"description":"cloudy","icon":"03d"}],"wind":{"speed":4.1}}"""
        val weather = parseWeatherJson(json)
        assertEquals("Minsk", weather.city)
        assertEquals(70, weather.humidity)
    }

    @Test
    fun formatsTemperature() {
        assertEquals("19 C", formatTemperature(18.7))
    }

    @Test
    fun savesAndReadsCache() {
        val cache = WeatherCache()
        cache.save(Weather("Minsk", 10.0, "rain", "10d", 90, 5.0))
        assertTrue(cache.read("minsk")?.cached == true)
    }

    @Test
    fun fakeServiceReturnsWeather() = runTest {
        val weather = FakeWeatherService().load("Brest", "")
        assertEquals("Brest", weather.city)
    }

    @Test
    fun repositoryFallsBackToCache() = runTest {
        val cache = WeatherCache()
        val repo = WeatherRepository(
            realService = object : WeatherService {
                override suspend fun load(city: String, apiKey: String): Weather = throw WeatherError.Network
            },
            fakeService = FakeWeatherService(),
            cache = cache,
        )
        cache.save(Weather("Minsk", 1.0, "cached", "01d", 40, 1.0))
        assertEquals("cached", repo.search("Minsk", "bad", demoMode = false).description)
    }
}
