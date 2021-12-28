package com.gthio.epsonplayground

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epson.epos2.Epos2Exception
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val printerWrapper: PrinterWrapper
) : ViewModel() {

    private var searchJob: Job? = null

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _printers = MutableStateFlow(hashMapOf<String, String>())
    val printers: StateFlow<HashMap<String, String>> = _printers

    fun onPrinterClicked(deviceName: String, target: String) {
        viewModelScope.launch {
            try {
                printerWrapper.print(deviceName, target, "Lorem ipsum dolor!")
            } catch (e: Epos2Exception) {
                Log.d("Epos2Error", "${e.errorStatus}: ${e.printStackTrace()}")
            }
        }
    }

    fun startPrinterDiscovery() {
        _isSearching.value = true
        if (searchJob == null)
            searchJob = viewModelScope.launch {
                printerWrapper.discoveredPrinters.collect { _printers.value = it }
            }
    }

    fun stopPrinterDiscovery() {
        searchJob?.cancel()
        searchJob = null
        _isSearching.value = false
    }

    override fun onCleared() {
        stopPrinterDiscovery()
        super.onCleared()
    }
}