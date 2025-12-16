package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import pt.ipt.dam2025.pawbuddy.databinding.ActivityMainBinding
import pt.ipt.dam2025.pawbuddy.ui.activity.activity.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Colocar o HomeFragment no arranque
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, HomeFragment())
            .commit()
    }
}
