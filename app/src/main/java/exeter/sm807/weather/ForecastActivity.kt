package exeter.sm807.weather

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import org.json.JSONException


/**
 * Created by 660046669 on 09/03/2018.
 */

class ForecastActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Any?>, OnColorChange, SharedPreferences.OnSharedPreferenceChangeListener {
    private var mDrawer: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private lateinit var preferences: SharedPreferences

    private lateinit var weather: Weather
    internal lateinit var mViewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var colors: IntArray

    companion object {
        /**
         * Loader variables
         * FORECAST and CURRENT are type for saving to DB
         * All others are different types of loaders
         */
        private const val SAVE_FORECAST = 1
        private const val FORECAST_LOADER = 0
        private const val FORECAST_LOADER_OFF = 2
        private const val QUICK_WEATHER_LOADER = 3
        private const val PULL_TO_REFRESH_FORECAST = 4
        private var tabToOpen: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forecast_activity)

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
                R.id.current -> {
                    val currentActivity = Intent(this, CurrentWeatherActivity::class.java)
                    startActivity(currentActivity)
                }

                R.id.settings -> {
                    val settingsActivity = Intent(this, SettingsActivity::class.java)
                    startActivity(settingsActivity)
                }
            }

            true
        }

        //Initialize the location shared preferences and add a listener
        preferences = getSharedPreferences("location", Context.MODE_PRIVATE)
        preferences.registerOnSharedPreferenceChangeListener(this)

        mSwipeRefreshLayout = findViewById(R.id.swiperefresh)
        mSwipeRefreshLayout?.setOnRefreshListener({
            //On swipe to refresh, launch the "pull to refresh" loader
            val forecast = Bundle()
            forecast.putString("city", preferences.getString("city_name", "Exeter"))
            forecast.putString("country", preferences.getString("country", "UK"))
            supportLoaderManager.initLoader(PULL_TO_REFRESH_FORECAST, forecast, this)
        })

        //Initialize the navigation view (make sure the right item is check)
        navigationView!!.menu.findItem(R.id.forecast).isChecked = true
        mViewPager = findViewById(R.id.pager)
        tabLayout = findViewById(R.id.tabs)
        tabLayout.setupWithViewPager(mViewPager, true)
        tabLayout.setTabTextColors(Color.WHITE, Color.WHITE)

        //Set title of the action bar
        supportActionBar?.title = "5 day forecast"

        //Check if there is a request for a specific day to open
        tabToOpen = (intent?.extras?.get("day") ?: 0) as Int

        //Launch the loaders
        val bundle = Bundle()
        bundle.putString("city", preferences.getString("qw_city", "Paris"))
        bundle.putString("country", preferences.getString("qw_country", "France"))
        supportLoaderManager.initLoader(QUICK_WEATHER_LOADER, bundle, this@ForecastActivity)

        val forecast = Bundle()
        forecast.putString("city", preferences.getString("city_name", "Exeter"))
        forecast.putString("country", preferences.getString("country", "UK"))
        forecast.putInt("type", CurrentWeatherActivity.FORECAST)
        supportLoaderManager.initLoader(FORECAST_LOADER_OFF, forecast, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any?> {
        return when (id) {
            PULL_TO_REFRESH_FORECAST -> ForecastLoader(args, this)
            QUICK_WEATHER_LOADER -> CurrentWeatherLoader(args, this)
            FORECAST_LOADER -> ForecastLoader(args, this)
            SAVE_FORECAST -> SaveWeather(this, args, weather)
            else -> WeatherLoaderOffline(this, args)
        }
    }

    private fun buildAdapter(data: Weather): PagerAdapter {
        return ForecastAdapter(supportFragmentManager, data, this)
    }

    override fun onLoadFinished(loader: Loader<Any?>, data: Any?) {
        //Show the user that the data is loading
        if (mSwipeRefreshLayout?.isRefreshing != true) mSwipeRefreshLayout?.isRefreshing = true

        val bundle = Bundle()
        bundle.putInt("type", CurrentWeatherActivity.FORECAST)
        if (loader.id == FORECAST_LOADER) {
            when {
                data != null -> {
                    // Only process the data if it's not null
                    displayForecast(data as Weather)
                    weather = data
                    supportLoaderManager.initLoader(SAVE_FORECAST, bundle, this)
                }
                else -> {
                    //If data is null, then say there is no data available and show dummy data
                    displayForecast(Weather().emptyWeather())
                    val layout = findViewById<ViewPager>(R.id.coordinator_layout)
                    Snackbar.make(layout,
                            "No data available.",
                            Snackbar.LENGTH_LONG)
                            .show()
                }
            }
        } else if (loader.id == FORECAST_LOADER_OFF) {
            /**
             * Get the data from the database. If there is no data (database not existing, empty or failure),
             * then try to get the data from online.
             */

            if (data == null) {
                val bundle = Bundle()
                bundle.putString("city", preferences.getString("city_name", "Exeter"))
                bundle.putString("country", preferences.getString("country", "UK"))
                supportLoaderManager.initLoader(FORECAST_LOADER, bundle, this)
            } else {
                data as Weather

                //If there is data but it is stale (based on user preference), fetch new online data
                if (CurrentWeatherActivity.isDataStale(data.time, this)) {
                    val bundle = Bundle()
                    bundle.putString("city", preferences.getString("city_name", "Exeter"))
                    bundle.putString("country", preferences.getString("country", "UK"))
                    supportLoaderManager.initLoader(FORECAST_LOADER, bundle, this)
                } else {
                    displayForecast(data)
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
        } else if (loader.id == PULL_TO_REFRESH_FORECAST) {
            /**
             * Fetch data from online and store it in the DB. If there is no data, then show the
             * last available from the database
             */

            if (data == null) {
                val forecast = Bundle()
                forecast.putString("city", preferences.getString("city_name", "Exeter"))
                forecast.putString("country", preferences.getString("country", "UK"))
                forecast.putInt("type", CurrentWeatherActivity.FORECAST)
                supportLoaderManager.initLoader(FORECAST_LOADER_OFF, forecast, this)

                val layout = findViewById<CoordinatorLayout>(R.id.coordinator_layout)
                Snackbar.make(layout,
                        "No Internet available, showing last available data.",
                        Snackbar.LENGTH_LONG)
                        .show()
            } else {
                displayForecast(data as Weather)
            }
        }

        //Tell user we are done refreshing
        if (mSwipeRefreshLayout?.isRefreshing == true) mSwipeRefreshLayout?.isRefreshing = false
    }

    private fun displayForecast(data: Weather) {
        /**
         * Display the forecast data by create the tab (ViewPager) adapter and sending it the
         * weather data. Initialize the colors of the different tabs and the animation that
         * needs to happen as a user swipes between different tabs
         */

        runOnUiThread({
            try {
                mViewPager.adapter = buildAdapter(data)

                val dayList = data.days
                colors = IntArray(dayList.size)

                for (i in 0 until dayList.size) {
                    val currentDay = dayList[i].list
                    colors[i] = Color.parseColor(currentDay[0].weather.backgroundColor())
                }
                mViewPager.currentItem = tabToOpen

                val window = window
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    internal var argbEvaluator = ArgbEvaluator()

                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                        val color: Int
                        val sBarColor: Int
                        if (position < mViewPager.adapter!!.count - 1 && position < colors.size - 1) {
                            color = argbEvaluator.evaluate(positionOffset, colors[position], colors[position + 1]) as Int

                            val factor = 0.8f
                            val a = Color.alpha(color)
                            val r = Math.round(Color.red(color) * factor)
                            val g = Math.round(Color.green(color) * factor)
                            val b = Math.round(Color.blue(color) * factor)
                            sBarColor = Color.argb(a,
                                    Math.min(r, 255),
                                    Math.min(g, 255),
                                    Math.min(b, 255))
                        } else {
                            color = colors[colors.size - 1]

                            val factor = 0.8f
                            val a = Color.alpha(color)
                            val r = Math.round(Color.red(color) * factor)
                            val g = Math.round(Color.green(color) * factor)
                            val b = Math.round(Color.blue(color) * factor)
                            sBarColor = Color.argb(a,
                                    Math.min(r, 255),
                                    Math.min(g, 255),
                                    Math.min(b, 255))

                        }

                        mViewPager.setBackgroundColor(color)
                        supportActionBar!!.setBackgroundDrawable(ColorDrawable(color))
                        window.statusBarColor = sBarColor
                    }

                    override fun onPageSelected(position: Int) {

                    }

                    //Stop conflict between ViewPager and Swipe to refresh
                    override fun onPageScrollStateChanged(state: Int) {
                        toggleRefreshing(state == ViewPager.SCROLL_STATE_IDLE)
                    }
                })

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

        quickTemp?.text = resources.getString(R.string.temp_placeholder, data.days[0].list[0].getTemp())
        quickLoc?.text = data.city.name
        quickIco?.setImageResource(
                data.days[0].list[0].weather.updateWeatherIcon() ?: R.drawable.clouds)
        layout?.background = ColorDrawable(
                Color.parseColor(data.days[0].list[0].weather.backgroundColor())
        )
    }

    override fun onLoaderReset(loader: Loader<Any?>) {

    }

    override fun onResume() {
        super.onResume()

        navigationView!!.menu.findItem(R.id.forecast).isChecked = true
    }

    override fun onViewPagerColorChange(color: ColorDrawable) {
        mViewPager.background = color
    }

    //As a user scrolls through the forecasts, the "top" color changes and needs to be updated
    override fun onScrollColorChange(color: ColorDrawable, position: Int) {
        colors[position] = color.color
    }

    fun toggleRefreshing(enabled: Boolean) {
        mSwipeRefreshLayout?.isEnabled = enabled
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        /**
         * Refresh the data if the shared preferences have changed
         */
        val forecast = Bundle()
        forecast.putString("city", preferences.getString("city_name", "Exeter"))
        forecast.putString("country", preferences.getString("country", "UK"))
        supportLoaderManager.initLoader(FORECAST_LOADER, forecast, this)
    }
}