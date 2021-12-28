package com.gthio.epsonplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gthio.epsonplayground.ui.theme.EpsonPlaygroundTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EpsonPlaygroundTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun MyApp() {
    Scaffold(topBar = { TopAppBar(title = { Text(text = "Epson Playground") }) }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            MyAppContent(viewModel = hiltViewModel())
        }
    }
}

@Composable
fun MyAppContent(viewModel: MainViewModel) {
    val discoveredPrinters by viewModel.printers.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = viewModel::startPrinterDiscovery, enabled = !isSearching) {
                Text(text = "Search")
            }
            Button(onClick = viewModel::stopPrinterDiscovery, enabled = isSearching) {
                Text(text = "Stop")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = discoveredPrinters.toList()) { printer ->
                Column(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                        .background(color = Color.LightGray, shape = RoundedCornerShape(30))
                        .padding(8.dp)
                        .clickable { viewModel.onPrinterClicked(printer.first, printer.second) },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "Printer name: ${printer.first}", fontWeight = FontWeight.Bold)
                    Text(text = "Target: ${printer.second}", fontSize = 12.sp)
                }
            }
        }
    }
}