package com.example.voicereaderapp.ui.index

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Index screen.
 * Manages navigation state between different tabs.
 */
@HiltViewModel
class IndexViewModel @Inject constructor() : ViewModel() {
    private val _selectedTab = MutableStateFlow(0)
    
    /**
     * Current selected tab index.
     * 0 = PDF Reader, 1 = Scanner, 2 = Live Reader, 3 = Settings
     */
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    /**
     * Selects a tab by index.
     *
     * @param index Tab index to select
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
}
