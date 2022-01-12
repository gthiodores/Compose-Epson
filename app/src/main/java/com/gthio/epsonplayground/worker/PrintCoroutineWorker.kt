package com.gthio.epsonplayground.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.printer.Printer
import com.gthio.epsonplayground.addBody
import com.gthio.epsonplayground.addHeader
import com.gthio.epsonplayground.di.IoDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltWorker
class PrintCoroutineWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        return withContext(ioDispatcher) {
            try {
                val printerSeries = inputData.getInt("printer_series", Printer.TM_T82)
                val connectionTarget = inputData.getString("connection_target")
                val mPrinter = Printer(printerSeries, Printer.LANG_EN, appContext)

                val textData = StringBuilder()
                mPrinter.addHeader(textData, "MY HEADER!")
                mPrinter.addFeedLine(1)
                mPrinter.addBody(textData, "Lorem Ipsum Dolor")
                mPrinter.addFeedLine(1)
                mPrinter.addCut(Printer.CUT_FEED)

                val result = awaitCallback(mPrinter, connectionTarget!!)

                if (result) {
                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("CoroutineWorker", "${e.message}")
                Result.failure()
            }
        }
    }

    private suspend fun awaitCallback(printer: Printer, target: String) =
        suspendCoroutine<Boolean> { continuation ->
            printer.setReceiveEventListener { mPrinter, code, _, _ ->
                if (code == Epos2CallbackCode.CODE_SUCCESS) {
                    mPrinter.disconnect()
                    continuation.resume(true)
                } else {
                    mPrinter.disconnect()
                    continuation.resume(false)
                }
            }
            printer.connect(target, Printer.PARAM_DEFAULT)
            printer.sendData(Printer.PARAM_DEFAULT)
            printer.clearCommandBuffer()
        }

}