package exeter.sm807.weather

import android.graphics.drawable.ColorDrawable

/**
 * Created by 660046669 on 17/03/2018.
 */
interface OnColorChange {
    fun onViewPagerColorChange(color: ColorDrawable)
    fun onScrollColorChange(color: ColorDrawable, position: Int)
}