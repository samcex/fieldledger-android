package com.indie.shiftledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.indie.shiftledger.ui.theme.FieldLedgerTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<FieldLedgerViewModel> {
        FieldLedgerViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FieldLedgerTheme {
                FieldLedgerApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBilling()
    }
}
