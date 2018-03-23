package exeter.sm807.weather

import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View


class MainActivity : AppCompatActivity(), OnForecastSelected {
    private var mDrawer: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mDrawer = findViewById(R.id.drawer_layout)
        mDrawerToggle = ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close)
        mDrawer!!.addDrawerListener(mDrawerToggle!!)
        supportActionBar!!.setHomeButtonEnabled(true)
        mDrawerToggle!!.syncState()

        navigationView = findViewById(R.id.nav_view)
        navigationView!!.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            mDrawer!!.closeDrawers()

            val fManager = supportFragmentManager
            val ft = fManager.beginTransaction()
            val fragment: Fragment?
            when (menuItem.itemId) {
                R.id.forecast -> {
                    ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top)
                    fragment = fManager.findFragmentByTag("forecast")

                    if (fragment == null) {
                        val forecastFragment = ForecastFragment()
                        ft.replace(R.id.fragment_parent, forecastFragment, "forecast")
                                .commit()
                    } else {
                        ft.replace(R.id.fragment_parent, fragment, "forecast")
                                .commit()
                    }
                }
                R.id.current -> {
                    ft.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom)
                    fragment = fManager.findFragmentByTag("current")

                    if (fragment == null) {
                        val currentFragment = CurrentWeatherFragment()
                        currentFragment.setListener(this@MainActivity)
                        ft.replace(R.id.fragment_parent, currentFragment, "current")
                                .commit()
                    } else {
                        ft.replace(R.id.fragment_parent, fragment, "current")
                                .commit()
                    }
                }
            }

            true
        }

        mDrawer!!.addDrawerListener(
                object : DrawerLayout.DrawerListener {
                    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                        // Respond when the drawer's position changes
                    }

                    override fun onDrawerOpened(drawerView: View) {
                        // Respond when the drawer is opened
                    }

                    override fun onDrawerClosed(drawerView: View) {
                        // Respond when the drawer is closed
                    }

                    override fun onDrawerStateChanged(newState: Int) {
                        // Respond when the drawer motion state changes
                    }
                }
        )

        navigationView!!.menu.findItem(R.id.current).isChecked = true

        val fManager = supportFragmentManager
        val ft = fManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_top)
        val fragment = fManager.findFragmentByTag("current")
        if (fragment == null) {
            val currentFragment = CurrentWeatherFragment()
            currentFragment.setListener(this@MainActivity)
            ft.replace(R.id.fragment_parent, currentFragment, "current")
                    .commit()
        } else {
            ft.replace(R.id.fragment_parent, fragment, "current")
                    .commit()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle!!.onOptionsItemSelected(item)) {
            return true
        }

        when (item.itemId) {
            android.R.id.home -> {
                mDrawer!!.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle!!.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onForecastSelected() {
        navigationView!!.menu.findItem(R.id.forecast).isChecked = true
    }
}