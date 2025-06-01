package br.com.coin_project_ia_bot.presentation

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import br.com.coin_project_ia_bot.R
import br.com.coin_project_ia_bot.databinding.ActivityMainBinding
import br.com.coin_project_ia_bot.presentation.fragments.dashboard.DashboardFragment
import br.com.coin_project_ia_bot.presentation.fragments.multi_tff.MultiTFFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)

        navController = supportFragmentManager.findFragmentById(R.id.navHostFragment)!!
            .findNavController()

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        // Configura Drawer Toggle no Toolbar
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Listener para troca via menu lateral
        navView.setNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.nav_dashboard -> navController.navigate(R.id.dashboardFragment)
                R.id.nav_multitime -> navController.navigate(R.id.multiTimeframeFragment)
                // outros casos
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
