package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.repository.InvoiceRepository
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InvoiceReviewScreen
import com.example.ui.theme.SaudiInvoiceTheme
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.ui.viewmodel.InvoiceViewModelFactory

enum class AppScreen {
    HOME,
    REVIEW,
    EXPORT,
    ABOUT
}

class MainActivity : ComponentActivity() {

    private val viewModel: InvoiceViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = InvoiceRepository(database.invoiceDao())
        InvoiceViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SaudiInvoiceTheme {
                SaudiInvoiceApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SaudiInvoiceApp(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToExport = { currentScreen = AppScreen.EXPORT },
                onNavigateToAbout = { currentScreen = AppScreen.ABOUT },
                onInvoiceClick = { currentScreen = AppScreen.REVIEW },
                modifier = modifier.fillMaxSize()
            )
        }
        AppScreen.REVIEW -> {
            InvoiceReviewScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.HOME },
                modifier = modifier.fillMaxSize()
            )
        }
        AppScreen.EXPORT -> {
            ExportScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.HOME },
                modifier = modifier.fillMaxSize()
            )
        }
        AppScreen.ABOUT -> {
            AboutScreen(
                onBack = { currentScreen = AppScreen.HOME },
                modifier = modifier.fillMaxSize()
            )
        }
    }
}
