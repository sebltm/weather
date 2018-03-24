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
            val weatherArr = reader.getJSONArray("weather")
            val weather = weatherArr.getJSONObject(0)
            val wind = reader.getJSONObject("wind")
            val sys = reader.getJSONObject("sys")
            val coord = reader.getJSONObject("coord")

            var rain: JSONObject? = null
            if (reader.has("rain") && !reader.isNull("rain")) rain = reader.getJSONObject("rain")

            var clouds: JSONObject? = null
            if (reader.has("clouds") && !reader.isNull("clouds")) clouds = reader.getJSONObject("clouds")

            var snow: JSONObject? = null
            if (reader.has("snow") && !reader.isNull("snow")) snow = reader.getJSONObject("snow")

            val description = weather.getString("description")

            val humidity = main.getDouble("humidity")
            val temp = main.getDouble("temp")
            val name = reader.getString("name")
            val id = weather.getInt("id")
            val deg = wind.getDouble("deg")
            val speed = wind.getDouble("speed")
            val pressure = main.getDouble("pressure")
            val sunrise = sys.getLong("sunrise")
            val sunset = sys.getLong("sunset")
            val country = sys.getString("country")
            val tempMin = main.getDouble("temp_min")
            val tempMax = main.getDouble("temp_max")
            val cloudsAll = clouds?.getDouble("all")
            val rain3h = rain?.getDouble("3h")
            val snow3h = snow?.getDouble("3h")
            val weatherMain = weather.getString("main")
            val icon = weather.getString("icon")
            val cityID = reader.getLong("id")

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

            response.city = response.City(cityID, name, country)
            response.city.coord = response.city.Coord(coord.getDouble("lon"), coord.getDouble("lat"))

            response.days[0].list[0].weather =
                    response.days[0].list[0].Weather(id, weatherMain, description, icon)

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
}