package exeter.sm807.weather

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.support.v4.content.AsyncTaskLoader

/**
 * Created by 660046669 on 18/03/2018.
 */
class WeatherLoaderOffline(context: Context, private val args: Bundle?) : AsyncTaskLoader<Any>(context) {
    var type: Int? = null

    override fun onStartLoading() {
        super.onStartLoading()

        type = args?.getInt("type")

        forceLoad()
    }

    override fun loadInBackground(): Any? {
        try {
            val offResponse = Weather()
            val db = SQLiteDatabase.openDatabase("${context.filesDir.path}/weather", null, SQLiteDatabase.CREATE_IF_NECESSARY)

            var c = db.rawQuery("SELECT * FROM city WHERE type = $type", null)
            while (c.moveToNext()) {
                offResponse.city = offResponse.City(
                        c.getLong(c.getColumnIndex("id")),
                        c.getString(c.getColumnIndex("name")),
                        c.getString(c.getColumnIndex("country"))
                )
            }
            c.close()

            c = db.rawQuery("SELECT * FROM day WHERE type = $type", null)
            while (c.moveToNext()) {
                offResponse.days.add(offResponse.Day())
                offResponse.time = c.getLong(c.getColumnIndex("time"))
            }
            c.close()

            for (i in 0 until offResponse.days.size) {

                c = db.rawQuery("SELECT * FROM list WHERE parent = $i AND type = $type", null)
                while (c.moveToNext()) {
                    offResponse.days[i].list.add(
                            offResponse.days[i].List(
                                    c.getDouble(c.getColumnIndex("temp")),
                                    c.getDouble(c.getColumnIndex("pressure")),
                                    c.getDouble(c.getColumnIndex("humidity")),
                                    c.getDouble(c.getColumnIndex("temp_min")),
                                    c.getDouble(c.getColumnIndex("temp_max")),
                                    c.getDouble(c.getColumnIndex("sea_level")),
                                    c.getDouble(c.getColumnIndex("grnd_level")),
                                    c.getDouble(c.getColumnIndex("clouds")),
                                    c.getDouble(c.getColumnIndex("rain")),
                                    c.getDouble(c.getColumnIndex("snow"))
                            )
                    )

                    offResponse.days[i].list.last().dt = c.getLong(c.getColumnIndex("dt"))
                }
                c.close()

                for (j in 0 until offResponse.days[i].list.size) {
                    c = db.rawQuery("SELECT * FROM weather WHERE day_parent = $i " +
                            "AND list_parent = $j AND type = $type", null)
                    while (c.moveToNext()) {
                        offResponse.days[i].list[j].weather =
                                offResponse.days[i].list[j].Weather(
                                        c.getInt(c.getColumnIndex("id")),
                                        c.getString(c.getColumnIndex("main")),
                                        c.getString(c.getColumnIndex("description")),
                                        c.getString(c.getColumnIndex("icon"))
                                )
                    }
                    c.close()

                    c = db.rawQuery("SELECT * FROM sys WHERE day_parent = $i " +
                            "AND list_parent = $j AND type = $type", null)
                    while (c.moveToNext()) {
                        offResponse.days[i].list[j].sys =
                                offResponse.days[i].list[j].Sys(
                                        c.getLong(c.getColumnIndex("sunrise")),
                                        c.getLong(c.getColumnIndex("sunset"))
                                )
                    }
                    c.close()

                    c = db.rawQuery("SELECT * FROM wind WHERE day_parent = $i " +
                            "AND list_parent = $j AND weather_parent = $j AND type = $type",
                            null)
                    while (c.moveToNext()) {
                        offResponse.days[i].list[j].weather.wind =
                                offResponse.days[i].list[j].weather.Wind(
                                        c.getDouble(c.getColumnIndex("speed")),
                                        c.getDouble(c.getColumnIndex("deg"))
                                )
                    }
                    c.close()
                }
            }

            db.close()
            return offResponse
        } catch (e: SQLiteException) {
            e.printStackTrace()
            return null
        }
    }
}