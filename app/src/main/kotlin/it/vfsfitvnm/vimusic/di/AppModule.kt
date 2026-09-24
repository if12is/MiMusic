package it.vfsfitvnm.vimusic.di

import it.vfsfitvnm.vimusic.ui.screens.home.HomeViewModel
import it.vfsfitvnm.vimusic.ui.screens.player.PlayerSessionModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { HomeViewModel(androidApplication()) }
    viewModel { PlayerSessionModel() }
}
