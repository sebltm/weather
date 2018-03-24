package exeter.sm807.weather

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.text.format.DateUtils
import android.view.ViewGroup
import org.json.JSONException
import java.util.*
import kotlin.collections.HashMap


/**
 * Created by sebltm on 10/03/2018.
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
        try {
            val day = data.days[position]
            val mills = day.list[0].dt * 1000L
            val dateData = Date(mills)

            val c1 = Calendar.getInstance()
            c1.add(Calendar.DAY_OF_YEAR, +1)

            val c2 = Calendar.getInstance()
            c2.time = dateData

            return if (DateUtils.isToday(mills)) {
                "Today"
            } else if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)) {
                "TMRW"
            } else {
                c2.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.UK).toString()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return null
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
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
