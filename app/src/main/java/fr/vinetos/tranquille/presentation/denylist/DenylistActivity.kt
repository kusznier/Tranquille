package fr.vinetos.tranquille.presentation.denylist

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.vinetos.tranquille.EventUtils
import fr.vinetos.tranquille.event.DenylistChangedEvent
import fr.vinetos.tranquille.event.DenylistItemChangedEvent
import fr.vinetos.tranquille.ui.components.setTranquilleContent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DenylistActivity : AppCompatActivity() {

    private val viewModel: DenylistViewModel by viewModels {
        viewModelFactory { initializer { DenylistViewModel(AppDenylistRepository) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTranquilleContent { DenylistRoute(viewModel, onNavigateUp = ::finish) }
    }

    override fun onStart() {
        super.onStart()
        EventUtils.register(this)
        viewModel.reload()
    }

    override fun onStop() {
        EventUtils.unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onDenylistChanged(event: DenylistChangedEvent) {
        viewModel.reload()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onDenylistItemChanged(event: DenylistItemChangedEvent) {
        viewModel.reload()
    }
}
