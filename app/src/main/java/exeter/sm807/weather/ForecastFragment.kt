package exeter.sm807.weather

import android.animation.ArgbEvaluator
import android.content.ContentValues.TAG
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import org.json.JSONException


/**
 * Created by sebltm on 09/03/2018.
 */

class ForecastFragment : Fragment(), LoaderManager.LoaderCallbacks<Any?>, OnColorChange {

    private lateinit var weather: Weather
    internal lateinit var mViewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var colors: IntArray

    companion object {
        private const val SAVE_FORECAST = 1
        private const val FORECAST_LOADER = 0
        private const val FORECAST_LOADER_OFF = 2
        private var tabToOpen: Int = 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.forecast, container, false)
        (activity!! as AppCompatActivity).supportActionBar?.title = "5 day forecast"
        mViewPager = view.findViewById(R.id.pager)

        tabLayout = view!!.findViewById(R.id.tabs)
        tabLayout.setupWithViewPager(mViewPager, true)
        tabLayout.setTabTextColors(Color.WHITE, Color.WHITE)

        /*Handler().postDelayed(
                { tv.startAnimation(animFadein) },
                2000
        ) //will start animation in 2 seconds*/

        return view
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Any?> {
        return when (id) {
            FORECAST_LOADER -> ForecastLoader(args!!, context!!)
            SAVE_FORECAST -> SaveWeather(context!!, args, weather)
            else -> WeatherLoaderOffline(context!!, args)
        }
    }

    private fun buildAdapter(data: Weather): PagerAdapter {
        return ForecastAdapter(childFragmentManager, data, this)
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)

        tabToOpen = args?.getInt("day") ?: 0
    }

    override fun onLoadFinished(loader: Loader<Any?>, data: Any?) {
        val bundle = Bundle()
        bundle.putInt("type", CurrentWeatherFragment.FORECAST)

        if (loader.id == FORECAST_LOADER) {
            if (data != null) {
                displayForecast(data as Weather)
                weather = data
                loaderManager.initLoader(SAVE_FORECAST, bundle, this)
            } else {
                println("Loading offline weather forecast")
                loaderManager.initLoader(FORECAST_LOADER_OFF, bundle, this)
            }
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

            val window = activity!!.window
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
                    (activity as AppCompatActivity).supportActionBar!!.setBackgroundDrawable(ColorDrawable(color))
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

    fun loadFragment() {
        val bundle = Bundle()
        bundle.putString("city", "Exeter")
        bundle.putString("country", "UK")
        loaderManager.initLoader(FORECAST_LOADER, bundle, this)
    }

    override fun onViewPagerColorChange(color: ColorDrawable) {
        mViewPager.background = color
    }

    override fun onScrollColorChange(color: ColorDrawable, position: Int) {
        colors[position] = color.color
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation {

        val anim = AnimationUtils.loadAnimation(activity, nextAnim)

        anim.setAnimationListener(object : AnimationListener {

            override fun onAnimationStart(animation: Animation) {
                Log.d(TAG, "Animation started.")
                // additional functionality
            }

            override fun onAnimationRepeat(animation: Animation) {
                Log.d(TAG, "Animation repeating.")
                // additional functionality
            }

            override fun onAnimationEnd(animation: Animation) {
                Log.d(TAG, "Animation ended.")
                loadFragment()
            }
        })

        return anim
    }
}