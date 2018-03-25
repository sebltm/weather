package exeter.sm807.weather

import android.content.Context
import android.os.Bundle
import android.support.v4.content.AsyncTaskLoader
import org.json.JSONObject

/**
 * Created by 660046669 on 18/03/2018.
 */

class CurrentWeatherLoader(private var loaderBundle: Bundle?, context: Context) : AsyncTaskLoader<Any>(context) {
    private var defaultBundle: Bundle? = null

    override fun onStartLoading() {
        if (loaderBundle == null) {
            val preferences = context.getSharedPreferences("location", Context.MODE_PRIVATE)

            defaultBundle = Bundle()
            defaultBundle?.putString("city", preferences.getString("city_name", "Exeter"))
            defaultBundle?.putString("country", preferences.getString("country", "UK"))
        } else {
            defaultBundle = loaderBundle
        }

        forceLoad()
    }

    override fun loadInBackground(): Weather? {
        val con = HttpConnection("weather", defaultBundle!!.getString("city"),
                defaultBundle!!.getString("country"), "metric")
        val searchUrl = con.url!!
        val out = con.openConnection(searchUrl) ?: return null

        return try {
            val reader = JSONObject(out)
            val response = Weather()

            response.days.add(response.Day())
            response.days[0].list.add(loadList(reader, response, 0))
            response.city = loadCity(reader, response)
            response.city.coord = loadCoord(reader, response)
            response.days[0].list[0].weather = loadWeather(reader, response)
            response.days[0].list[0].weather.wind = loadWind(reader, response)
            response.days[0].list[0].sys = loadSys(reader, response)

            return response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadList(reader: JSONObject, weather: Weather, pos: Int): Weather.Day.List {
        var humidity: Double? = null
        var temp: Double? = null
        var pressure: Double? = null
        var tempMin: Double? = null
        var tempMax: Double? = null
        var seaLevel: Double? = null
        var grndLevel: Double? = null
        var cloudsAll: Double? = null
        var rain3h: Double? = null
        var snow3h: Double? = null

        if (!reader.isNull("rain")) {
            val rain = reader.getJSONObject("rain")
            if (!rain.isNull("3h")) rain3h = rain.getDouble("3h")
        }

        if (!reader.isNull("clouds")) {
            val clouds = reader.getJSONObject("clouds")
            if (!clouds.isNull("all")) cloudsAll = clouds.getDouble("all")
        }

        if (!reader.isNull("snow")) {
            val snow = reader.getJSONObject("snow")
            if (!snow.isNull("3h")) snow3h = snow.getDouble("3h")
        }

        if (!reader.isNull("main")) {
            val main = reader.getJSONObject("main")

            if (!main.isNull("humidity")) humidity = main.getDouble("humidity")
            if (!main.isNull("temp")) temp = main.getDouble("temp")
            if (!main.isNull("pressure")) pressure = main.getDouble("pressure")
            if (!main.isNull("temp_min")) tempMin = main.getDouble("temp_min")
            if (!main.isNull("temp_max")) tempMax = main.getDouble("temp_max")
            if (!main.isNull("sea_level")) seaLevel = main.getDouble("sea_level")
            if (!main.isNull("grnd_level")) grndLevel = main.getDouble("grnd_level")
        }

        return weather.days.last().List(temp, pressure, humidity, tempMin, tempMax, seaLevel, grndLevel, cloudsAll, rain3h, snow3h)
    }

    private fun loadCity(reader: JSONObject, weather: Weather): Weather.City {
        var cityID: Long? = null
        var name: String? = null
        var country: String? = null

        if (!reader.isNull("id")) cityID = reader.getLong("id")
        if (!reader.isNull("name")) name = reader.getString("name")
        if (!reader.isNull("sys")) {
            val sys = reader.getJSONObject("sys")

            if (!sys.isNull("country")) country = sys.getString("country")
        }

        return weather.City(cityID, name, country)
    }

    private fun loadCoord(reader: JSONObject, weather: Weather): Weather.City.Coord {
        var lat: Double? = null
        var lon: Double? = null

        if (!reader.isNull("coord")) {
            val coord = reader.getJSONObject("coord")

            if (!coord.isNull("lat")) lat = coord.getDouble("lat")
            if (!coord.isNull("lon")) lon = coord.getDouble("lon")
        }

        return weather.city.Coord(lon, lat)

    }

    private fun loadWeather(reader: JSONObject, weather: Weather): Weather.Day.List.Weather {
        var id: Int? = null
        var weatherMain: String? = null
        var description: String? = null
        var icon: String? = null

        if (!reader.isNull("weather")) {
            val weatherArr = reader.getJSONArray("weather")

            if (!weatherArr.isNull(0)) {
                val weatherObj = weatherArr.getJSONObject(0)

                if (!weatherObj.isNull("id")) id = weatherObj.getInt("id")
                if (!weatherObj.isNull("main")) weatherMain = weatherObj.getString("main")
                if (!weatherObj.isNull("description")) description = weatherObj.getString("description")
                if (!weatherObj.isNull("icon")) icon = weatherObj.getString("icon")
            }
        }

        return weather.days.last().list.last().Weather(id, weatherMain, description, icon)
    }

    private fun loadWind(reader: JSONObject, weather: Weather): Weather.Day.List.Weather.Wind {
        var speed: Double? = null
        var deg: Double? = null

        if (!reader.isNull("wind")) {
            val wind = reader.getJSONObject("wind")

            if (!wind.isNull("speed")) speed = wind.getDouble("speed")
            if (!wind.isNull("deg")) deg = wind.getDouble("deg")
        }
        return weather.days.last().list.last().weather.Wind(speed, deg)
    }

    private fun loadSys(reader: JSONObject, weather: Weather): Weather.Day.List.Sys {
        var sunrise: Long? = null
        var sunset: Long? = null

        if (!reader.isNull("sys")) {
            val sys = reader.getJSONObject("sys")

            if (!sys.isNull("sunrise")) sunrise = sys.getLong("sunrise")
            if (!sys.isNull("sunset")) sunset = sys.getLong("sunset")
        }

        return weather.days.last().list.last().Sys(sunrise, sunset)
    }
}