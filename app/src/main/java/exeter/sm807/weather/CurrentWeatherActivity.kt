package exeter.sm807.weather

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONException
import java.util.*


class CurrentWeatherActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Any>, OnForecastSelected {
    private var mDrawer: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.current_weather_activity)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mDrawer = findViewById(R.id.drawer_layout)
        mDrawerToggle = ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close)
        mDrawer!!.addDrawerListener(mDrawerToggle!!)
        supportActionBar!!.setHomeButtonEnabled(true)
        mDrawerToggle!!.syncState()

        navigationView = findViewById(R.id.nav_view)
        navigationView!!.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawer!!.closeDrawers()

            when (menuItem.itemId) {
                R.id.forecast -> {
                    val forecastIntent = Intent(this, ForecastActivity::class.java)
                    startActivity(forecastIntent)
                }

                R.id.settings -> {
                    val settingsIntent = Intent(this, SettingsActivity::class.java)
                    startActivity(settingsIntent)
                }
            }

            true
        }

        mDrawer!!.addDrawerListener(
                object : DrawerLayout.DrawerListener {
                    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                        // Respond when the drawer's position changes
                    }

                    override fun onDrawerOpened(drawerView: View) {
                        // Respond when the drawer is opened
                    }

                    override fun onDrawerClosed(drawerView: View) {
                        // Respond when the drawer is closed
                    }

                    override fun onDrawerStateChanged(newState: Int) {
                        // Respond when the drawer motion state changes
                    }
                }
        )

        forecastLayout = findViewById(R.id.linearLayout)

        mainWeatherDegrees = findViewById(R.id.main_weather_degrees)
        mainWeatherLocation = findViewById(R.id.main_weather_location)
        mainWeatherIcon = findViewById(R.id.weatherIcon)
        mainWeatherIcon?.setImageResource(R.drawable.cloud)
        mainWeatherHumidity = findViewById(R.id.main_weather_humidity)
        mainWeatherDesc = findViewById(R.id.main_weather_desc)
        mainWeatherWind = findViewById(R.id.main_weather_wind)
        mainWeatherPressure = findViewById(R.id.main_weather_pressure)

        this.supportActionBar?.title = "Current weather conditions"

        supportLoaderManager.initLoader(CURRENT_WEATHER_LOADER, null, this)
        supportLoaderManager.initLoader(FORECAST_LOADER, null, this)
    }

    /**
     * @param id : id of the loader
     * @param args : bundle of arguments for the loader
     */
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
        return when (id) {
            CURRENT_WEATHER_LOADER -> CurrentWeatherLoader(args, this)
            FORECAST_LOADER -> ForecastLoader(args, this)
            SAVE_FORECAST -> SaveWeather(this, args, forecast!!)
            FORECAST_LOADER_OFF -> WeatherLoaderOffline(this, args)
            SAVE_CURRENT -> SaveWeather(this, args, current!!)
            else -> WeatherLoaderOffline(this, args)
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
                supportLoaderManager.initLoader(SAVE_CURRENT, bundle, this)
            } else if (data == current && data != null) {
                supportLoaderManager.initLoader(CURRENT_WEATHER_LOADER_OFF, bundle, this)
            } else {
                /**Load previous data from database*/
                supportLoaderManager.initLoader(CURRENT_WEATHER_LOADER_OFF, bundle, this)

                println("No internet")
                val layout = findViewById<ConstraintLayout>(R.id.constraintLayout)
                Snackbar.make(layout,
                        "No internet. Loading last available data.",
                        Snackbar.LENGTH_LONG)
                        .show()
            }
        } else if (loader.id == FORECAST_LOADER) {
            val bundle = Bundle()
            bundle.putInt("type", FORECAST)

            if (data != null && data != forecast) {
                forecast = data as Weather
                displayForecast(data)
                supportLoaderManager.initLoader(SAVE_FORECAST, bundle, this)
            } else if (data == current && data != null) {
                supportLoaderManager.initLoader(FORECAST_LOADER_OFF, bundle, this)
            } else if (data == null) {
                /**Load previous data from database*/
                supportLoaderManager.initLoader(FORECAST_LOADER_OFF, bundle, this)

                /**Show warning only if no data was received*/
                println("No internet")
                val layout = findViewById<ConstraintLayout>(R.id.constraintLayout)
                Snackbar.make(layout,
                        "No internet. Loading last available data.",
                        Snackbar.LENGTH_LONG)
                        .show()
            }
        } else if ((loader.id == FORECAST_LOADER_OFF || loader.id == CURRENT_WEATHER_LOADER_OFF) && data == null) {
            /**
             * Should there be no data available at all:
             *  1. No internet connection
             *  2. No saved data in the database
             *
             * Create an "empty" Weather object and display it where necessary
             */

            if (loader.id == FORECAST_LOADER_OFF) {
                displayForecast(Weather().emptyWeather())
            } else if (loader.id == CURRENT_WEATHER_LOADER_OFF) {
                displayCurrent(Weather().emptyWeather())
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
            mainWeatherDegrees!!.text = resources.getString(R.string.temp_placeholder, data.days[0].list[0].getTemp())
            mainWeatherLocation!!.text = data.city.name
            mainWeatherHumidity!!.text = resources.getString(R.string.humidity_placeholder, data.days[0].list[0].getHumidity())
            mainWeatherDesc!!.text = data.days[0].list[0].weather.getBuiltDescription()
            mainWeatherWind!!.text = resources.getString(
                    R.string.wind_placeholder,
                    data.days[0].list[0].weather.wind.getDeg(),
                    data.days[0].list[0].weather.wind.getSpeed())
            mainWeatherPressure!!.text = resources.getString(R.string.pressure_placeholder, data.days[0].list[0].getPressure())
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
                 * Add a forecast_activity listener to each to open the forecast_activity fragment
                 * Display the relevant data in the layout
                 */
                val day = layoutInflater.inflate(R.layout.forecast_short, forecastLayout, false)
                forecastLayout!!.addView(day)

                day.setOnClickListener {
                    mOnForecastSelected?.onForecastSelected()

                    /**
                     * Only create a new "forecastFragment" if it does not
                     * already exist in the FragmentManager, otherwise replace
                     * with the existing forecastFragment
                     */
                    val forecastIntent = Intent(this, ForecastActivity::class.java)
                    forecastIntent.putExtra("day", i)
                    startActivity(forecastIntent)
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

                dayStr.text = if (DateUtils.isToday(mills)) "Today"
                else if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)) "Tomorrow"
                else c2.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.UK)

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
                    temp.text = String.format(resources.getString(R.string.temp_placeholder, hour.getTemp()))

                    val date = Calendar.getInstance()
                    date.time = Date(hour.dt * 1000L)
                    time.text = String.format(resources.getString(R.string.time_placeholder), date[Calendar.HOUR_OF_DAY])

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

        val window = window

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        val colorFrom = Color.parseColor("#A9A9A9")
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 500 // milliseconds
        colorAnimation.addUpdateListener { animator ->
            window.decorView.setBackgroundColor(animator.animatedValue as Int)
            supportActionBar
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


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle!!.onOptionsItemSelected(item)) return true

        when (item.itemId) {
            android.R.id.home -> {
                mDrawer!!.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle!!.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onForecastSelected() {
        navigationView!!.menu.findItem(R.id.forecast).isChecked = true
    }

    override fun onResume() {
        super.onResume()

        navigationView!!.menu.findItem(R.id.current).isChecked = true
    }
}