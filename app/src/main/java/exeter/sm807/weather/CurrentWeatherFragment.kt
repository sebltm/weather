package exeter.sm807.weather

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by 660046669 on 09/03/2018.
 */

class CurrentWeatherFragment : Fragment(), LoaderManager.LoaderCallbacks<Any> {
    private var mOnForecastSelected: OnForecastSelected? = null
    private var mainWeatherIcon: ImageView? = null
    private var mainWeatherDegrees: TextView? = null
    private var mainWeatherLocation: TextView? = null
    private var mainWeatherHumidity: TextView? = null
    private var mainWeatherDesc: TextView? = null
    private var mainWeatherWind: TextView? = null
    private var mainWeatherPressure: TextView? = null
    private var forecastLayout: LinearLayout? = null
    private var forecast: Weather? = null
    private var current: Weather? = null
    private var colorTo: Int = 0
    private lateinit var mFragmentManager: FragmentManager

    companion object {
        const val FORECAST = 1
        private const val CURRENT = 0
        private const val CURRENT_WEATHER_LOADER = 0
        private const val FORECAST_LOADER = 1
        private const val SAVE_FORECAST = 2
        private const val SAVE_CURRENT = 4
        private const val FORECAST_LOADER_OFF = 3
        private const val CURRENT_WEATHER_LOADER_OFF = 5
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.current, container, false)
        forecastLayout = view.findViewById(R.id.linearLayout)

        mainWeatherDegrees = view.findViewById(R.id.main_weather_degrees)
        mainWeatherLocation = view.findViewById(R.id.main_weather_location)
        mainWeatherIcon = view.findViewById(R.id.weatherIcon)
        mainWeatherIcon?.setImageResource(R.drawable.cloud)
        mainWeatherHumidity = view.findViewById(R.id.main_weather_humidity)
        mainWeatherDesc = view.findViewById(R.id.main_weather_desc)
        mainWeatherWind = view.findViewById(R.id.main_weather_wind)
        mainWeatherPressure = view.findViewById(R.id.main_weather_pressure)

        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.title = "Current weather conditions"

        mFragmentManager = fragmentManager!!

        val bundle = Bundle()
        bundle.putString("city", "Exeter")
        bundle.putString("country", "UK")
        loaderManager.initLoader(CURRENT_WEATHER_LOADER, bundle, this)
        loaderManager.initLoader(FORECAST_LOADER, bundle, this)

