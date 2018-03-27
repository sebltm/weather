package exeter.sm807.weather

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONException
import java.util.*


class CurrentWeatherActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Any>, OnForecastSelected, SharedPreferences.OnSharedPreferenceChangeListener {
    private var mDrawer: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private lateinit var preferences: SharedPreferences

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
        /**
         * Loader variables
         * FORECAST and CURRENT are type for saving to DB
         * All others are different types of loaders
         */
        const val FORECAST = 1
        private const val CURRENT = 0
        private const val CURRENT_WEATHER_LOADER = 0
        private const val FORECAST_LOADER = 1
        private const val SAVE_FORECAST = 2
        private const val SAVE_CURRENT = 4
        private const val FORECAST_LOADER_OFF = 3
        private const val CURRENT_WEATHER_LOADER_OFF = 5
        private const val QUICK_WEATHER_LOADER = 6
        private const val PULL_TO_REFRESH_CURRENT = 7
        private const val PULL_TO_REFRESH_FORECAST = 8


        /**
         * @param list: the list where to retrieve the timestamp for the day
         * @param format: the format of the string (Calendar.LONG/Calendar.SHORT)
         * @return String containing either Today, Tomorrow/TMRW or the day if its further in the future
         */
        fun getDayName(list: Weather.Day.List, format: Int): String {
            val mills = (list.dt ?: (System.currentTimeMillis() / 1000L)) * 1000L

            val c1 = UTCCal(System.currentTimeMillis() / 1000L)
            c1.add(Calendar.DAY_OF_YEAR, 1)

            val c2 = UTCCal(list.dt ?: (System.currentTimeMillis() / 1000L))

            return if (DateUtils.isToday(mills)) "Today"
            else if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)) {
                if (format == Calendar.LONG) {
                    "Tomorrow"
                } else {
                    "TMRW"
                }
            } else c2.getDisplayName(Calendar.DAY_OF_WEEK, format, Locale.UK)
        }

        /**
         * Check if the data is stale based on the timestamp of the data and the users preference
         * @param timestamp: the timestamp of when the data was last retrieved
         * @param activity: to be able to getSharedPreferences
         * @return true if the data stale, false if it isn't
         * Warning! Checking the data too often might result in the API key running out of requests
         */
        fun isDataStale(timestamp: Long, activity: AppCompatActivity): Boolean {
            val preferences = activity.getSharedPreferences("data", Context.MODE_PRIVATE)
            val index = preferences.getInt("stale_data", 3)

            val timeCal = UTCCal(timestamp)
            val staleLimit = UTCCal(System.currentTimeMillis() / 1000L)

            when (index) {
                GeneralSettingsFragment.TWELVE_HOURS -> timeCal.add(Calendar.HOUR, 12)
                GeneralSettingsFragment.FIVE_HOURS -> timeCal.add(Calendar.HOUR, 5)
                GeneralSettingsFragment.THREE_HOURS -> timeCal.add(Calendar.HOUR, 3)
                GeneralSettingsFragment.ONE_HOUR -> timeCal.add(Calendar.HOUR, 1)
                GeneralSettingsFragment.THIRTY_MINUTES -> timeCal.add(Calendar.MINUTE, 30)
                GeneralSettingsFragment.TEN_MINUTES -> timeCal.add(Calendar.MINUTE, 10)
                GeneralSettingsFragment.FIVE_MINUTES -> timeCal.add(Calendar.MINUTE, 5)
                GeneralSettingsFragment.ONE_MINUTE -> timeCal.add(Calendar.MINUTE, 1)
                GeneralSettingsFragment.ALWAYS -> return true
            }

            return staleLimit.time.time > timeCal.time.time
        }

        /**
         * Return a UTC Calendar based on timestamp
         * @param timestamp: the timestamp from which to create the calendar
         * @return Calendar in UTC timezone
         */
        fun UTCCal(timestamp: Long): Calendar {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.time = Date(timestamp * 1000L)
            cal.add(Calendar.MILLISECOND, TimeZone.getDefault().rawOffset)

            return cal
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.current_weather_activity)

        /**
         * Initialize the custom toolbar and set it as the action bar
         */
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        /**
         * Initialize the drawer and synchronize toolbar menu button and drawer opening/closing
         */
        mDrawer = findViewById(R.id.drawer_layout)
        mDrawerToggle = ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close)
        mDrawer!!.addDrawerListener(mDrawerToggle!!)
        supportActionBar!!.setHomeButtonEnabled(true)
        mDrawerToggle!!.syncState()

        /**
         * Set behaviour for the navigation buttons
         */
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

        //Initialize the location shared preferences and add a listener
        preferences = getSharedPreferences("location", Context.MODE_PRIVATE)
        preferences.registerOnSharedPreferenceChangeListener(this)

        mSwipeRefreshLayout = findViewById(R.id.swiperefresh)
        mSwipeRefreshLayout?.setOnRefreshListener({
            //On swipe to refresh, launch the "pull to refresh" loaders
            val bundle = Bundle()
            bundle.putString("city", preferences.getString("city_name", "Exeter"))
            bundle.putString("country", preferences.getString("country", "UK"))
            supportLoaderManager.initLoader(PULL_TO_REFRESH_CURRENT, bundle, this)
            supportLoaderManager.initLoader(PULL_TO_REFRESH_FORECAST, bundle, this)
        })

        //Get all the current weather elements + forecast layout
        forecastLayout = findViewById(R.id.linearLayout)
        mainWeatherDegrees = findViewById(R.id.main_weather_degrees)
        mainWeatherLocation = findViewById(R.id.main_weather_location)
        mainWeatherIcon = findViewById(R.id.weatherIcon)
        mainWeatherIcon?.setImageResource(R.drawable.cloud)
        mainWeatherHumidity = findViewById(R.id.main_weather_humidity)
        mainWeatherDesc = findViewById(R.id.main_weather_desc)
        mainWeatherWind = findViewById(R.id.main_weather_wind)
        mainWeatherPressure = findViewById(R.id.main_weather_pressure)

        //Set proper title
        this.supportActionBar?.title = "Current weather conditions"

        //Start the offline loaders
        startLoaders()
    }

    /**
     * @param id : id of the loader
     * @param args : bundle of arguments for the loader
     */
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any> {
        return when (id) {
            PULL_TO_REFRESH_CURRENT -> CurrentWeatherLoader(args, this)
            PULL_TO_REFRESH_FORECAST -> ForecastLoader(args, this)
            QUICK_WEATHER_LOADER -> CurrentWeatherLoader(args, this)
            CURRENT_WEATHER_LOADER -> CurrentWeatherLoader(args, this)
            FORECAST_LOADER -> ForecastLoader(args, this)
            SAVE_FORECAST -> SaveWeather(this, args, forecast!!)
            FORECAST_LOADER_OFF -> WeatherLoaderOffline(this, args)
            SAVE_CURRENT -> SaveWeather(this, args, current!!)
            else -> WeatherLoaderOffline(this, args)
        }
    }

    override fun onLoadFinished(loader: Loader<Any>, data: Any?) {
        //Show the user that the data is loading
        if (!(mSwipeRefreshLayout!!.isRefreshing)) mSwipeRefreshLayout?.isRefreshing = true

        if (loader.id == CURRENT_WEATHER_LOADER) {
            val bundle = Bundle()
            bundle.putInt("type", CURRENT)

            if (data != null) {
                // Only process the data if it's not null
                current = data as Weather
                displayCurrent(data)
                supportLoaderManager.initLoader(SAVE_CURRENT, bundle, this)
            } else {
                //If data is null, then say there is no data available and show dummy data
                displayCurrent(Weather().emptyWeather())
                val layout = findViewById<CoordinatorLayout>(R.id.coordinator_layout)
                Snackbar.make(layout,
                        "No data available.",
                        Snackbar.LENGTH_LONG)
                        .show()
            }
        } else if (loader.id == FORECAST_LOADER) {
            val bundle = Bundle()
            bundle.putInt("type", FORECAST)

            if (data != null) {
                // Only process the data if it's not null
                forecast = data as Weather
                displayForecast(data)
                supportLoaderManager.initLoader(SAVE_FORECAST, bundle, this)
            } else {
                //If data is null, then say there is no data available and show dummy data
                displayForecast(Weather().emptyWeather())
                val layout = findViewById<CoordinatorLayout>(R.id.coordinator_layout)
                Snackbar.make(layout,
                        "No data available.",
                        Snackbar.LENGTH_LONG)
                        .show()
            }
        } else if (loader.id == FORECAST_LOADER_OFF || loader.id == CURRENT_WEATHER_LOADER_OFF) {
            /**
             * Get the data from the database. If there is no data (database not existing, empty or failure),
             * then try to get the data from online.
             */

            if (data == null) {
                val bundle = Bundle()
                bundle.putString("city", preferences.getString("city_name", "Exeter"))
                bundle.putString("country", preferences.getString("country", "UK"))

                if (loader.id == FORECAST_LOADER_OFF) {
                    supportLoaderManager.initLoader(FORECAST_LOADER, bundle, this)
                } else {
                    supportLoaderManager.initLoader(CURRENT_WEATHER_LOADER, bundle, this)
                }
            } else {
                data as Weather

                //If there is data but it is stale (based on user preference), fetch new online data
                if (isDataStale(data.time, this)) {
                    val bundle = Bundle()
                    bundle.putString("city", preferences.getString("city_name", "Exeter"))
                    bundle.putString("country", preferences.getString("country", "UK"))
                    if (loader.id == FORECAST_LOADER_OFF) {
                        supportLoaderManager.initLoader(FORECAST_LOADER, bundle, this)
                    } else {
                        supportLoaderManager.initLoader(CURRENT_WEATHER_LOADER, bundle, this)
                    }
                } else {
                    if (loader.id == FORECAST_LOADER_OFF) displayForecast(data)
                    else displayCurrent(data)
                }
            }
        } else if (loader.id == QUICK_WEATHER_LOADER) {
            //Display the quick weather or dummy data if empty
            if (data == null) {
                displayQuickWeather(Weather().emptyWeather())
            } else {
                data as Weather
                displayQuickWeather(data)
            }
        } else if (loader.id == PULL_TO_REFRESH_CURRENT || loader.id == PULL_TO_REFRESH_FORECAST) {
            /**
             * Fetch data from online and store it in the DB. If there is no data, then show the
             * last available from the database
             */

            if (data == null) {
                if (loader.id == PULL_TO_REFRESH_CURRENT) {
                    val current = Bundle()
                    current.putString("city", preferences.getString("city_name", "Exeter"))
                    current.putString("country", preferences.getString("country", "UK"))
                    current.putInt("type", CURRENT)
                    supportLoaderManager.initLoader(CURRENT_WEATHER_LOADER_OFF, current, this)
                } else {
                    val forecast = Bundle()
                    forecast.putString("city", preferences.getString("city_name", "Exeter"))
                    forecast.putString("country", preferences.getString("country", "UK"))
                    forecast.putInt("type", FORECAST)
                    supportLoaderManager.initLoader(FORECAST_LOADER_OFF, forecast, this)
                }

                val layout = findViewById<CoordinatorLayout>(R.id.coordinator_layout)
                Snackbar.make(layout,
                        "No Internet available, showing last available data.",
                        Snackbar.LENGTH_LONG)
                        .show()
            } else {
                if (loader.id == PULL_TO_REFRESH_CURRENT) {
                    val bundle = Bundle()
                    bundle.putInt("type", CURRENT)

                    current = data as Weather
                    displayCurrent(data)
                    supportLoaderManager.initLoader(SAVE_CURRENT, bundle, this)
                    displayCurrent(data)
                } else {
                    val bundle = Bundle()
                    bundle.putInt("type", FORECAST)

                    forecast = data as Weather
                    displayForecast(data)
                    supportLoaderManager.initLoader(SAVE_FORECAST, bundle, this)
                    displayForecast(data)
                }
            }
        }

        //Tell user we are done refreshing
        if (mSwipeRefreshLayout!!.isRefreshing) mSwipeRefreshLayout?.isRefreshing = false
    }

    private fun displayCurrent(data: Weather) {
        /**
         * Fill the "current" weather with the data and upgrade the background color
         */

        runOnUiThread({
            try {
                mainWeatherDegrees!!.text = resources.getString(R.string.temp_placeholder, data.days[0].list[0].getTemp())
                mainWeatherLocation!!.text = data.city.name
                mainWeatherHumidity!!.text = resources.getString(R.string.humidity_placeholder, data.days[0].list[0].getHumidity())
                mainWeatherDesc!!.text = data.days[0].list[0].weather.getBuiltDescription()

                if (data.days[0].list[0].weather.wind.getDeg() == null) {
                    mainWeatherWind!!.text = getString(R.string.no_wind)
                } else {
                    mainWeatherWind!!.text = resources.getString(
                            R.string.wind_placeholder,
                            data.days[0].list[0].weather.wind.getDeg(),
                            data.days[0].list[0].weather.wind.getSpeed())
                }
                mainWeatherPressure!!.text = resources.getString(R.string.pressure_placeholder, data.days[0].list[0].getPressure())
                mainWeatherIcon!!.setImageResource(
                        data.days[0].list[0].weather.updateWeatherIcon() ?: R.drawable.clouds)
                colorTo = Color.parseColor(data.days[0].list[0].weather.backgroundColor())

                updateBackgroundColor()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        })
    }

    private fun displayQuickWeather(data: Weather) {
        /**
         * Fill the quick weather display with the appropriate data
         */

        val quickTemp: TextView? = mDrawer?.findViewById(R.id.quickWeatherTemp)
        val quickLoc: TextView? = mDrawer?.findViewById(R.id.quickWeatherLoc)
        val quickIco: ImageView? = mDrawer?.findViewById(R.id.quickWeatherIco)
        val layout: ConstraintLayout? = mDrawer?.findViewById(R.id.qw_layout)

        val color = Color.parseColor(data.days[0].list[0].weather.backgroundColor())

        quickTemp?.text = resources.getString(R.string.temp_placeholder, data.days[0].list[0].getTemp())
        quickLoc?.text = data.city.name
        quickIco?.setImageResource(
                data.days[0].list[0].weather.updateWeatherIcon() ?: R.drawable.clouds)
        layout?.background = ColorDrawable(color)
    }

    private fun displayForecast(data: Weather) {
        /**
         * Fill the forecast display by only inflating as many layouts as needed
         */
        runOnUiThread({
            try {
                //Three days on portrait, 5 on landscape
                var daysToDisplay = 3
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    daysToDisplay = data.days.size
                }

                //Show only as many as actually exist at most
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
                         * If the user taps on a day, take him to the forecast activity on the fragment
                         * of the corresponding day
                         */
                        val forecastIntent = Intent(this, ForecastActivity::class.java)
                        forecastIntent.putExtra("day", i)
                        startActivity(forecastIntent)
                    }

                    val dayLayout = day.findViewById<LinearLayout>(R.id.dayLayout)

                    val dayStr = dayLayout.findViewById<TextView>(R.id.day)
                    val list = data.days[i].list

                    dayStr.text = getDayName(list[0], Calendar.LONG)

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

                        val cal = UTCCal(hour.dt ?: System.currentTimeMillis() / 1000L)
                        time.text = resources.getString(R.string.time_placeholder, cal[Calendar.HOUR_OF_DAY])

                        weatherIcon.setImageResource(hour.weather.updateWeatherIcon()
                                ?: R.drawable.clouds)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        })
    }

    override fun onLoaderReset(loader: Loader<Any>) {

    }

    private fun updateBackgroundColor() {
        /**
         * Animate the background color from grey to the required background color
         * Applies to backround color, status bar, background
         */

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
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

    /**
     * OnResume, reload the data and make sure the navigation drawer is also up to date
     */
    override fun onResume() {
        super.onResume()
        startLoaders()
        navigationView!!.menu.findItem(R.id.current).isChecked = true
    }

    /**
     * If the shared preferences change, reload with the new data
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val bundle = Bundle()
        bundle.putString("city", preferences.getString("city_name", "Exeter"))
        bundle.putString("country", preferences.getString("country", "UK"))
        supportLoaderManager.initLoader(CURRENT_WEATHER_LOADER, bundle, this)
        supportLoaderManager.initLoader(FORECAST_LOADER, bundle, this)
    }

    private fun startLoaders() {
        val bundle = Bundle()
        bundle.putString("city", preferences.getString("qw_city", "Paris"))
        bundle.putString("country", preferences.getString("qw_country", "France"))
        supportLoaderManager.initLoader(QUICK_WEATHER_LOADER, bundle, this@CurrentWeatherActivity)

        val current = Bundle()
        current.putString("city", preferences.getString("city_name", "Exeter"))
        current.putString("country", preferences.getString("country", "UK"))
        current.putInt("type", CURRENT)
        supportLoaderManager.initLoader(CURRENT_WEATHER_LOADER_OFF, current, this)

        val forecast = Bundle()
        forecast.putString("city", preferences.getString("city_name", "Exeter"))
        forecast.putString("country", preferences.getString("country", "UK"))
        forecast.putInt("type", FORECAST)
        supportLoaderManager.initLoader(FORECAST_LOADER_OFF, forecast, this)
    }
}