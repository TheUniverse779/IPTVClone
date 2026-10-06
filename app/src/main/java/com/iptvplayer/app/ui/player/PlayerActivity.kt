package com.iptvplayer.app.ui.player

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.google.android.material.chip.Chip
import com.iptvplayer.app.Features
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.datastore.SettingsStore
import com.iptvplayer.app.databinding.ActivityPlayerBinding
import com.iptvplayer.app.databinding.ItemChannelListBinding
import com.iptvplayer.app.player.AspectMode
import com.iptvplayer.app.player.PlaybackService
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.TimeFmt
import com.iptvplayer.app.util.darken
import com.iptvplayer.app.util.gradient
import com.iptvplayer.app.util.shareText
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * Separate Activity so PiP and orientation changes are isolated (configChanges handled here).
 * Portrait: 16:9 video + info + zapping list. Landscape: fullscreen + side channel panel.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlayerActivity : BaseActivity<ActivityPlayerBinding>(ActivityPlayerBinding::inflate) {
    @Inject lateinit var settings: SettingsStore
    private val vm: PlayerViewModel by viewModels()
    override val applyInsets = false

    private var fullscreen = false
    private var locked = false
    private var hideJob: Job? = null
    private var tickJob: Job? = null
    private var aspect = AspectMode.FIT
    private var userSeeking = false
    private var autoPip = true
    private var backgroundAudio = false
    private val audio by lazy { getSystemService(AudioManager::class.java) }

    private val listAdapter by lazy { zapAdapter() }
    private val sideAdapter by lazy { zapAdapter() }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.getIntExtra(EXTRA_PIP, 0)) {
                PIP_TOGGLE -> togglePlay()
                PIP_PREV -> vm.zap(-1)
                PIP_NEXT -> vm.zap(1)
            }
            updatePip()
        }
    }

    override fun setup(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.playerView.player = vm.manager.player
        // Read synchronously: onStart/onStop decide on these, and an async load could arrive after them.
        kotlinx.coroutines.runBlocking { settings.current() }.let { autoPip = it.autoPip; backgroundAudio = Features.BACKGROUND_AUDIO && it.backgroundAudio }

        intent.request()?.let { vm.open(it) }
        setupControls()
        setupGestures()
        setupBelow()
        applyOrientation(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

        vm.manager.player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { renderPlayButton(); updatePip() }
        })
        collect(vm.now) { n -> n?.let { renderNow(it) } }
        // Scroll to the playing channel once the list is actually applied (it arrives after currentKey).
        collect(vm.list) { l -> listAdapter.submitList(l) { scrollToCurrent() }; sideAdapter.submitList(l) }
        collect(vm.currentKey) { listAdapter.notifyDataSetChanged(); sideAdapter.notifyDataSetChanged(); scrollToCurrent() }
        collect(vm.manager.buffering) { binding.buffering.visible(it && vm.manager.error.value == null) }
        collect(vm.manager.error) { e ->
            binding.err.root.visible(e != null)
            binding.err.btnNextCh.visible(vm.list.value.isNotEmpty())
            if (e != null) showControls(false)
        }
        collect(vm.manager.sleepLeft) { left ->
            binding.ctl.timerChip.visible(left != null)
            binding.ctl.btnTimer.isSelected = left != null
            binding.ctl.btnTimer.setColorFilter(getColor(if (left != null) R.color.accent else R.color.white))
            left?.let { binding.ctl.timerChip.text = TimeFmt.clock(it) }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    locked -> toast(R.string.locked_hint)
                    binding.sidePanel.visibility == View.VISIBLE -> binding.sidePanel.visible(false)
                    fullscreen -> setFullscreen(false)
                    else -> finish()
                }
            }
        })
        ContextCompat.registerReceiver(this, pipReceiver, IntentFilter(ACTION_PIP), ContextCompat.RECEIVER_NOT_EXPORTED)
        showControls(true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.request()?.let { vm.open(it) }
    }

    private fun Intent.request(): PlayRequest? = IntentCompat.getParcelableExtra(this, EXTRA_REQ, PlayRequest::class.java)

    // ---------------- rendering ----------------
    private fun renderNow(n: NowPlaying) {
        binding.ctl.title.text = n.title
        binding.ctl.title.isSelected = true
        binding.ctl.subtitle.text = n.subtitle
        binding.nowTitle.text = n.title
        binding.nowSub.text = n.subtitle
        binding.nowLogo.root.visible(n.isLive)
        if (n.isLive) LogoUtil.bind(binding.nowLogo.image, binding.nowLogo.initials, n.title, n.logo)
        val favVisible = n.favoriteKey != null
        binding.nowFav.visible(favVisible); binding.ctl.btnFav.visible(favVisible)
        binding.nowFav.isSelected = n.isFavorite
        binding.nowFav.setImageResource(if (n.isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart)
        binding.ctl.btnFav.setImageResource(if (n.isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart)
        binding.ctl.btnFav.setColorFilter(getColor(if (n.isFavorite) R.color.accent else R.color.white))
        // live vs VOD controls
        binding.ctl.liveBadge.visible(n.isLive)
        binding.ctl.position.visible(!n.isLive); binding.ctl.duration.visible(!n.isLive)
        binding.ctl.seek.isEnabled = !n.isLive
        binding.ctl.seek.progressDrawable = getDrawable(if (n.isLive) R.drawable.bg_seek_live else R.drawable.bg_seek)
        binding.ctl.seek.thumb = if (n.isLive) null else getDrawable(R.drawable.seek_thumb)
        if (n.isLive) binding.ctl.seek.progress = binding.ctl.seek.max
        val hasList = vm.list.value.isNotEmpty()
        binding.ctl.btnPrev.setImageResource(if (n.isLive) R.drawable.ic_prev else R.drawable.ic_rewind)
        binding.ctl.btnNext.setImageResource(if (n.isLive) R.drawable.ic_next else R.drawable.ic_forward)
        binding.ctl.btnPrev.visible(!n.isLive || hasList); binding.ctl.btnNext.visible(!n.isLive || hasList)
        binding.ctl.tChannels.visible(hasList)
        binding.resumeCard.visible(!n.isLive && n.resumeMs > 0)
        binding.resumeTitle.text = getString(R.string.resume_from, TimeFmt.clock(n.resumeMs))
        binding.secList.root.visible(hasList); binding.rvList.visible(hasList)
        binding.secList.secTitle.setText(if (n.favoriteKey?.startsWith("x:") == true) R.string.xtream_live_list else R.string.channels_in_playlist)
        binding.sideTitle.text = n.listTitle
        val c = LogoUtil.color(n.title)
        binding.videoBox.background = gradient(darken(c), 0xFF0C0F18.toInt())
        renderPlayButton()
    }

    private fun renderPlayButton() {
        val playing = vm.manager.player.playWhenReady
        binding.ctl.btnPlay.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
    }

    // ---------------- controls ----------------
    private fun setupControls() = with(binding.ctl) {
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnPlay.setOnClickListener { togglePlay(); showControls(true) }
        btnPrev.setOnClickListener { if (vm.now.value?.isLive == true) vm.zap(-1) else vm.manager.player.seekBack(); showControls(true) }
        btnNext.setOnClickListener { if (vm.now.value?.isLive == true) vm.zap(1) else vm.manager.player.seekForward(); showControls(true) }
        btnFull.setOnClickListener { setFullscreen(!fullscreen) }
        btnFav.setOnClickListener { vm.toggleFavorite(); showControls(true) }
        btnShare.setOnClickListener { shareText(getString(R.string.share_app_text, packageName)) }
        btnTimer.setOnClickListener { PlayerSheets.timer(supportFragmentManager) }
        timerChip.setOnClickListener { PlayerSheets.timer(supportFragmentManager) }
        tChannels.setOnClickListener { binding.sidePanel.visible(binding.sidePanel.visibility != View.VISIBLE); scrollToCurrent(); showControls(true) }
        tSubs.setOnClickListener { PlayerSheets.subtitles(supportFragmentManager) }
        tAudio.setOnClickListener { PlayerSheets.audio(supportFragmentManager) }
        tAspect.setOnClickListener { PlayerSheets.aspect(supportFragmentManager, aspect) }
        tPip.setOnClickListener { enterPip() }
        tCast.setOnClickListener { PlayerSheets.cast(supportFragmentManager) }
        tCast.visible(Features.CAST)
        tLock.setOnClickListener { setLocked(true) }
        binding.btnUnlock.setOnClickListener { setLocked(false) }
        binding.sideClose.setOnClickListener { binding.sidePanel.visible(false) }
        binding.err.btnRetry.setOnClickListener { vm.manager.retry() }
        binding.err.btnErrBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.err.btnNextCh.setOnClickListener { vm.zap(1) }
        binding.err.btnNextCh.visible(vm.list.value.isNotEmpty())

        seek.max = 1000
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) position.text = TimeFmt.clock(vm.manager.player.duration * p / 1000)
            }
            override fun onStartTrackingTouch(s: SeekBar) { userSeeking = true; hideJob?.cancel() }
            override fun onStopTrackingTouch(s: SeekBar) {
                userSeeking = false
                vm.manager.player.seekTo(vm.manager.player.duration * s.progress / 1000)
                showControls(true)
            }
        })
        tickJob = lifecycleScope.launch {
            while (isActive) {
                val p = vm.manager.player
                if (!userSeeking && vm.now.value?.isLive == false && p.duration > 0) {
                    seek.progress = (p.currentPosition * 1000 / p.duration).toInt()
                    seek.secondaryProgress = (p.bufferedPosition * 1000 / p.duration).toInt()
                    position.text = TimeFmt.clock(p.currentPosition)
                    duration.text = TimeFmt.clock(p.duration)
                }
                delay(1000)
            }
        }
        supportFragmentManager.setFragmentResultListener(PlayerSheets.RESULT_ASPECT, this@PlayerActivity) { _, b -> setAspect(AspectMode.valueOf(b.getString("v")!!)) }
    }

    private fun togglePlay() { val p = vm.manager.player; if (p.playbackState == Player.STATE_ENDED) p.seekTo(0); p.playWhenReady = !p.playWhenReady }

    private fun setAspect(m: AspectMode) {
        aspect = m
        binding.playerView.resizeMode = when (m) {
            AspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            AspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectMode.R16_9, AspectMode.R4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        }
        val ratio = when (m) { AspectMode.R16_9 -> 16f / 9f; AspectMode.R4_3 -> 4f / 3f; else -> null }
        (binding.playerView.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame))?.setAspectRatio(ratio ?: 0f)
    }

    private fun showControls(show: Boolean) {
        hideJob?.cancel()
        val ctl = binding.ctl.ctlRoot
        if (locked) { ctl.visible(false); binding.btnUnlock.visible(show); if (show) scheduleHide(); return }
        ctl.animate().cancel()
        if (show) { ctl.visible(true); ctl.animate().alpha(1f).setDuration(150).start(); scheduleHide() }
        else ctl.animate().alpha(0f).setDuration(200).withEndAction { ctl.visible(false) }.start()
    }

    private fun scheduleHide() {
        hideJob = lifecycleScope.launch {
            delay(HIDE_MS)
            if (vm.manager.player.isPlaying && binding.sidePanel.visibility != View.VISIBLE) {
                if (locked) binding.btnUnlock.visible(false) else showControls(false)
            }
        }
    }

    private fun setLocked(v: Boolean) {
        locked = v
        binding.ctl.ctlRoot.visible(!v)
        binding.btnUnlock.visible(v)
        requestedOrientation = if (v) ActivityInfo.SCREEN_ORIENTATION_LOCKED else if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (v) toast(R.string.locked_hint)
        showControls(true)
    }

    // ---------------- gestures: tap, double-tap seek, vertical swipe brightness/volume ----------------
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        var mode = 0 // 1 brightness, 2 volume
        var startY = 0f; var startVal = 0f
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean { mode = 0; startY = e.y; return true }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val visible = if (locked) binding.btnUnlock.visibility == View.VISIBLE else binding.ctl.ctlRoot.visibility == View.VISIBLE
                showControls(!visible); return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (locked || vm.now.value?.isLive != false) return false
                val p = vm.manager.player
                if (e.x < binding.gestureLayer.width / 2) { p.seekBack(); pill(R.drawable.ic_rewind, "−10s") } else { p.seekForward(); pill(R.drawable.ic_forward, "+10s") }
                return true
            }
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                if (locked || e1 == null) return false
                if (mode == 0) {
                    if (abs(e2.y - e1.y) < 24 || abs(dy) < abs(dx)) return false
                    mode = if (e1.x < binding.gestureLayer.width / 2) 1 else 2
                    startVal = if (mode == 1) currentBrightness() else audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                }
                val delta = (startY - e2.y) / binding.gestureLayer.height
                val v = (startVal + delta).coerceIn(0f, 1f)
                if (mode == 1) {
                    window.attributes = window.attributes.apply { screenBrightness = v.coerceAtLeast(0.01f) }
                    pill(R.drawable.ic_sun, "${getString(R.string.brightness)} ${(v * 100).toInt()}%")
                } else {
                    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, (v * max).toInt(), 0)
                    pill(R.drawable.ic_volume, "${getString(R.string.volume)} ${(v * 100).toInt()}%")
                }
                return true
            }
        })
        binding.gestureLayer.setOnTouchListener { _, e -> detector.onTouchEvent(e); true }
    }

    private fun currentBrightness(): Float {
        val w = window.attributes.screenBrightness
        if (w >= 0) return w
        // Correct 0..255 scale (the original divided by 225).
        return Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
    }

    private var pillJob: Job? = null
    private fun pill(icon: Int, text: String) {
        binding.gesturePill.visible(true)
        binding.gesturePill.text = text
        binding.gesturePill.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        pillJob?.cancel()
        pillJob = lifecycleScope.launch { delay(900); binding.gesturePill.visible(false) }
    }

    // ---------------- portrait info + zapping list ----------------
    private fun setupBelow() {
        binding.rvList.adapter = listAdapter
        binding.rvSide.adapter = sideAdapter
        binding.nowFav.setOnClickListener { vm.toggleFavorite() }
        fun chip(icon: Int, text: Int, onClick: () -> Unit) = Chip(this, null, com.google.android.material.R.attr.chipStyle).apply {
            setText(text); setChipIconResource(icon); isChipIconVisible = true
            chipIconTint = getColorStateList(R.color.text_2); setTextColor(getColor(R.color.text_2))
            chipBackgroundColor = getColorStateList(R.color.surface_2); chipStrokeWidth = 0f
            setEnsureMinTouchTargetSize(false)
            setOnClickListener { onClick() }
            binding.tools.addView(this)
        }
        chip(R.drawable.ic_subtitle, R.string.subtitles) { PlayerSheets.subtitles(supportFragmentManager) }
        chip(R.drawable.ic_audio, R.string.audio) { PlayerSheets.audio(supportFragmentManager) }
        chip(R.drawable.ic_aspect, R.string.aspect) { PlayerSheets.aspect(supportFragmentManager, aspect) }
        chip(R.drawable.ic_pip, R.string.pip) { enterPip() }
        if (Features.CAST) chip(R.drawable.ic_cast, R.string.cast) { PlayerSheets.cast(supportFragmentManager) }
        // No Lock here: in portrait the chips and channel list stay tappable, so it would lock nothing.
        // Lock lives in the fullscreen tools row, where it covers the whole screen.
    }

    private fun zapAdapter() = SimpleAdapter<ZapItem, ItemChannelListBinding>(ItemChannelListBinding::inflate, { a, b -> a.key == b.key }) { b, z, i ->
        val cur = z.key == vm.currentKey.value
        b.num.text = (i + 1).toString()
        LogoUtil.bind(b.logo.image, b.logo.initials, z.name, z.logo)
        b.name.text = z.name
        b.name.setTextColor(getColor(if (cur) R.color.accent else R.color.text))
        b.sub.text = z.sub; b.sub.visible(z.sub.isNotEmpty())
        b.root.setBackgroundResource(if (cur) R.drawable.bg_row_current else R.drawable.bg_row)
        b.btnFav.visible(false); b.btnMore.visible(false); b.lockIcon.visible(false)
        b.root.setOnClickListener { vm.openKey(z.key) }
    }

    private fun scrollToCurrent() {
        val i = vm.list.value.indexOfFirst { it.key == vm.currentKey.value }
        if (i >= 0) { binding.rvList.scrollToPosition(i); binding.rvSide.scrollToPosition(i) }
    }

    // ---------------- orientation / fullscreen ----------------
    private fun setFullscreen(v: Boolean) {
        requestedOrientation = if (v) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (!v) lifecycleScope.launch { delay(1500); if (!fullscreen && !locked) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isInPip()) applyOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    private fun applyOrientation(landscape: Boolean) {
        fullscreen = landscape
        val cs = ConstraintSet().apply { clone(binding.root) }
        if (landscape) {
            cs.setDimensionRatio(binding.videoBox.id, null)
            cs.connect(binding.videoBox.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            cs.setVisibility(binding.below.id, View.GONE)
        } else {
            cs.setDimensionRatio(binding.videoBox.id, "H,16:9")
            cs.clear(binding.videoBox.id, ConstraintSet.BOTTOM)
            cs.setVisibility(binding.below.id, View.VISIBLE)
            binding.sidePanel.visible(false)
        }
        cs.applyTo(binding.root)
        binding.ctl.toolsRow.visible(landscape)
        binding.ctl.btnFull.setImageResource(if (landscape) R.drawable.ic_exit_full else R.drawable.ic_fullscreen)
        val ctrl = WindowCompat.getInsetsController(window, window.decorView)
        if (landscape) {
            ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, 0, 0, 0)
        } else {
            ctrl.show(WindowInsetsCompat.Type.systemBars())
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                if (!fullscreen) {
                    val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                    v.setPadding(b.left, b.top, b.right, b.bottom)
                }
                insets
            }
            androidx.core.view.ViewCompat.requestApplyInsets(binding.root)
        }
    }

    // ---------------- Picture-in-picture ----------------
    private fun pipSupported() = Build.VERSION.SDK_INT >= 26 && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    private fun isInPip() = Build.VERSION.SDK_INT >= 24 && isInPictureInPictureMode

    fun enterPip() {
        if (!pipSupported()) { toast(R.string.device_not_supported); return }
        runCatching { enterPictureInPictureMode(pipParams()) }
    }

    private fun pipParams(): PictureInPictureParams {
        fun action(icon: Int, title: Int, code: Int) = RemoteAction(Icon.createWithResource(this, icon), getString(title), getString(title),
            PendingIntent.getBroadcast(this, code, Intent(ACTION_PIP).setPackage(packageName).putExtra(EXTRA_PIP, code), PendingIntent.FLAG_IMMUTABLE))
        val playing = vm.manager.player.playWhenReady
        val actions = buildList {
            if (vm.list.value.isNotEmpty()) add(action(R.drawable.ic_prev, R.string.pip_prev, PIP_PREV))
            add(action(if (playing) R.drawable.ic_pause else R.drawable.ic_play, if (playing) R.string.pip_pause else R.string.pip_play, PIP_TOGGLE))
            if (vm.list.value.isNotEmpty()) add(action(R.drawable.ic_next, R.string.pip_next, PIP_NEXT))
        }
        val b = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).setActions(actions)
        if (Build.VERSION.SDK_INT >= 31) b.setAutoEnterEnabled(autoPip && playing).setSeamlessResizeEnabled(true)
        return b.build()
    }

    private fun updatePip() { if (pipSupported()) runCatching { setPictureInPictureParams(pipParams()) } }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // API 31+ uses setAutoEnterEnabled; older versions enter here.
        if (Build.VERSION.SDK_INT in 26..30 && autoPip && vm.manager.player.isPlaying) enterPip()
    }

    private var wasInPip = false

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Closing the PiP window with ×: Android stops the activity first, then reports PiP ended — so we
        // see "left PiP while already stopped". (Expanding PiP leaves it while started.) The user dismissed
        // the video: stop playback and the media notification even if background audio is on.
        if (!isInPictureInPictureMode && wasInPip && !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            vm.manager.stop()
            if (Features.BACKGROUND_AUDIO) stopService(Intent(this, PlaybackService::class.java))
            finish()
        }
        wasInPip = isInPictureInPictureMode
        binding.ctl.ctlRoot.visible(!isInPictureInPictureMode)
        binding.below.visible(!isInPictureInPictureMode && !fullscreen)
        binding.sidePanel.visible(false)
        if (isInPictureInPictureMode) {
            val cs = ConstraintSet().apply { clone(binding.root) }
            cs.setDimensionRatio(binding.videoBox.id, null)
            cs.connect(binding.videoBox.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            cs.applyTo(binding.root)
        } else applyOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    // ---------------- lifecycle ----------------
    override fun onStart() {
        super.onStart()
        if (backgroundAudio) startPlaybackService()
    }

    override fun onStop() {
        super.onStop()
        vm.saveProgress()
        if (isInPip()) return
        if (backgroundAudio) startPlaybackService() // keeps the process + audio alive with a media notification
        else vm.manager.player.pause()
    }

    private fun startPlaybackService() {
        // MediaSessionService promotes itself to foreground (media notification) once playback is active.
        runCatching { startService(Intent(this, PlaybackService::class.java)) }
    }

    override fun onDestroy() {
        unregisterReceiver(pipReceiver)
        tickJob?.cancel()
        if (isFinishing) {
            binding.playerView.player = null
            if (!backgroundAudio) vm.manager.stop()
            vm.manager.setSleepTimer(null)
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_REQ = "req"
        private const val ACTION_PIP = "com.iptvplayer.app.PIP"
        private const val EXTRA_PIP = "pip"
        private const val PIP_TOGGLE = 1; private const val PIP_PREV = 2; private const val PIP_NEXT = 3
        private const val HIDE_MS = 6_000L

        fun start(ctx: Context, req: PlayRequest) =
            ctx.startActivity(Intent(ctx, PlayerActivity::class.java).putExtra(EXTRA_REQ, req))
    }
}