        return view
    }

    /**
     * @param id : id of the loader
     * @param args : bundle of arguments for the loader
     */
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
        return when (id) {
            CURRENT_WEATHER_LOADER -> CurrentWeatherLoader(args!!, context!!)
            FORECAST_LOADER -> ForecastLoader(args!!, context!!)
            SAVE_FORECAST -> SaveWeather(context!!, args, forecast!!)
            FORECAST_LOADER_OFF -> WeatherLoaderOffline(context!!, args)
            SAVE_CURRENT -> SaveWeather(context!!, args, current!!)
            else -> WeatherLoaderOffline(context!!, args)
        }
    }

    override fun onLoadFinished(loader: Loader<Any>, data: Any?) {
        if (loader.id == CURRENT_WEATHER_LOADER) {
            /**
             * Normal behaviour to load current weather
             *  1. Fetch results from the API
             *  2.
             *      2.1 If there is new data returned
             *          2.1.1 Display the data
             *          2.1.2 Save the data to the database
             *      2.2 If there is no new data
             *          2.2.1 Try to load saved data from the database
             *          2.2.2 Display the data
             *          2.2.3 Show a warning that data is not the most recent
             */

            val bundle = Bundle()
            bundle.putInt("type", CURRENT)

            if (data != null && data != current) {
                /**Only process data if it's different from current*/

                current = data as Weather
                displayCurrent(data)
                loaderManager.initLoader(SAVE_CURRENT, bundle, this)
            } else if (data == null || data == current) {

                /**Load previous data from database*/
                loaderManager.initLoader(CURRENT_WEATHER_LOADER_OFF, bundle, this)

                /**Show warning only if no data was received*/
                if (data == null) {
                    println("No internet")
                    val layout = activity!!.findViewById<ConstraintLayout>(R.id.constraintLayout)
                    Snackbar.make(layout,
                            "No internet. Loading last available data.",
                            Snackbar.LENGTH_LONG)
                            .show()
                }
            }
        } else if (loader.id == FORECAST_LOADER) {
            val bundle = Bundle()
            bundle.putInt("type", FORECAST)

            if (data != null && data != forecast) {
                forecast = data as Weather
                displayForecast(data)
                loaderManager.initLoader(SAVE_FORECAST, bundle, this)
            } else if (data == null || data == current) {

                /**Load previous data from database*/
                loaderManager.initLoader(FORECAST_LOADER_OFF, bundle, this)

                /**Show warning only if no data was received*/
                if (data == null) {
                    println("No internet")
                    val layout = activity!!.findViewById<ConstraintLayout>(R.id.constraintLayout)
                    Snackbar.make(layout,
                            "No internet. Loading last available data.",
                            Snackbar.LENGTH_LONG)
                            .show()
                }
            }
        } else if ((loader.id == FORECAST_LOADER_OFF || loader.id == CURRENT_WEATHER_LOADER_OFF) && data == null) {
            @Suppress("NAME_SHADOWING")
                    /**
                     * Should there be no data available at all:
                     *  1. No internet connection
                     *  2. No saved data in the database
                     *
                     * Create an "empty" Weather object and display it where necessary
                     */

            val data = Weather()
            data.city = data.City(0L, "--", "--")
            data.days.add(data.Day())
            data.days.last().list.add(data.days.last().List(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            ))


            data.days.last().list.last().dt = System.currentTimeMillis() * 1000L

            data.days.last().list.last().weather =
                    data.days.last().list.last().Weather(
                            0,
                            "No data",
                            "No data",
                            "No data"
                    )

            data.days.last().list.last().weather.wind =
                    data.days.last().list.last().weather.Wind(0.0, 0.0)

            if (loader.id == CURRENT_WEATHER_LOADER_OFF) {
                displayCurrent(data)

                current = data
                val bundle = Bundle()
                bundle.putInt("type", CURRENT)
                loaderManager.initLoader(SAVE_CURRENT, bundle, this)
            } else if (loader.id == FORECAST_LOADER_OFF) {
                displayForecast(data)

                forecast = data
                val bundle = Bundle()
                bundle.putInt("type", FORECAST)
                loaderManager.initLoader(SAVE_FORECAST, bundle, this)
            }

        } else if (loader.id == FORECAST_LOADER_OFF) {
            /**
             * Display database data
             */

            data as Weather
            displayForecast(data)
        } else if (loader.id == CURRENT_WEATHER_LOADER_OFF) {
            /**
             * Display database data
             */

            data as Weather
            displayCurrent(data)
        }
    }

    private fun displayCurrent(data: Weather) {
        try {
            mainWeatherDegrees!!.text = data.days[0].list[0].getTemp()
            mainWeatherLocation!!.text = data.city.name
            mainWeatherHumidity!!.text = data.days[0].list[0].getHumidity()
            mainWeatherDesc!!.text = data.days[0].list[0].weather.getBuiltDescription()
            mainWeatherWind!!.text = data.days[0].list[0].weather.wind?.getWind()
            mainWeatherPressure!!.text = data.days[0].list[0].getPressure()
            mainWeatherIcon!!.setImageResource(
                    data.days[0].list[0].weather.updateWeatherIcon())
            colorTo = Color.parseColor(data.days[0].list[0].weather.backgroundColor())

            updateBackgroundColor()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    private fun displayForecast(data: Weather) {
        try {
            var daysToDisplay = 3
            val lp = LinearLayout.LayoutParams(0, 100)
            lp.weight = 1f
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                daysToDisplay = data.days.size
            }

            if (data.days.size < daysToDisplay) {
                daysToDisplay = data.days.size
            }

            forecastLayout!!.removeAllViews()
            for (i in 0 until daysToDisplay) {
                /**
                 * Create a "forecast_short" layout for each day to display
                 * Add a forecast listener to each to open the forecast fragment
                 * Display the relevant data in the layout
                 */
                val day = layoutInflater.inflate(R.layout.forecast_short, forecastLayout, false)
                forecastLayout!!.addView(day)

                day.setOnClickListener {
                    mOnForecastSelected?.onForecastSelected()
                    val bundle = Bundle()
                    bundle.putInt("day", i)

                    /**
                     * Only create a new "forecastFragment" if it does not
                     * already exist in the FragmentManager, otherwise replace
                     * with the existing forecastFragment
                     */
                    val fTransaction = mFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                    val fragment = mFragmentManager.findFragmentByTag("forecastLayout")
                    if (fragment == null) {
                        val forecastFragment = ForecastFragment()
                        forecastFragment.arguments = bundle
                        fTransaction.replace(R.id.fragment_parent, forecastFragment, "forecastLayout")
                                // Add this transaction to the back stack
                                .addToBackStack("forecastLayout")
                                .commit()
                    } else {
                        fragment.arguments = bundle
                        fTransaction.replace(R.id.fragment_parent, fragment, "forecastLayout")
                                .addToBackStack("forecastLayout")
                                .commit()
                    }
                }

                val dayLayout = day.findViewById<LinearLayout>(R.id.dayLayout)

                val dayStr = dayLayout.findViewById<TextView>(R.id.day)
                val list = data.days[i].list

                val mills = list[i].dt * 1000L
                val dateData = Date(mills)

                val c1 = Calendar.getInstance()
                c1.add(Calendar.DAY_OF_YEAR, +1)

                val c2 = Calendar.getInstance()
                c2.time = dateData

                dayStr.text = if (DateUtils.isToday(mills)) {
                    "Today"
                } else if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)) {
                    "Tomorrow"
                } else {
                    SimpleDateFormat("EEEE", Locale.ENGLISH).format(dateData)
                }

                val hourForecastLayout = dayLayout.findViewById<LinearLayout>(R.id.hourForecast)
                for (j in 0 until list.size) {
                    /**
                     * Inflate an "hour_forecast_short" layout for each hour in the day
                     * Display the relevant data in the layout
                     */

                    val hourLayout = layoutInflater.inflate(R.layout.hour_forecast_short, hourForecastLayout, false)
                    hourForecastLayout.addView(hourLayout)

                    val weatherIcon = hourLayout.findViewById<ImageView>(R.id.imageView)
                    val temp = hourLayout.findViewById<TextView>(R.id.temp)
                    val time = hourLayout.findViewById<TextView>(R.id.time)

                    val hour = list[j]
                    temp.text = hour.getTemp()

                    val date = SimpleDateFormat("HH", Locale.ENGLISH)
                    val dateFormat = date.format(Date(hour.dt * 1000L))

                    time.text = "${dateFormat}h"
                    weatherIcon.setImageResource(hour.weather.updateWeatherIcon())
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    override fun onLoaderReset(loader: Loader<Any>) {

    }

    private fun updateBackgroundColor() {

        val window = activity!!.window

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        val colorFrom = Color.parseColor("#A9A9A9")
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 500 // milliseconds
        colorAnimation.addUpdateListener { animator ->
            view?.setBackgroundColor(animator.animatedValue as Int)
            (activity as AppCompatActivity)
                    .supportActionBar
                    ?.setBackgroundDrawable(ColorDrawable(animator.animatedValue as Int))
            val factor = 0.8f
            val toolbarColor = animator.animatedValue as Int
            val a = Color.alpha(toolbarColor)
            val r = Math.round(Color.red(toolbarColor) * factor)
            val g = Math.round(Color.green(toolbarColor) * factor)
            val b = Math.round(Color.blue(toolbarColor) * factor)
            val color = Color.argb(a,
                    Math.min(r, 255),
                    Math.min(g, 255),
                    Math.min(b, 255))
            window.statusBarColor = color
        }
        colorAnimation.start()
    }

    override fun onResume() {
        super.onResume()

        loaderManager.initLoader(CURRENT_WEATHER_LOADER, null, this)
        loaderManager.initLoader(FORECAST_LOADER, null, this)
    }

    fun setListener(listener: OnForecastSelected) {
        mOnForecastSelected = listener
    }
}
