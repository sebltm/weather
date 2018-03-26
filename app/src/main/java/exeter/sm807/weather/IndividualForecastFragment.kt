package exeter.sm807.weather

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import org.json.JSONException


/**
 * Created by 660046669 on 09/03/2018.
 */

class IndividualForecastFragment : Fragment() {

    private var mRecyclerView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var dayData: Weather.Day? = null
    private lateinit var previousColor: ColorDrawable
    private var listener: OnColorChange? = null
    private var firstItemVisible = 0
    private var position: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.individual_forecast, container, false)

        mRecyclerView = view.findViewById(R.id.hourList)

        mLayoutManager = LinearLayoutManager(context)
        mRecyclerView!!.layoutManager = mLayoutManager
        return view
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        try {
            dayData = args?.getSerializable("dayData") as Weather.Day
            previousColor = ColorDrawable(Color.parseColor(dayData!!.list[0].weather.backgroundColor()))
            position = args.getInt("position")
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val mAdapter = IndividualForecastAdapter(dayData!!, activity)
        mRecyclerView!!.adapter = mAdapter
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                if (dy > 0 || dy < 0) {
                    updateColors()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                enableDisableSwipeRefresh(newState == ViewPager.SCROLL_STATE_IDLE)
            }

            private fun enableDisableSwipeRefresh(enable: Boolean) {
                val mSwipeRefreshLayout = activity?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.isEnabled = enable
                }
            }
        })
    }

    fun setOnColorChangeListener(listener: OnColorChange) {
        this.listener = listener
    }

    private fun darkenColor(factor: Float, color: ColorDrawable): ColorDrawable {
        val a = color.alpha
        val r = Math.round(Color.red(color.color) * factor)
        val g = Math.round(Color.green(color.color) * factor)
        val b = Math.round(Color.blue(color.color) * factor)

        return ColorDrawable(Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255)))
    }

    fun updateColors() {
        activity?.runOnUiThread({
            val colorTo: ColorDrawable
            if (mLayoutManager!!.findFirstCompletelyVisibleItemPosition() != -1) {
                firstItemVisible = mLayoutManager!!.findFirstCompletelyVisibleItemPosition()
            }
            var animator: ValueAnimator? = null

            colorTo = try {
                ColorDrawable(Color.parseColor(dayData!!.list[firstItemVisible].weather.backgroundColor()))
            } catch (e: JSONException) {
                e.printStackTrace()
                ColorDrawable(Color.GRAY)
            }

            val window = activity!!.window
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            animator?.removeAllUpdateListeners()
            animator?.cancel()

            if (previousColor.color != colorTo.color) {
                animator = ValueAnimator.ofArgb(previousColor.color, colorTo.color)
                animator?.addUpdateListener { animation ->
                    val color = ColorDrawable(animation.animatedValue as Int)

                    (activity!! as AppCompatActivity)
                            .supportActionBar
                            ?.setBackgroundDrawable(color)
                    mRecyclerView?.background = color
                    mRecyclerView?.rootView?.background = color
                    listener?.onViewPagerColorChange(color)
                    listener?.onScrollColorChange(color, position!!)

                    val sBarColor = darkenColor(0.8f, color)

                    window.statusBarColor = sBarColor.color
                }
                animator?.duration = 250
                animator?.start()
            }
            previousColor = colorTo
        })
    }

    override fun onResume() {
        super.onResume()
        updateColors()
    }
}

