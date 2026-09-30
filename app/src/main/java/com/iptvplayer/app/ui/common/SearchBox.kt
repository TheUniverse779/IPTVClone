package com.iptvplayer.app.ui.common

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import com.iptvplayer.app.databinding.ViewSearchBoxBinding
import com.iptvplayer.app.util.visible

/** Wires the search box: debounced text callback, clear button, IME search action. */
fun ViewSearchBoxBinding.setup(hint: Int, debounceMs: Long = 300, onSubmit: ((String) -> Unit)? = null, onChange: (String) -> Unit) {
    et.setHint(hint)
    var pending: Runnable? = null
    et.doAfterTextChanged { e ->
        val q = e?.toString().orEmpty()
        btnClear.visible(q.isNotEmpty())
        pending?.let { et.removeCallbacks(it) }
        pending = Runnable { onChange(q.trim()) }.also { et.postDelayed(it, debounceMs) }
    }
    btnClear.setOnClickListener { et.setText("") }
    et.setOnEditorActionListener { v, id, _ ->
        if (id == EditorInfo.IME_ACTION_SEARCH) {
            (v.context.getSystemService(InputMethodManager::class.java)).hideSoftInputFromWindow(v.windowToken, 0)
            onSubmit?.invoke(v.text.toString().trim()); true
        } else false
    }
}
