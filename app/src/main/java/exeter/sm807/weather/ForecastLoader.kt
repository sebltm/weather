package exeter.sm807.weather

import android.content.Context
import android.os.Bundle
import android.support.v4.content.AsyncTaskLoader
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Created by 660046669 on 18/03/2018.
 */

class ForecastLoader(private var loaderBundle: Bundle?, context: Context) : AsyncTaskLoader<Any>(context) {
    private var defaultBundle: Bundle? = null
    private var prevDate: Date? = null

    override fun onStartLoading() {
        if (loaderBundle == null || loaderBundle!!.isEmpty) {
            val preferences = context.getSharedPreferences("location", Context.MODE_PRIVATE)

            /**
             * If the preferences are empty, default to Exeter, UK
             */
            defaultBundle = Bundle()
            defaultBundle?.putString("city", preferences.getString("city_name", "Exeter"))
            defaultBundle?.putString("country", preferences.getString("country", "UK"))
        } else defaultBundle = loaderBundle

        forceLoad()
    }

    override fun loadInBackground(): Any? {
        val response: Weather
        val con = HttpConnection("forecast", defaultBundle!!.getString("city"),
                defaultBundle!!.getString("country"), "metric")
        val searchUrl = con.url!!
        val out = con.openConnection(searchUrl) ?: return null

        return try {
            /**
             * Try to parse the JSON data and return a full weather array, otherwise null for any error
             */
            response = Weather()
            val data = JSONObject(out)
            val list = data.getJSONArray("list")

            println(data)

            response.time = System.currentTimeMillis()

            response.city = loadCity(data, response)
            response.city.coord = loadCoord(data, response, 0)

            for (i in 0 until list.length()) {
                val listInd = list.getJSONObject(i)
                val dt = listInd.getLong("dt")

                //Java date uses milliseconds, need to multiply by 1000 and cast to long
                val date = Date(dt * 1000L)
                if (prevDate == null) prevDate = date
                val cal = CurrentWeatherActivity.UTCCal(dt)
                cal.time = date
                val prevCal = CurrentWeatherActivity.UTCCal((prevDate as Date).time)
                prevCal.time = prevDate

                if (cal.get(Calendar.DAY_OF_YEAR) != prevCal.get(Calendar.DAY_OF_YEAR) || response.days.isEmpty()) {
                    response.days.add(response.Day())
                    prevDate = date
                }

                response.days.last().list.add(loadList(data, response, i))
                response.days.last().list.last().dt = dt
                response.days.last().list.last().weather = loadWeather(data, response, i)
                response.days.last().list.last().weather.wind = loadWind(data, response, i)
                response.days.last().list.last().sys = loadSys(data, response)
            }

            response
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * The following method all have the same purpose : load the object and double check that
     * the required elements exist in the JSON before adding them, otherwise default to null
     */

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

        if (!reader.isNull("list")) {
            val list = reader.getJSONArray("list")

            if (!(list[pos] as JSONObject).isNull("main")) {
                val main = (list[pos] as JSONObject).getJSONObject("main")

                if (!main.isNull("humidity")) humidity = main.getDouble("humidity")
                if (!main.isNull("temp")) temp = main.getDouble("temp")
                if (!main.isNull("pressure")) pressure = main.getDouble("pressure")
                if (!main.isNull("temp_min")) tempMin = main.getDouble("temp_min")
                if (!main.isNull("temp_max")) tempMax = main.getDouble("temp_max")
                if (!main.isNull("sea_level")) seaLevel = main.getDouble("sea_level")
                if (!main.isNull("grnd_level")) grndLevel = main.getDouble("grnd_level")
            }

            if (!(list[pos] as JSONObject).isNull("rain")) {
                val rain = (list[pos] as JSONObject).getJSONObject("rain")
                if (!rain.isNull("3h")) rain3h = rain.getDouble("3h")
            }

            if (!(list[pos] as JSONObject).isNull("clouds")) {
                val clouds = (list[pos] as JSONObject).getJSONObject("clouds")
                if (!clouds.isNull("all")) cloudsAll = clouds.getDouble("all")
            }

            if (!(list[pos] as JSONObject).isNull("snow")) {
                val snow = (list[pos] as JSONObject).getJSONObject("snow")
                if (!snow.isNull("3h")) snow3h = snow.getDouble("3h")
            }
        }
        return weather.days.last().List(temp, pressure, humidity, tempMin, tempMax, seaLevel, grndLevel, cloudsAll, rain3h, snow3h, weather.days.size - 1)
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

    private fun loadCoord(reader: JSONObject, weather: Weather, pos: Int): Weather.City.Coord {
        var lat: Double? = null
        var lon: Double? = null

        if (!reader.isNull("coord")) {
            val coord = reader.getJSONObject("coord")

            if (!coord.isNull("lat")) lat = coord.getDouble("lat")
            if (!coord.isNull("lon")) lon = coord.getDouble("lon")
        }

        return weather.city.Coord(lon, lat)

    }

    private fun loadWeather(reader: JSONObject, weather: Weather, pos: Int): Weather.Day.List.Weather {
        var id: Int? = null
        var weatherMain: String? = null
        var description: String? = null
        var icon: String? = null

        if (!reader.isNull("list")) {
            val list = reader.getJSONArray("list")

            if (!(list[pos] as JSONObject).isNull("weather")) {
                val weatherArr = (list[pos] as JSONObject).get("weather") as JSONArray

                if (!weatherArr.isNull(0)) {
                    val weatherObj = weatherArr.getJSONObject(0)

                    if (!weatherObj.isNull("id")) id = weatherObj.getInt("id")
                    if (!weatherObj.isNull("main")) weatherMain = weatherObj.getString("main")
                    if (!weatherObj.isNull("description")) description = weatherObj.getString("description")
                    if (!weatherObj.isNull("icon")) icon = weatherObj.getString("icon")
                }
            }
        }

        return weather.days.last().list.last().Weather(id, weatherMain, description, icon)
    }

    private fun loadWind(reader: JSONObject, weather: Weather, pos: Int): Weather.Day.List.Weather.Wind {
        var speed: Double? = null
        var deg: Double? = null

        if (!reader.isNull("list")) {
            val list = reader.getJSONArray("list")

            if (!(list[pos] as JSONObject).isNull("wind")) {
                val wind = (list[pos] as JSONObject).getJSONObject("wind")

                if (!wind.isNull("speed")) speed = wind.getDouble("speed")
                if (!wind.isNull("deg")) deg = wind.getDouble("deg")
            }
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