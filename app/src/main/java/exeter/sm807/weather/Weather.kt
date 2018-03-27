package exeter.sm807.weather

import java.io.Serializable
import java.util.*
import kotlin.math.roundToInt

/**
 * Created by 660046669 on 18/03/2018.
 */

class Weather : Serializable {
    var time: Long = 0
    val days: ArrayList<Day> = ArrayList()
    lateinit var city: City

    fun emptyWeather(): Weather {
        val data = Weather()
        data.city = data.City(0L, "--", "--")
        data.days.add(data.Day())
        data.days.last().list.add(data.days.last().List(
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0
        ))


        data.days.last().list.last().dt = System.currentTimeMillis()

        data.days.last().list.last().weather =
                data.days.last().list.last().Weather(
                        0,
                        "No data",
                        "No data",
                        "No data"
                )

        data.days.last().list.last().weather.wind =
                data.days.last().list.last().weather.Wind(0.0, 0.0)

        return data
    }

    inner class City(var id: Long?, var name: String?, var country: String?) : Serializable {
        lateinit var coord: Coord

        inner class Coord(var long: Double?, var lat: Double?) : Serializable
    }

    inner class Day : Serializable {
        val list: ArrayList<List> = ArrayList()

        inner class List(var temp: Double?,
                         var pressure: Double?,
                         var humidity: Double?,
                         var temp_min: Double?,
                         var temp_max: Double?,
                         var sea_level: Double?,
                         var grnd_level: Double?,
                         var clouds: Double?,
                         var rain: Double?,
                         var snow: Double?,
                         var pos: Int) : Serializable {

            lateinit var weather: Weather
            lateinit var sys: Sys
            var dt: Long? = null

            fun getTemp(): Int? {
                return temp?.roundToInt()
            }

            fun getPressure(): Int? {
                return pressure?.roundToInt()
            }

            fun getHumidity(): Int? {
                return humidity?.roundToInt()
            }

            fun getTempMin(): Int? {
                return temp_min?.roundToInt()
            }

            fun getTempMax(): Int? {
                return temp_max?.roundToInt()
            }

            fun getSeaLevel(): Int? {
                return sea_level?.roundToInt()
            }

            fun getGrndLevel(): Int? {
                return grnd_level?.roundToInt()
            }

            inner class Weather(var id: Int?,
                                var main: String?,
                                var description: String?,
                                var icon: String?) : Serializable {

                lateinit var wind: Wind

                fun backgroundColor(): String? {
                    return if (id in 200..299 && icon?.contains("d") == true) {
                        "#2F4F4F"
                    } else if (icon?.contains("n") == true) {
                        "#000049"
                    } else if (id in 200..299 && icon?.contains("n") == true) {
                        "#001C1C"
                    } else if (id in 300..499 && icon?.contains("d") == true) {
                        "#D3D3D3"
                    } else if (id in 300..499 && icon?.contains("n") == true) {
                        "#A0A0A0"
                    } else if (id == 500 && icon?.contains("d") == true) {
                        "#A9A9A9"
                    } else if (id == 500 && icon?.contains("n") == true) {
                        "#767676"
                    } else if (id in 501..511 && icon?.contains("d") == true) {
                        "#708090"
                    } else if (id in 501..511 && icon?.contains("n") == true) {
                        "#3D4D5D"
                    } else if (id in 520..599 && icon?.contains("d") == true) {
                        "#808080"
                    } else if (id in 520..599 && icon?.contains("n") == true) {
                        "#4D4D4D"
                    } else if ((id == 602 || id == 622) && icon?.contains("d") == true) {
                        "#808080"
                    } else if ((id == 602 || id == 622) && icon?.contains("n") == true) {
                        "#4D4D4D"
                    } else if (id in 600..699 && icon?.contains("d") == true) {
                        "#A9A9A9"
                    } else if (id in 600..699 && icon?.contains("n") == true) {
                        "#767676"
                    } else if (id in 700..799 && icon?.contains("d") == true) {
                        "#C0C0C0"
                    } else if (id in 700..799 && icon?.contains("n") == true) {
                        "#8D8D8D"
                    } else if (id == 800 && icon?.contains("d") == true) {
                        "#FFD700"
                    } else if (id == 800 && icon?.contains("n") == true) {
                        "#CCA400"
                    } else if (id in 801..899 && icon?.contains("d") == true) {
                        "#FFE977"
                    } else if (id in 801..899 && icon?.contains("n") == true) {
                        "#CCB644"
                    } else if (icon?.contains("n") == true) {
                        "#767676"
                    } else {
                        "#A9A9A9"
                    }
                }

                fun updateWeatherIcon(): Int? {
                    return when {
                        icon?.contains("01d") == true -> R.drawable.sunny
                        icon?.contains("02d") == true -> R.drawable.overcast
                        icon?.contains("03d") == true -> R.drawable.cloud
                        icon?.contains("04d") == true -> R.drawable.clouds
                        icon?.contains("09d") == true -> R.drawable.heavy_rain
                        icon?.contains("10d") == true -> R.drawable.shower_rain
                        icon?.contains("11d") == true -> R.drawable.thunderstorm
                        icon?.contains("13d") == true -> R.drawable.light_snow
                        icon?.contains("50d") == true -> R.drawable.misty
                        icon?.contains("01n") == true -> R.drawable.moon
                        icon?.contains("02n") == true -> R.drawable.moon_overcast
                        icon?.contains("03n") == true -> R.drawable.cloud
                        icon?.contains("04n") == true -> R.drawable.clouds
                        icon?.contains("09n") == true -> R.drawable.moon_drizzle
                        icon?.contains("10n") == true -> R.drawable.moon_drizzle
                        icon?.contains("11n") == true -> R.drawable.moon_thunderstorm
                        icon?.contains("13n") == true -> R.drawable.moon_heavy_snow
                        icon?.contains("50n") == true -> R.drawable.moon_misty
                        else -> R.drawable.cloud
                    }
                }

                fun getBuiltDescription(): String? {
                    val descrArr = description?.split(" ")
                    val builder = StringBuilder()
                    return if (descrArr != null) {
                        for (word in descrArr) {
                            builder.append("${word.capitalize()} ")
                        }

                        builder.toString()
                    } else null
                }

                inner class Wind(var speed: Double?,
                                 var deg: Double?) : Serializable {

                    fun getSpeed(): Int {
                        /**
                         * Speed from m/s to km/h
                         */
                        return (speed ?: 0 * 3.6).roundToInt()
                    }

                    fun getDeg(): String? {
                        return windDirection(deg)
                    }

                    private fun windDirection(deg: Double?): String? {
                        /**
                         * Convert degrees to String bearing
                         */
                        if (deg == null) {
                            return null
                        }

                        val windDirection = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE",
                                "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW", "N")
                        val compass = deg % 360
                        val index = Math.round(compass / 22.5).toInt()
                        return windDirection[index]
                    }
                }
            }

            inner class Sys(var sunrise: Long?, var sunset: Long?) : Serializable
        }

    }
}
