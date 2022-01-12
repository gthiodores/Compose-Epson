package com.gthio.epsonplayground

import android.content.Context
import android.util.Log
import androidx.work.*
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epos2.printer.Printer
import com.gthio.epsonplayground.di.IoDispatcher
import com.gthio.epsonplayground.worker.PrintCoroutineWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PrinterWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val seriesDictionary = mapOf(
        "TM-M10" to Printer.TM_M10,
        "TM-M30" to Printer.TM_M30,
        "TM-P20" to Printer.TM_P20,
        "TM-P60" to Printer.TM_P60,
        "TM-P60II" to Printer.TM_P60II,
        "TM-P80" to Printer.TM_P80,
        "TM-T20" to Printer.TM_T20,
        "TM-T60" to Printer.TM_T60,
        "TM-T70" to Printer.TM_T70,
        "TM-T81" to Printer.TM_T81,
        "TM-T82" to Printer.TM_T82,
        "TM-T83" to Printer.TM_T83,
        "TM-T88" to Printer.TM_T88,
        "TM-T90" to Printer.TM_T90,
        "TM-T90KP" to Printer.TM_T90KP,
        "TM-U220" to Printer.TM_U220,
        "TM-U330" to Printer.TM_U330,
        "TM-L90" to Printer.TM_L90,
        "TM-H6000" to Printer.TM_H6000,
        "TM-T83III" to Printer.TM_T83III,
        "TM-T100" to Printer.TM_T100,
        "TM-M30II" to Printer.TM_M30II,
        "TS-100" to Printer.TS_100,
        "TM-M50" to Printer.TM_M50,
        "TM-T88VII" to Printer.TM_T88VII,
        "TM-L90LFC" to Printer.TM_L90LFC,
        "EU-M30" to Printer.EU_M30
    )

    val discoveredPrinters = callbackFlow {
        val printers = hashMapOf<String, String>()

        val printerFoundCallback = DiscoveryListener { deviceInfo ->
            val printerName = deviceInfo.deviceName
            val target = deviceInfo.target
            printers[printerName] = target
            trySend(printers)
        }

        Discovery.start(
            context,
            FilterOption(),
            printerFoundCallback
        ).also {
            Log.d("Epos2", "Search Started")
        }

        awaitClose {
            Discovery.stop()
            Log.d("Epos2", "Search Stopped")
        }
    }.flowOn(ioDispatcher)

    /**
     * Prints a text using the specified printer
     * @param deviceName the device name string (e.g TM-T82)
     * @param connectionTarget the connection target (DeviceInfo.target)
     */
    suspend fun print(
        deviceName: String,
        connectionTarget: String,
    ) {
        withContext(ioDispatcher) {
            Log.d("Printer", "Connection Target: $connectionTarget")
            val inputData = Data.Builder()
                .putInt("printer_series", seriesDictionary[deviceName] ?: Printer.TM_T82)
                .putString("connection_target", connectionTarget)
                .build()
            val workConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<PrintCoroutineWorker>()
                .setInputData(inputData)
                .setConstraints(workConstraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10000,
                    TimeUnit.MILLISECONDS
                )
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

fun Printer.addHeader(builder: StringBuilder, header: String) {
    builder.append(header)

    addTextAlign(Printer.ALIGN_CENTER)
    addText(builder.toString())
    builder.clear()
}

fun Printer.addBody(builder: StringBuilder, body: String) {
    builder.append(body)
    builder.append("\n")
    builder.append(body)
    builder.append("\n")
    builder.append(body)
    builder.append("\n")
    builder.append(body)

    addTextAlign(Printer.ALIGN_LEFT)
    addText(builder.toString())
    builder.clear()
}