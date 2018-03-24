package exeter.sm807.weather

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.WindowManager
import org.json.JSONException


/**
 * Created by sebltm on 09/03/2018.
 */

class ForecastActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Any?>, OnColorChange {
    private var mDrawer: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null

    private lateinit var weather: Weather
    internal lateinit var mViewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var colors: IntArray
    private var forecast: Weather? = null

    companion object {
        private const val SAVE_FORECAST = 1
        private const val FORECAST_LOADER = 0
        private const val FORECAST_LOADER_OFF = 2
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

        navigationView!!.menu.findItem(R.id.forecast).isChecked = true
        supportActionBar?.title = "5 day forecast"
        mViewPager = findViewById(R.id.pager)

        tabLayout = findViewById(R.id.tabs)
        tabLayout.setupWithViewPager(mViewPager, true)
        tabLayout.setTabTextColors(Color.WHITE, Color.WHITE)

        tabToOpen = (intent?.extras?.get("day") ?: 0) as Int

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any?> {
        return when (id) {
            FORECAST_LOADER -> ForecastLoader(args, this)
            SAVE_FORECAST -> SaveWeather(this, args, weather)
            else -> WeatherLoaderOffline(this, args)
        }
    }

    private fun buildAdapter(data: Weather): PagerAdapter {
        return ForecastAdapter(supportFragmentManager, data, this)
    }

    override fun onLoadFinished(loader: Loader<Any?>, data: Any?) {
        val bundle = Bundle()
        bundle.putInt("type", CurrentWeatherActivity.FORECAST)

        if (loader.id == FORECAST_LOADER) {
            if (data != null && data != forecast) {
                displayForecast(data as Weather)
                weather = data
                supportLoaderManager.initLoader(SAVE_FORECAST, bundle, this)
            } else if (data == null) {
                supportLoaderManager.initLoader(FORECAST_LOADER_OFF, bundle, this)

                println("No internet")
                val layout = findViewById<ViewPager>(R.id.pager)
                Snackbar.make(layout,
                        "No internet. Loading last available data.",
                        Snackbar.LENGTH_LONG)
                        .show()
            } else {
                supportLoaderManager.initLoader(FORECAST_LOADER_OFF, bundle, this)
            }
        } else if (loader.id == FORECAST_LOADER_OFF && data == null) {
            /**
             * Should there be no data available at all:
             *  1. No internet connection
             *  2. No saved data in the database
             *
             * Create an "empty" Weather object and display it where necessary
             */
            displayForecast(Weather().emptyWeather())
        } else if (loader.id == FORECAST_LOADER_OFF) {
            displayForecast(data as Weather)
        }
    }

    private fun displayForecast(data: Weather) {
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

                }
            })

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onLoaderReset(loader: Loader<Any?>) {

    }

    override fun onResume() {
        super.onResume()

        supportLoaderManager.initLoader(FORECAST_LOADER, null, this)
    }

    override fun onViewPagerColorChange(color: ColorDrawable) {
        mViewPager.background = color
    }

    override fun onScrollColorChange(color: ColorDrawable, position: Int) {
        colors[position] = color.color
    }
}