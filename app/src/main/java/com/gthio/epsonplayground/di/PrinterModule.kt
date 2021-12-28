package com.gthio.epsonplayground.di

import android.content.Context
import com.gthio.epsonplayground.PrinterWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(ViewModelComponent::class)
object PrinterModule {

    @ViewModelScoped
    @Provides
    fun providesPrinterWrapper(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PrinterWrapper = PrinterWrapper(context, ioDispatcher)
}