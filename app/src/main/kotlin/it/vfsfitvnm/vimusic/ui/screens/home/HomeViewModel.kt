package it.vfsfitvnm.vimusic.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.vfsfitvnm.vimusic.utils.PlannedMix
import it.vfsfitvnm.vimusic.utils.Region
import it.vfsfitvnm.vimusic.utils.planDailyMixes
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val region: StateFlow<String?> = Region.flow

    private val history = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    val dailyMixes: StateFlow<List<PlannedMix>> = history
        .map { songs ->
            planDailyMixes(songs, TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis()))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        Region.load(application)
        viewModelScope.launch {
            Region.refresh(application)
        }
    }

    fun onPlaybackHistory(songs: List<Pair<String, String>>) {
        history.value = songs
    }
}
