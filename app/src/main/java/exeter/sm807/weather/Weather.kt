package exeter.sm807.weather

import java.io.Serializable
import kotlin.math.roundToInt

/**
 * Created by 660046669 on 18/03/2018.
 */


class Weather : Serializable {
    val days: ArrayList<Day> = ArrayList()
    lateinit var city: City

    inner class City(var id: Long, var name: String, var country: String?) : Serializable {
        var coord: Coord? = null

        inner class Coord(var long: Double?, var lat: Double?) : Serializable
    }

    inner class Day : Serializable {
        val list: ArrayList<List> = ArrayList()

        inner class List(var temp: Double,
                         var pressure: Double,
                         var humidity: Double,
                         var temp_min: Double,
                         var temp_max: Double,
                         var sea_level: Double?,
                         var grnd_level: Double?,
                         var clouds: Double?,
                         var rain: Double?,
                         var snow: Double?) : Serializable {

            lateinit var weather: Weather
            var sys: Sys? = null
            var dt: Long = 0

            fun getTemp(): String {
                return "${temp.roundToInt()}°C"
            }

            fun getPressure(): String {
                return "${pressure.roundToInt()}hpa"
            }

            fun getHumidity(): String {
                return "${humidity.roundToInt()}%"
            }

            fun getTempMin(): String {
                return "$temp_min°C"
            }

            fun getTempMax(): String {
                return "$temp_max°C"
            }

            fun getSeaLevel(): String {
                return "${sea_level}hpa"
            }

            fun getGrndLevel(): String {
                return "${grnd_level}hpa"
            }

            fun getClouds(): String {
                return "$clouds%"
            }

            fun getRain(): String {
                return "$rain"
            }

            fun getSnow(): String {
                return "$snow"
            }

            inner class Weather(var id: Int,
                                var main: String,
                                var description: String,
                                var icon: String) : Serializable {

                var wind: Wind? = null

                fun backgroundColor(): String {
                    return if (id in 200..299) {
                        "#2F4F4F"
                    } else if (id in 300..499) {
                        "#D3D3D3"
                    } else if (id == 500) {
                        "#A9A9A9"
                    } else if (id in 501..511) {
                        "#708090"
                    } else if (id in 520..599) {
                        "#808080"
                    } else if (id == 602 || id == 622) {
                        "#808080"
                    } else if (id in 600..699) {
                        "#A9A9A9"
                    } else if (id in 700..799) {
                        "#C0C0C0"
                    } else if (id == 800) {
                        "#FFD700"
                    } else if (id in 801..899) {
                        "#FFE977"
                    } else {
                        "#A9A9A9"
                    }
                }

                fun updateWeatherIcon(): Int {
                    return if (id in 200..299) {
                        R.drawable.thunderstorm
                    } else if (id in 300..499) {
                        R.drawable.drizzle
                    } else if (id == 500) {
                        R.drawable.light_rain
                    } else if (id in 501..504) {
                        R.drawable.heavy_rain
                    } else if (id == 511) {
                        R.drawable.heavy_snow
                    } else if (id in 520..599) {
                        R.drawable.shower_rain
                    } else if (id == 602 || id == 622) {
                        R.drawable.heavy_snow
                    } else if (id in 600..699) {
                        R.drawable.light_snow
                    } else if (id in 700..799) {
                        R.drawable.misty
                    } else if (id == 800) {
                        R.drawable.sunny
                    } else if (id == 801) {
                        R.drawable.overcast
                    } else if (id in 802..899) {
                        R.drawable.clouds
                    } else {
                        R.drawable.cloud
                    }
                }

                fun getBuiltDescription(): String {
                    val descrArr = description.split(" ")
                    val builder = StringBuilder()
                    for (word in descrArr) {
                        builder.append("${word.capitalize()} ")
                    }

                    return builder.toString()
                }

                inner class Wind(var speed: Double,
                                 var deg: Double) : Serializable {

                    private fun getSpeed(): String {
                        return (speed * 3.6).roundToInt().toString() + "km/h"
                    }

                    private fun getDeg(): String {
                        return windDirection(deg)
                    }

                    fun getWind(): String {
                        return "${getDeg()} ${getSpeed()}"
                    }

                    private fun windDirection(deg: Double): String {
                        val windDirection = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE",
                                "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW", "N")
                        val compass = deg % 360
                        val index = Math.round(compass / 22.5).toInt()
                        return windDirection[index]
                    }
                }
            }

            inner class Sys(var sunrise: Long, var sunset: Long) : Serializable
        }

    }
}
