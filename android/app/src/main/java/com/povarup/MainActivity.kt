package com.povarup

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.povarup.core.RoleStateHolder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = RoleStateHolder().current()
        val label = TextView(this).apply {
            text = "PovarUp (${state.role})"
            textSize = 20f
        }
        setContentView(label)
    }
}
