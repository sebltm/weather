package exeter.sm807.weather

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.support.v4.content.AsyncTaskLoader
import java.sql.SQLException

/**
 * Created by 660046669 on 18/03/2018.
 */
class SaveWeather(context: Context, private val bundle: Bundle?, private val weather: Weather) : AsyncTaskLoader<Any>(context) {
    /**
     * Save the data to the database by iterating through the weather array
     * Differentiate between forecast and current data by using the "type"
     */

    private var type: Int = 0

    override fun onStartLoading() {
        super.onStartLoading()

        type = bundle!!.getInt("type")
        forceLoad()
    }

    override fun loadInBackground(): Any? {

        val db = SQLiteDatabase.openDatabase("${context.getDatabasePath("weather")}", null, SQLiteDatabase.CREATE_IF_NECESSARY)

        db.beginTransaction()

        return try {
            db.execSQL("CREATE TABLE IF NOT EXISTS day(" +
                    "id INTEGER, " +
                    "time INTEGER, " +
                    "type INTEGER, " +
                    "PRIMARY KEY(id, type));")

            db.execSQL("CREATE TABLE IF NOT EXISTS city(" +
                    "type INTEGER PRIMARY KEY, " +
                    "id INTEGER," +
                    "name TEXT, " +
                    "country TEXT);")

            db.execSQL("CREATE TABLE IF NOT EXISTS list(" +
                    "id INTEGER, " +
                    "type INTEGER, " +
                    "parent INTEGER, " +
                    "dt INTEGER, " +
                    "temp REAL, " +
                    "pressure REAL, " +
                    "humidity REAL, " +
                    "temp_min REAL, " +
                    "temp_max REAL, " +
                    "sea_level REAL, " +
                    "grnd_level REAL, " +
                    "clouds REAL, " +
                    "rain REAL, " +
                    "snow REAL, " +
                    "FOREIGN KEY (parent) REFERENCES day(id), " +
                    "PRIMARY KEY(id, type, parent));")

            db.execSQL("CREATE TABLE IF NOT EXISTS weather(" +
                    "db_id INTEGER, " +
                    "type INTEGER, " +
                    "day_parent INTEGER," +
                    "list_parent INTEGER, " +
                    "id INTEGER, " +
                    "main TEXT, " +
                    "description TEXT, " +
                    "icon TEXT, " +
                    "FOREIGN KEY(day_parent, list_parent) REFERENCES list(parent, id), " +
                    "PRIMARY KEY(db_id, type, day_parent, list_parent));")

            db.execSQL("CREATE TABLE IF NOT EXISTS sys(" +
                    "id INTEGER, " +
                    "type INTEGER, " +
                    "day_parent INTEGER, " +
                    "list_parent INTEGER, " +
                    "sunrise INTEGER, " +
                    "sunset INTEGER, " +
                    "FOREIGN KEY(day_parent, list_parent) REFERENCES list(parent, id), " +
                    "PRIMARY KEY(id, type, day_parent, list_parent));")

            db.execSQL("CREATE TABLE IF NOT EXISTS wind(" +
                    "id INTEGER, " +
                    "type INTEGER, " +
                    "day_parent INTEGER, " +
                    "list_parent INTEGER, " +
                    "weather_parent INTEGER, " +
                    "speed REAL," +
                    "deg REAL, " +
                    "FOREIGN KEY(day_parent, list_parent, weather_parent) REFERENCES " +
                    "weather(day_parent, list_parent, db_id)," +
                    "PRIMARY KEY(id, type, day_parent, list_parent));")

            db.setTransactionSuccessful()

            db.execSQL("INSERT OR REPLACE INTO city(type, id, name, country) VALUES(" +
                    "$type, ${weather.city.id}, '${weather.city.name}', '${weather.city.country}');")

            for (i in 0 until weather.days.size) {

                db.execSQL("INSERT OR REPLACE INTO day(id, time, type) VALUES($i, ${weather.time}, $type);")

                for (j in 0 until weather.days[i].list.size) {
                    val list = weather.days[i].list[j]

                    db.execSQL("INSERT OR REPLACE INTO list(id, type, parent, dt, temp, pressure, " +
                            "humidity, temp_min, temp_max, sea_level, grnd_level, clouds, rain, snow)" +
                            " VALUES($j, $type, $i, ${list.dt}, ${list.temp}, ${list.pressure}, " +
                            "${list.humidity}, ${list.temp_min}, ${list.temp_max}," +
                            "${list.sea_level}, ${list.grnd_level}, ${list.clouds}, ${list.rain}," +
                            "${list.snow});")

                    val weather = list.weather
                    db.execSQL("INSERT OR REPLACE INTO weather(db_id, type, day_parent, list_parent, " +
                            "id, main, description, icon) VALUES($j, $type, $i, $j, ${weather.id}," +
                            "'${weather.main}', '${weather.description}', '${weather.icon}');")


                    val sys = list.sys
                    db.execSQL("INSERT OR REPLACE INTO sys(id, type, day_parent, list_parent, " +
                            "sunrise, sunset) VALUES($j, $type, $i, $j, ${sys.sunrise}, ${sys.sunset});")

                    val wind = weather.wind
                    db.execSQL("INSERT OR REPLACE INTO wind(id, type, day_parent, list_parent, " +
                            "weather_parent, speed, deg) VALUES($j, $type, $i, $j, $j, ${wind.speed}," +
                            "${wind.deg});")
                }
            }

            true
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
            db.close()
        }
    }
}