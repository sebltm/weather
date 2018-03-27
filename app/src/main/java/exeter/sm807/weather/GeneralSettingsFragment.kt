package exeter.sm807.weather

import android.content.Context
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment

/**
 * Created by 660046669 on 19/03/2018.
 */

class GeneralSettingsFragment : PreferenceFragment() {
    /**
     * Loads the general settings and update the shared preferences when the user changes
     * a preference
     */

    companion object {
        const val TWELVE_HOURS = 0
        const val FIVE_HOURS = 1
        const val THREE_HOURS = 2
        const val ONE_HOUR = 3
        const val THIRTY_MINUTES = 4
        const val TEN_MINUTES = 5
        const val FIVE_MINUTES = 6
        const val ONE_MINUTE = 7
        const val ALWAYS = 8
    }

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

        val listPreference = findPreference("stale_data") as ListPreference
        listPreference.summary =
                listPreference.entries[activity.getSharedPreferences("data", Context.MODE_PRIVATE).getInt("stale_data", 3)]
        findPreference("stale_data")?.setOnPreferenceChangeListener { preference, newValue ->
            val index = listPreference.findIndexOfValue(newValue as String)

            println("New setting $newValue at index $index")

            // Set the summary to reflect the new value.
            preference.summary = if (index >= 0)
                listPreference.entries[index]
            else
                null

            activity.getSharedPreferences("data", Context.MODE_PRIVATE)
                    .edit()
                    .putInt(preference.key, index)
                    .apply()

            true
        }
    }
}
