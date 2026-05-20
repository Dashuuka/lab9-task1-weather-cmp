package lab9.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

data class Weather(
    val city: String,
    val temperatureC: Double,
    val description: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Double,
    val cached: Boolean = false,
)

sealed class WeatherError(message: String) : Exception(message) {
    data object EmptyCity : WeatherError("empty_city")
    data object Network : WeatherError("network")
    data object InvalidApiKey : WeatherError("invalid_api_key")
    data object CityNotFound : WeatherError("city_not_found")
    data object Parsing : WeatherError("parsing")
    data object Cache : WeatherError("cache")
}

interface WeatherService {
    suspend fun load(city: String, apiKey: String): Weather
}

class FakeWeatherService : WeatherService {
    override suspend fun load(city: String, apiKey: String): Weather {
        val normalized = validateCity(city)
        return Weather(normalized, 18.7, "demo clear sky", "01d", 63, 3.4)
    }
}

class OpenWeatherMapService(
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) : WeatherService {
    override suspend fun load(city: String, apiKey: String): Weather {
        val normalized = validateCity(city)
        if (apiKey.isBlank()) throw WeatherError.InvalidApiKey
        return try {
            val response = client.get("https://api.openweathermap.org/data/2.5/weather") {
                url {
                    parameters.append("q", normalized)
                    parameters.append("appid", apiKey)
                    parameters.append("units", "metric")
                    parameters.append("lang", "ru")
                }
            }
            when (response.status) {
                HttpStatusCode.Unauthorized -> throw WeatherError.InvalidApiKey
                HttpStatusCode.NotFound -> throw WeatherError.CityNotFound
                else -> if (response.status.value >= 400) throw WeatherError.Network
            }
            response.body<OpenWeatherDto>().toDomain()
        } catch (e: WeatherError) {
            println("[Weather] ${e.message}")
            throw e
        } catch (e: Exception) {
            println("[Weather] ${e.message}")
            throw WeatherError.Network
        }
    }
}

class WeatherCache {
    private val items = mutableMapOf<String, Weather>()

    fun save(weather: Weather) {
        try {
            items[weather.city.lowercase()] = weather.copy(cached = true)
        } catch (e: Exception) {
            println("[WeatherCache] ${e.message}")
            throw WeatherError.Cache
        }
    }

    fun read(city: String): Weather? = try {
        items[city.lowercase()]
    } catch (e: Exception) {
        println("[WeatherCache] ${e.message}")
        throw WeatherError.Cache
    }
}

class WeatherRepository(
    private val realService: WeatherService = OpenWeatherMapService(),
    private val fakeService: WeatherService = FakeWeatherService(),
    private val cache: WeatherCache = WeatherCache(),
) {
    suspend fun search(city: String, apiKey: String, demoMode: Boolean): Weather {
        val service = if (demoMode) fakeService else realService
        return try {
            service.load(city, apiKey).also(cache::save)
        } catch (e: Exception) {
            val cached = cache.read(city)
            if (cached != null) cached else throw e
        }
    }
}

fun validateCity(city: String): String {
    val value = city.trim()
    if (value.isBlank()) throw WeatherError.EmptyCity
    return value
}

fun formatTemperature(value: Double): String = "${value.roundToInt()} C"

fun weatherGridColumnsForWidth(widthDp: Float): Int = when {
    widthDp < 600f -> 1
    widthDp < 1000f -> 2
    else -> 3
}

@Serializable
data class OpenWeatherDto(
    val name: String,
    val main: MainDto,
    val weather: List<WeatherDto>,
    val wind: WindDto,
) {
    fun toDomain(): Weather {
        val first = weather.firstOrNull() ?: throw WeatherError.Parsing
        return Weather(name, main.temp, first.description, first.icon, main.humidity, wind.speed)
    }
}

@Serializable data class MainDto(val temp: Double, val humidity: Int)
@Serializable data class WeatherDto(val description: String, val icon: String)
@Serializable data class WindDto(val speed: Double)

fun parseWeatherJson(json: String): Weather = try {
    Json { ignoreUnknownKeys = true }.decodeFromString<OpenWeatherDto>(json).toDomain()
} catch (e: Exception) {
    println("[WeatherParser] ${e.message}")
    throw WeatherError.Parsing
}

enum class Lang { Ru, En, Be }

private val strings = mapOf(
    Lang.Ru to mapOf(
        "title" to "Погода",
        "city" to "Город",
        "api" to "API ключ",
        "demo" to "Demo",
        "search" to "Найти",
        "temp" to "Температура",
        "humidity" to "Влажность",
        "wind" to "Ветер",
        "cached" to "Показан кеш",
        "empty_city" to "Введите город",
    ),
    Lang.En to mapOf(
        "title" to "Weather",
        "city" to "City",
        "api" to "API key",
        "demo" to "Demo",
        "search" to "Search",
        "temp" to "Temperature",
        "humidity" to "Humidity",
        "wind" to "Wind",
        "cached" to "Cached data",
        "empty_city" to "Enter city",
    ),
    Lang.Be to mapOf(
        "title" to "Надвор'е",
        "city" to "Горад",
        "api" to "API ключ",
        "demo" to "Demo",
        "search" to "Шукаць",
        "temp" to "Тэмпература",
        "humidity" to "Вільготнасць",
        "wind" to "Вецер",
        "cached" to "Паказаны кеш",
        "empty_city" to "Увядзіце горад",
    ),
)

@Composable
fun WeatherApp(repository: WeatherRepository = remember { WeatherRepository() }) {
    var city by remember { mutableStateOf("Minsk") }
    var apiKey by remember { mutableStateOf("") }
    var demoMode by remember { mutableStateOf(true) }
    var lang by remember { mutableStateOf(Lang.Ru) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val results = remember { mutableStateListOf<Weather>() }
    val scope = rememberCoroutineScope()
    val s = strings.getValue(lang)

    MaterialTheme {
        Surface {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val columnCount = weatherGridColumnsForWidth(maxWidth.value)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(columnCount) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(s.getValue("title"), style = MaterialTheme.typography.headlineMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { lang = Lang.Ru }) { Text("RU") }
                                Button(onClick = { lang = Lang.En }) { Text("EN") }
                                Button(onClick = { lang = Lang.Be }) { Text("BE") }
                            }
                        }
                    }
                    item(span = { GridItemSpan(columnCount) }) {
                        Card {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(city, { city = it }, label = { Text(s.getValue("city")) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(apiKey, { apiKey = it }, label = { Text(s.getValue("api")) }, modifier = Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(s.getValue("demo"))
                                    Switch(demoMode, { demoMode = it })
                                    Button(onClick = {
                                        loading = true
                                        error = null
                                        scope.launch {
                                            try {
                                                results.add(0, repository.search(city, apiKey, demoMode))
                                            } catch (e: Exception) {
                                                error = s[e.message] ?: e.message ?: "Error"
                                                println("[WeatherUi] ${e.message}")
                                            } finally {
                                                loading = false
                                            }
                                        }
                                    }) { Text(s.getValue("search")) }
                                }
                                if (loading) CircularProgressIndicator()
                                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                    items(results) { weather ->
                        WeatherCard(weather, s)
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: Weather, s: Map<String, String>) {
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${weather.city} ${weather.icon}", style = MaterialTheme.typography.titleLarge)
            Text("${s.getValue("temp")}: ${formatTemperature(weather.temperatureC)}")
            Text(weather.description)
            Text("${s.getValue("humidity")}: ${weather.humidity}%")
            Text("${s.getValue("wind")}: ${weather.windSpeed} m/s")
            if (weather.cached) Text(s.getValue("cached"))
        }
    }
}
