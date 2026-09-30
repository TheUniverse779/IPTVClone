package com.iptvplayer.app.ui.disclaimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.data.datastore.SettingsStore
import com.iptvplayer.app.databinding.ActivityDisclaimerBinding
import com.iptvplayer.app.ui.main.MainActivity
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DisclaimerActivity : BaseActivity<ActivityDisclaimerBinding>(ActivityDisclaimerBinding::inflate) {
    @Inject lateinit var settings: SettingsStore
    private val viewOnly by lazy { intent.getBooleanExtra(EXTRA_VIEW_ONLY, false) }

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.license_title)
        binding.toolbar.btnBack.visible(viewOnly)
        binding.toolbar.btnBack.setOnClickListener { finish() }

        listOf(R.string.license_1, R.string.license_2, R.string.license_3, R.string.license_4, R.string.license_5, R.string.license_6)
            .forEachIndexed { i, res -> binding.items.addView(item(i + 1, getString(res))) }

        binding.bottomActions.visible(!viewOnly)
        binding.cbAgree.setOnCheckedChangeListener { _, checked -> binding.btnAccept.isEnabled = checked }
        binding.btnAccept.setOnClickListener {
            lifecycleScope.launch {
                settings.setDisclaimerAccepted()
                startActivity(Intent(this@DisclaimerActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun item(n: Int, text: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, 6.dp, 0, 6.dp)
        addView(TextView(context).apply {
            this.text = n.toString()
            setTextAppearance(R.style.Text_Cond)
            setTextColor(getColor(R.color.accent))
            textSize = 16f
            width = 22.dp
        })
        addView(TextView(context).apply {
            this.text = text
            setTextAppearance(R.style.Text_Body_Secondary)
            setLineSpacing(0f, 1.2f)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 10.dp })
    }

    companion object {
        private const val EXTRA_VIEW_ONLY = "view_only"
        fun start(ctx: Context, viewOnly: Boolean) = ctx.startActivity(Intent(ctx, DisclaimerActivity::class.java).putExtra(EXTRA_VIEW_ONLY, viewOnly))
    }
}
