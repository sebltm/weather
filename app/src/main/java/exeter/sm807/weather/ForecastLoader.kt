package exeter.sm807.weather

import android.content.Context
import android.os.Bundle
import android.support.v4.content.AsyncTaskLoader
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by sebltm on 18/03/2018.
 */

class ForecastLoader(private var loaderBundle: Bundle?, context: Context) : AsyncTaskLoader<Any>(context) {
    private var defaultBundle: Bundle? = null

    override fun onStartLoading() {
        if (loaderBundle == null || loaderBundle!!.isEmpty) {
            defaultBundle = Bundle()
            defaultBundle!!.putString("city", "Exeter")
            defaultBundle!!.putString("country", "UK")
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
            response = Weather()

            val data = JSONObject(out)
            val list = data.getJSONArray("list")
            val city = data.getJSONObject("city")

            var coord: JSONObject? = null
            if (data.has("coord") && !data.isNull("coord")) coord = data.getJSONObject("coord")

            var country: String? = null
            if (city.has("country") && !city.isNull("country")) country = city.getString("country")

            response.city = response.City(city.getLong("id"),
                    city.getString("name"),
                    country)
            response.city.coord = response.city.Coord(coord?.getDouble("lon"), coord?.getDouble("lat"))

            for (i in 0 until list.length()) {
                val listInd = list.getJSONObject(i)

                val dt = listInd.getLong("dt")

                //Java date uses milliseconds, need to multiply by 1000 and cast to long
                val date = Date(dt * 1000L)
                val dateFormat = SimpleDateFormat("HH", Locale.ENGLISH).format(date)

                if (dateFormat.toString().equals("00", true) || response.days.isEmpty()) {
                    response.days.add(response.Day())
                }

                val main = listInd.getJSONObject("main")
                val weatherArr = listInd.getJSONArray("weather")
                val wind = listInd.getJSONObject("wind")

                var clouds: JSONObject? = null
                if (main.has("clouds")) clouds = main.getJSONObject("clouds")

                var rain: JSONObject? = null
                if (main.has("rain")) rain = main.getJSONObject("rain")

                var snow: JSONObject? = null
                if (main.has("snow")) snow = main.getJSONObject("snow")

                response.days.last().list.add(response.days.last().List(main.getDouble("temp"),
                        main.getDouble("pressure"),
                        main.getDouble("humidity"),
                        main.getDouble("temp_min"),
                        main.getDouble("temp_max"),
                        main.getDouble("sea_level"),
                        main.getDouble("grnd_level"),
                        clouds?.getDouble("all"),
                        rain?.getDouble("3h"),
                        snow?.getDouble("3h")))
                response.days.last().list.last().dt = dt
                response.days.last().list.last().weather =
                        response.days.last().list.last().Weather(
                                (weatherArr[0] as JSONObject).getInt("id"),
                                (weatherArr[0] as JSONObject).getString("main"),
                                (weatherArr[0] as JSONObject).getString("description"),
                                (weatherArr[0] as JSONObject).getString("icon")
                        )
                response.days.last().list.last().weather.wind =
                        response.days.last().list.last().weather.Wind(
                                wind.getDouble("speed"),
                                wind.getDouble("deg")
                        )
            }

            response
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }
}