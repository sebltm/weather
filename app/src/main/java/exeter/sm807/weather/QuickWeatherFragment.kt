package exeter.sm807.weather

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceFragment

/**
 * Created by 660046669 on 26/03/2018.
 */
class QuickWeatherFragment : PreferenceFragment() {

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.pref_quick_weather)
        findPreference("qw_country")?.summary =
                activity.getSharedPreferences("location", Context.MODE_PRIVATE).getString("qw_country", "France")
        findPreference("qw_city")?.summary =
                activity.getSharedPreferences("location", Context.MODE_PRIVATE).getString("qw_city", "Paris")
        findPreference("qw_country")?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue as String
            activity.getSharedPreferences("location", Context.MODE_PRIVATE)
                    .edit()
                    .putString(preference.key, newValue)
                    .apply()
            true
        }
        findPreference("qw_city")?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue as String
            activity.getSharedPreferences("location", Context.MODE_PRIVATE)
                    .edit()
                    .putString("qw_city", newValue)
                    .apply()
            true
        }
    }
}