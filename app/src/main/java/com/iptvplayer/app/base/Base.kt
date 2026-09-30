package com.iptvplayer.app.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.iptvplayer.app.R
import com.iptvplayer.app.util.wireSwitchRows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Collects [flow] while the owner is at least STARTED. */
fun <T> LifecycleOwner.collect(flow: Flow<T>, block: suspend (T) -> Unit) {
    lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { block(it) } } }
}

abstract class BaseActivity<VB : ViewBinding>(private val inflate: (LayoutInflater) -> VB) : AppCompatActivity() {
    protected lateinit var binding: VB
    /** Pads the root for system bars (edge-to-edge). Player overrides this. */
    protected open val applyInsets = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = inflate(layoutInflater)
        setContentView(binding.root)
        if (applyInsets) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = maxOf(bars.bottom, ime.bottom))
                insets
            }
        }
        setup(savedInstanceState)
        binding.root.wireSwitchRows()
    }

    abstract fun setup(savedInstanceState: Bundle?)
}

abstract class BaseFragment<VB : ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB) : Fragment() {
    private var _binding: VB? = null
    protected val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { setup(savedInstanceState); view.wireSwitchRows() }
    abstract fun setup(savedInstanceState: Bundle?)

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

/** Centered card dialog (custom layout, rounded surface). */
abstract class BaseDialog<VB : ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB) : DialogFragment() {
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    open val fullScreen = false

    override fun getTheme() = if (fullScreen) R.style.Theme_IPTV_FullDialog else R.style.Theme_IPTV_DialogWindow

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflate(inflater, container, false).also { _binding = it }.root

    override fun onStart() {
        super.onStart()
        if (fullScreen) dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { setup(savedInstanceState); view.wireSwitchRows() }
    abstract fun setup(savedInstanceState: Bundle?)
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

abstract class BaseBottomSheet<VB : ViewBinding>(private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB) : BottomSheetDialogFragment() {
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    /** Expand to (almost) full height, e.g. the WebView guide. */
    open val tall = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflate(inflater, container, false).also { _binding = it }.root

    override fun onStart() {
        super.onStart()
        if (tall) {
            val sheet = (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.layoutParams?.height = (resources.displayMetrics.heightPixels * 0.94f).toInt()
            sheet?.requestLayout()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { setup(savedInstanceState); view.wireSwitchRows() }
    abstract fun setup(savedInstanceState: Bundle?)
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
