package exeter.sm807.weather

import android.net.Uri
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * Created by sebltm on 02/03/2018.
 */

internal class HttpConnection {
    var url: URL? = null
        private set

    constructor(mode: String, city: String) {
        try {
            val builder: Uri.Builder = Uri.parse(APIURL).buildUpon().appendPath(mode)
                    .appendQueryParameter("q", city)
                    .appendQueryParameter("APPID", APIKEY)

            url = URL(builder.build().toString())
        } catch (e: MalformedURLException) {
            //TODO (1) Handle error
        }

    }

    constructor(mode: String, city: String, country: String) {
        try {
            val builder: Uri.Builder = Uri.parse(APIURL).buildUpon().appendPath(mode)
                    .appendQueryParameter("q", "$city,$country")
                    .appendQueryParameter("APPID", APIKEY)

            url = URL(builder.build().toString())
        } catch (e: MalformedURLException) {
            //TODO (2) Handle error
        }

    }

    constructor(mode: String, city: String, country: String?, units: String?) {
        try {
            val builder: Uri.Builder = if (country != null) {
                Uri.parse(APIURL).buildUpon().appendPath(mode)
                        .appendQueryParameter("q", "$city,$country")
                        .appendQueryParameter("APPID", APIKEY)
            } else {
                Uri.parse(APIURL).buildUpon().appendQueryParameter("q", city)
                        .appendQueryParameter("APPID", APIKEY)
            }

            if (units != null) {
                builder.appendQueryParameter(UNITS, units)
            }

            url = URL(builder.build().toString())
        } catch (e: MalformedURLException) {
            //TODO (3) Handle error
        }

    }

    fun openConnection(url: URL): String? {
        try {
            val urlConnection = url.openConnection() as HttpURLConnection
            val input = urlConnection.inputStream

            val scanner = Scanner(input)
            scanner.useDelimiter("\\A")

            var hasInput = scanner.hasNext()

            val response = StringBuilder()
            while (hasInput) {
                response.append(scanner.next())
                hasInput = scanner.hasNext()
            }

            urlConnection.disconnect()
            return response.toString()
        } catch (e: IOException) {

        }

        return null
    }

    companion object {
        private const val APIURL = "http://api.openweathermap.org/data/2.5"
        private const val APIKEY = "cd456cbf874b33c0f32b1dfd308c785e"
        private const val UNITS = "units"
    }
}
