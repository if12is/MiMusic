package it.vfsfitvnm.vimusic.ui.components.themed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class GlassNavigationHost {
    private val registrations = mutableListOf<Int>()
    private var nextId = 0

    var isActive by mutableStateOf(false)
        private set
    var leadingIconId by mutableStateOf(0)
        private set
    var leadingIconDescription by mutableStateOf("")
        private set
    var onLeadingIconClick by mutableStateOf({})
        private set
    var tabIndex by mutableStateOf(0)
        private set
    var onTabIndexChanged by mutableStateOf<(Int) -> Unit>({})
        private set
    var tabs by mutableStateOf<@Composable (@Composable (Int, String, Int) -> Unit) -> Unit>({})
        private set

    fun acquire(): Int {
        val id = ++nextId
        registrations.add(id)
        isActive = true
        return id
    }

    fun release(id: Int) {
        registrations.remove(id)
        isActive = registrations.isNotEmpty()
    }

    fun bind(
        id: Int,
        leadingIconId: Int,
        leadingIconDescription: String,
        onLeadingIconClick: () -> Unit,
        tabIndex: Int,
        onTabIndexChanged: (Int) -> Unit,
        tabs: @Composable (@Composable (Int, String, Int) -> Unit) -> Unit
    ) {
        if (registrations.lastOrNull() != id) return

        this.leadingIconId = leadingIconId
        this.leadingIconDescription = leadingIconDescription
        this.onLeadingIconClick = onLeadingIconClick
        this.tabIndex = tabIndex
        this.onTabIndexChanged = onTabIndexChanged
        this.tabs = tabs
    }
}

val LocalGlassNavigationHost = staticCompositionLocalOf { GlassNavigationHost() }

@Composable
fun GlassNavigationHost.NavigationBar(docked: Boolean) {
    if (!isActive) return

    GlassyNavigationBar(
        leadingIconId = leadingIconId,
        onLeadingIconClick = onLeadingIconClick,
        leadingIconDescription = leadingIconDescription,
        tabIndex = tabIndex,
        onTabIndexChanged = onTabIndexChanged,
        docked = docked,
        content = { item ->
            tabs(item)
        }
    )
}
