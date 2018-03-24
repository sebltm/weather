package exeter.sm807.weather

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceFragment

/**
 * Created by sebltm on 19/03/2018.
 */

class GeneralSettingsFragment : PreferenceFragment() {

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.pref_general)

        findPreference("country")?.summary =
                activity.getSharedPreferences("location", Context.MODE_PRIVATE).getString("country", "UK")

        findPreference("city_name")?.summary =
                activity.getSharedPreferences("location", Context.MODE_PRIVATE).getString("city_name", "Exeter")

        findPreference("city_name")?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue as String
            activity.getSharedPreferences("location", Context.MODE_PRIVATE)
                    .edit()
                    .putString(preference.key, newValue)
                    .apply()
            true
        }

        findPreference("country")?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue as String
            activity.getSharedPreferences("location", Context.MODE_PRIVATE)
                    .edit()
                    .putString(preference.key, newValue)
                    .apply()
            true
        }
    }
}
