package exeter.sm807.weather

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup
import org.json.JSONException
import java.util.*


/**
 * Created by 660046669 on 10/03/2018.
 */

class ForecastAdapter internal constructor(fm: FragmentManager, private val data: Weather, private val forecastActivity: ForecastActivity) : FragmentPagerAdapter(fm) {

    private var mFragmentTags: HashMap<Int, String>? = null
    private var mFragmentManager: FragmentManager? = null

    init {
        mFragmentManager = fm
        mFragmentTags = HashMap()
    }

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putSerializable("dayData", data.days[position])
        bundle.putInt("position", position)

        val forecastFragment = IndividualForecastFragment()

        forecastFragment.arguments = bundle
        forecastFragment.setOnColorChangeListener(forecastActivity)

        return forecastFragment
    }

    override fun getCount(): Int {
        return data.days.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        /**
         * Get the day to be displayed on top of each tab
         */
        try {
            val day = data.days[position]
            return CurrentWeatherActivity.getDayName(day.list[0], Calendar.SHORT)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return null
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        /**
         * Only create new Fragment if it does not already exist, otherwise
         * use existing
         */
        val newObject = super.instantiateItem(container, position)
        if (newObject is Fragment) {
            val tag = newObject.tag!!

            val bundle = Bundle()
            bundle.putSerializable("dayData", data.days[position])
            bundle.putInt("position", position)

            mFragmentTags?.put(position, tag)
        }
        return newObject
    }

}
