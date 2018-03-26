package exeter.sm807.weather

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
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
import java.util.*


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
        private const val SAVE_FORECAST = 1
        private const val FORECAST_LOADER = 0
        private const val FORECAST_LOADER_OFF = 2
        private const val QUICK_WEATHER_LOADER = 3
        private var tabToOpen: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forecast_activity)

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

        preferences = getSharedPreferences("location", Context.MODE_PRIVATE)

        mSwipeRefreshLayout = findViewById(R.id.swiperefresh)
        mSwipeRefreshLayout?.setOnRefreshListener({
            // This method performs the actual data-refresh operation.
            // The method calls setRefreshing(false) when it's finished.
            val forecast = Bundle()
            forecast.putString("city", preferences.getString("city_name", "Exeter"))
            forecast.putString("country", preferences.getString("country", "UK"))
            supportLoaderManager.initLoader(FORECAST_LOADER, forecast, this)
        })

        navigationView!!.menu.findItem(R.id.forecast).isChecked = true
        supportActionBar?.title = "5 day forecast"
        mViewPager = findViewById(R.id.pager)

        tabLayout = findViewById(R.id.tabs)
        tabLayout.setupWithViewPager(mViewPager, true)
        tabLayout.setTabTextColors(Color.WHITE, Color.WHITE)

        tabToOpen = (intent?.extras?.get("day") ?: 0) as Int

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
        if (mSwipeRefreshLayout?.isRefreshing != true) mSwipeRefreshLayout?.isRefreshing = true

        val bundle = Bundle()
        bundle.putInt("type", CurrentWeatherActivity.FORECAST)
        if (loader.id == FORECAST_LOADER) {
            when {
                data != null -> {
                    displayForecast(data as Weather)
                    weather = data
                    supportLoaderManager.initLoader(SAVE_FORECAST, bundle, this)
                }
                else -> {
                    supportLoaderManager.initLoader(FORECAST_LOADER_OFF, bundle, this)

                    val layout = findViewById<ViewPager>(R.id.pager)
                    Snackbar.make(layout,
                            "No internet. Loading last available data.",
                            Snackbar.LENGTH_LONG)
                            .show()
                }
            }
        } else if (loader.id == FORECAST_LOADER_OFF) {
            /**
             * Display database data
             */

            if (data == null) {
                val bundle = Bundle()
                bundle.putString("city", preferences.getString("city_name", "Exeter"))
                bundle.putString("country", preferences.getString("country", "UK"))
                supportLoaderManager.initLoader(FORECAST_LOADER, bundle, this)
            } else {
                data as Weather
                val timeCal = Calendar.getInstance()
                val timeData = Date(data.time)
                timeCal.time = timeData

                val staleLimit = Calendar.getInstance()
                staleLimit.time = Date(System.currentTimeMillis())
                staleLimit.add(Calendar.HOUR, 3)

                if (staleLimit.time.time < timeCal.time.time) {
                    val bundle = Bundle()
                    bundle.putString("city", preferences.getString("city_name", "Exeter"))
                    bundle.putString("country", preferences.getString("country", "UK"))
                    supportLoaderManager.initLoader(FORECAST_LOADER, bundle, this)
                } else {
                    displayForecast(data)
                }
            }
        } else if (loader.id == QUICK_WEATHER_LOADER) {
            if (data == null) {
                displayQuickWeather(Weather().emptyWeather())
            }

            data as Weather
            displayQuickWeather(data)
        }

        if (mSwipeRefreshLayout?.isRefreshing == true) mSwipeRefreshLayout?.isRefreshing = false
    }

    private fun displayForecast(data: Weather) {
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

    override fun onScrollColorChange(color: ColorDrawable, position: Int) {
        colors[position] = color.color
    }

    fun toggleRefreshing(enabled: Boolean) {
        mSwipeRefreshLayout?.isEnabled = enabled
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val forecast = Bundle()
        forecast.putString("city", preferences.getString("city_name", "Exeter"))
        forecast.putString("country", preferences.getString("country", "UK"))
        supportLoaderManager.initLoader(FORECAST_LOADER, forecast, this)
    }
}