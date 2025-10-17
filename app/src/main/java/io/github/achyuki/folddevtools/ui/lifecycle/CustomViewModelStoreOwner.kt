package io.github.achyuki.folddevtools.ui.lifecycle

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class CustomViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
