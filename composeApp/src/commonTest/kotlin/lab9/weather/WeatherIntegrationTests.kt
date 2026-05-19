package lab9.weather

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WeatherIntegrationTests {
    @Test
    fun fakeServiceSearchStoresResultInCache() = runTest {
        val cache = WeatherCache()
        val repository = WeatherRepository(fakeService = FakeWeatherService(), cache = cache)
        val result = repository.search("Minsk", "", demoMode = true)
        assertEquals("Minsk", result.city)
        assertTrue(cache.read("Minsk")?.cached == true)
    }

    @Test
    fun networkFailureFallsBackToCachedWeather() = runTest {
        val cache = WeatherCache()
        cache.save(Weather("Grodno", 1.0, "cached snow", "13d", 90, 4.0))
        val repository = WeatherRepository(
            realService = object : WeatherService {
                override suspend fun load(city: String, apiKey: String): Weather = throw WeatherError.Network
            },
            cache = cache,
        )

        val result = repository.search("Grodno", "key", demoMode = false)
        assertEquals("cached snow", result.description)
        assertTrue(result.cached)
    }

    @Test
    fun invalidApiKeyWithoutCachePropagatesError() = runTest {
        val repository = WeatherRepository(
            realService = object : WeatherService {
                override suspend fun load(city: String, apiKey: String): Weather = throw WeatherError.InvalidApiKey
            },
            cache = WeatherCache(),
        )

        assertFailsWith<WeatherError.InvalidApiKey> {
            repository.search("Brest", "", demoMode = false)
        }
    }
}
