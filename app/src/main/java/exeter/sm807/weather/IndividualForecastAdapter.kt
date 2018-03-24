package exeter.sm807.weather

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONException
import java.util.*

/**
 * Created by 660046669 on 14/03/2018.
 */

class IndividualForecastAdapter internal constructor(private var dayData: Weather.Day) : RecyclerView.Adapter<IndividualForecastAdapter.ViewHolder>() {

    class ViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
        var time: TextView = view.findViewById(R.id.time)
        var temp: TextView = view.findViewById(R.id.main_weather_degrees)
        var humidity: TextView = view.findViewById(R.id.main_weather_humidity)
        var hpa: TextView = view.findViewById(R.id.main_weather_pressure)
        var wind: TextView = view.findViewById(R.id.main_weather_wind)
        var desc: TextView = view.findViewById(R.id.main_weather_desc)
        var ico: ImageView = view.findViewById(R.id.weather_ico)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndividualForecastAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.hour_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: IndividualForecastAdapter.ViewHolder, position: Int) {
        try {
            val hourData = dayData.list[position]

            holder.temp.text = hourData.getTemp()
            holder.desc.text = hourData.weather.getBuiltDescription()
            holder.humidity.text = hourData.getHumidity()
            holder.hpa.text = hourData.getPressure()
            holder.wind.text = hourData.weather.wind?.getWind()

            val cal = Calendar.getInstance()
            cal.time = Date(hourData.dt * 1000L)
            holder.time.text = "${cal.get(Calendar.HOUR_OF_DAY)}h"

            val colorTo: Int
            colorTo = if (position == dayData.list.size - 1) {
                Color.parseColor(hourData.weather.backgroundColor())
            } else {
                Color.parseColor(dayData.list[position + 1].weather.backgroundColor())
            }

            val colorFrom = Color.parseColor(hourData.weather.backgroundColor())
            val colors = intArrayOf(colorFrom, colorTo)

            holder.view.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)

            holder.ico.setImageResource(hourData.weather.updateWeatherIcon())
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    override fun getItemCount(): Int {
        return dayData.list.size
    }
}