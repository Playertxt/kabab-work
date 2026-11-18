package com.kabab.work

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kabab.work.ui.WorkOrganizerApp
import com.kabab.work.ui.WorkViewModel
import com.kabab.work.ui.theme.WorkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkTheme {
                val viewModel: WorkViewModel = viewModel(factory = WorkViewModel.Factory)
                WorkOrganizerApp(viewModel)
            }
        }
    }
}
