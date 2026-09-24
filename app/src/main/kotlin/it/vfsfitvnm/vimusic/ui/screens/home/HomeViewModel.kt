package it.vfsfitvnm.vimusic.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.vfsfitvnm.vimusic.utils.Region
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val region: StateFlow<String?> = Region.flow

    init {
        Region.load(application)
        viewModelScope.launch {
            Region.refresh(application)
        }
    }
}
