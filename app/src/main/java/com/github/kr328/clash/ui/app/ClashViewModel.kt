package com.github.kr328.clash.ui.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import java.util.UUID

open class ClashViewModel(application: Application) : AndroidViewModel(application), Broadcasts.Observer {
    protected val uiStore = UiStore(application)
    protected val app: Application
        get() = getApplication()
    protected val clashRunning: Boolean
        get() = Remote.broadcasts.clashRunning

    init {
        Remote.broadcasts.addObserver(this)
    }

    override fun onCleared() {
        Remote.broadcasts.removeObserver(this)
        super.onCleared()
    }

    override fun onServiceRecreated() = Unit
    override fun onStarted() = Unit
    override fun onStopped(cause: String?) = Unit
    override fun onProfileChanged() = Unit
    override fun onProfileUpdateCompleted(uuid: UUID?) = Unit
    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) = Unit
    override fun onProfileLoaded() = Unit
}
