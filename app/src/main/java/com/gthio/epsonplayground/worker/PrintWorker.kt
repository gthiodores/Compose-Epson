package com.gthio.epsonplayground.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.printer.Printer
import com.gthio.epsonplayground.PrinterWrapper
import com.gthio.epsonplayground.addBody
import com.gthio.epsonplayground.addHeader
import com.gthio.epsonplayground.di.IoDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.CountDownLatch

@HiltWorker
class PrintWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val printerWrapper: PrinterWrapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        val printerSeries = inputData.getInt("printer_series", Printer.TM_T82)
        val connectionTarget = inputData.getString("connection_target")
        val mPrinter = Printer(printerSeries, Printer.LANG_EN, appContext)

        val textData = StringBuilder()
        mPrinter.addHeader(textData, "MY HEADER!")
        mPrinter.addFeedLine(1)
        mPrinter.addBody(textData, "Lorem Ipsum Dolor")
        mPrinter.addFeedLine(1)
        mPrinter.addCut(Printer.CUT_FEED)

        try {
            var result: Result = Result.success()
            val latch = CountDownLatch(1)
            mPrinter.setReceiveEventListener { printer, code, status, _ ->
                when (status.connection) {
                    Printer.FALSE -> {
                        Log.d("Epos2", "NOT CONNECTED TO PRINTER")
                        result = Result.retry()
                        latch.countDown()
                    }
                }
                when (code) {
                    Epos2CallbackCode.CODE_SUCCESS -> {
                        Log.d("Epos2", "Print Success!")
                        printer.disconnect()
                        printer.setReceiveEventListener(null)
                        result = Result.success()
                        latch.countDown()
                    }
                    else -> {
                        Log.d("Epos2", "Print Failed!")
                        printer.disconnect()
                        printer.setReceiveEventListener(null)
                        result = Result.failure()
                        latch.countDown()
                    }
                }
            }
            mPrinter.connect(connectionTarget, Printer.PARAM_DEFAULT)
            mPrinter.sendData(Printer.PARAM_DEFAULT)
            mPrinter.clearCommandBuffer()
            latch.await()
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("Worker", "Failed: ${e.message}")
            return Result.failure()
        }
    }

}