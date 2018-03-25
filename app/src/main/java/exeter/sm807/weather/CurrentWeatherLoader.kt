package exeter.sm807.weather

import android.content.Context
import android.os.Bundle
import android.support.v4.content.AsyncTaskLoader
import org.json.JSONObject

/**
 * Created by sebltm on 18/03/2018.
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

            val main = reader.getJSONObject("main")
            val wind = reader.getJSONObject("wind")
            val sys = reader.getJSONObject("sys")

            var rain: JSONObject? = null
            if (reader.has("rain") && !reader.isNull("rain")) rain = reader.getJSONObject("rain")

            var clouds: JSONObject? = null
            if (reader.has("clouds") && !reader.isNull("clouds")) clouds = reader.getJSONObject("clouds")

            var snow: JSONObject? = null
            if (reader.has("snow") && !reader.isNull("snow")) snow = reader.getJSONObject("snow")

            val humidity = main.getDouble("humidity")
            val temp = main.getDouble("temp")
            val deg = wind.getDouble("deg")
            val speed = wind.getDouble("speed")
            val pressure = main.getDouble("pressure")
            val sunrise = sys.getLong("sunrise")
            val sunset = sys.getLong("sunset")
            val tempMin = main.getDouble("temp_min")
            val tempMax = main.getDouble("temp_max")
            val cloudsAll = clouds?.getDouble("all")
            val rain3h = rain?.getDouble("3h")
            val snow3h = snow?.getDouble("3h")

            var seaLevel: Double? = null
            if (main.has("sea_level") && !main.isNull("sea_level")) seaLevel = main.getDouble("sea_level")

            var grndLevel: Double? = null
            if (main.has("grnd_level") && !main.isNull("grnd_level")) grndLevel = main.getDouble("grnd_level")

            val response = Weather()
            response.days.add(response.Day())
            response.days[0].list.add(response.days[0].List(
                    temp,
                    pressure,
                    humidity,
                    tempMin,
                    tempMax,
                    seaLevel,
                    grndLevel,
                    cloudsAll,
                    rain3h,
                    snow3h)
            )

            response.city = loadCity(reader, response)
            response.city.coord = loadCoord(reader, response)
            response.days[0].list[0].weather = loadWeather(reader, response)

            response.days[0].list[0].weather.wind =
                    response.days[0].list[0].weather.Wind(speed, deg)

            response.days[0].list[0].sys =
                    response.days[0].list[0].Sys(sunrise, sunset)

            return response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadCity(reader: JSONObject, weather: Weather): Weather.City {
        var cityID: Long? = null
        var name: String? = null
        var country: String? = null

        if (reader.has("id")) cityID = reader.getLong("id")
        if (reader.has("name")) name = reader.getString("name")
        if (reader.has("sys")) {
            val sys = reader.getJSONObject("sys")

            if (sys.has("country")) country = reader.getString("country")
        }

        return weather.City(cityID, name, country)
    }

    private fun loadCoord(reader: JSONObject, weather: Weather): Weather.City.Coord {
        var lat: Double? = null
        var lon: Double? = null

        if (reader.has("coord")) {
            val coord = reader.getJSONObject("coord")

            if (coord.has("lat")) lat = coord.getDouble("lat")
            if (coord.has("lon")) lon = coord.getDouble("lon")
        }

        return weather.city.Coord(lon, lat)

    }

    private fun loadWeather(reader: JSONObject, weather: Weather): Weather.Day.List.Weather {
        return if (reader.has("weather")) {
            val weatherArr = reader.getJSONArray("weather")
            val weatherObj = weatherArr.getJSONObject(0)

            val id = if (weatherObj.has("id")) weatherObj.getInt("id")
            else null

            val weatherMain = if (weatherObj.has("main")) weatherObj.getString("main")
            else null

            val description = if (weatherObj.has("description")) weatherObj.getString("description")
            else null

            val icon = if (weatherObj.has("icon")) weatherObj.getString("icon")
            else null

            weather.days[0].list[0].Weather(id, weatherMain, description, icon)
        } else weather.days[0].list[0].Weather(null, null, null, null)
    }

    private fun loadWind(reader: JSONObject, weather: Weather): Weather.Day.List.Weather.Wind {
        var speed: Double? = null
        var deg: Double? = null


    }
}