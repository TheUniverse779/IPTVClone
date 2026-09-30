"""Item/row layouts used by adapters."""
import io, os
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'layout')
H = '<?xml version="1.0" encoding="utf-8"?>\n'
NS = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools"'
L = {}

L['item_strip_channel'] = f'''<LinearLayout {NS}
    android:layout_width="wrap_content" android:layout_height="44dp" android:layout_marginEnd="8dp" android:background="@drawable/bg_card_2_press"
    android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="4dp" android:paddingEnd="12dp">
    <include android:id="@+id/logo" layout="@layout/view_logo" android:layout_width="36dp" android:layout_height="36dp" />
    <TextView android:id="@+id/name" style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="8dp" android:maxWidth="160dp" android:maxLines="1" android:ellipsize="end" android:textSize="13sp" tools:text="HTV1" />
</LinearLayout>'''

L['item_xtream_chip'] = f'''<LinearLayout {NS}
    android:layout_width="wrap_content" android:layout_height="56dp" android:layout_marginEnd="10dp" android:background="@drawable/bg_card_press"
    android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="10dp" android:paddingEnd="14dp">
    <TextView android:id="@+id/avatar" android:layout_width="36dp" android:layout_height="36dp" android:fontFamily="@font/barlow_condensed_bold" android:gravity="center" android:textColor="@color/white" android:textSize="17sp" tools:text="M" />
    <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="10dp" android:orientation="vertical">
        <TextView android:id="@+id/name" style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:maxWidth="150dp" android:maxLines="1" android:ellipsize="end" tools:text="My Xtream" />
        <TextView android:id="@+id/sub" style="@style/Text.Hint" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="11sp" tools:text="Valid until 12/03/2027" />
    </LinearLayout>
    <ImageView android:id="@+id/lock" android:layout_width="16dp" android:layout_height="16dp" android:layout_marginStart="8dp" android:src="@drawable/ic_lock" android:visibility="gone" app:tint="@color/text_2" />
</LinearLayout>'''

L['item_profile'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_horizontal" android:orientation="vertical" android:paddingTop="10dp" android:paddingBottom="10dp">
    <FrameLayout android:layout_width="104dp" android:layout_height="104dp">
        <View android:id="@+id/newRing" android:layout_width="100dp" android:layout_height="100dp" android:layout_gravity="center" android:background="@drawable/bg_profile_new_ring" android:visibility="gone" />
        <TextView android:id="@+id/avatar" android:layout_width="88dp" android:layout_height="88dp" android:layout_gravity="center" android:fontFamily="@font/barlow_condensed_bold" android:foreground="?attr/selectableItemBackground" android:gravity="center" android:textColor="@color/white" android:textSize="40sp" tools:background="#5B5BD6" tools:text="M" />
        <FrameLayout android:id="@+id/lockBadge" android:layout_width="28dp" android:layout_height="28dp" android:background="@drawable/bg_badge_round" android:visibility="gone">
            <ImageView android:layout_width="14dp" android:layout_height="14dp" android:layout_gravity="center" android:src="@drawable/ic_lock" />
        </FrameLayout>
        <TextView android:id="@+id/tag" style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="22dp" android:layout_gravity="top|end" android:gravity="center" android:paddingStart="8dp" android:paddingEnd="8dp" android:textSize="11sp" android:visibility="gone" tools:background="@drawable/bg_tag_new" tools:text="New" tools:visibility="visible" />
    </FrameLayout>
    <LinearLayout android:layout_width="wrap_content" android:layout_height="32dp" android:gravity="center_vertical" android:orientation="horizontal">
        <TextView android:id="@+id/name" style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:maxWidth="80dp" android:maxLines="1" android:ellipsize="end" tools:text="My Xtream" />
        <ImageButton android:id="@+id/btnMore" android:layout_width="28dp" android:layout_height="32dp" android:background="?attr/selectableItemBackgroundBorderless" android:contentDescription="@string/more" android:padding="4dp" android:src="@drawable/ic_more" app:tint="@color/text_2" />
    </LinearLayout>
    <TextView android:id="@+id/sub" style="@style/Text.Hint" android:layout_width="wrap_content" android:layout_height="wrap_content" android:gravity="center" android:textSize="11sp" tools:text="Valid until 12/03/2027" />
</LinearLayout>'''

L['item_profile_add'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_horizontal" android:orientation="vertical" android:paddingTop="10dp" android:paddingBottom="10dp">
    <FrameLayout android:layout_width="104dp" android:layout_height="104dp">
        <FrameLayout android:id="@+id/addBox" android:layout_width="88dp" android:layout_height="88dp" android:layout_gravity="center" android:background="@drawable/bg_profile_add" android:foreground="?attr/selectableItemBackground">
            <ImageView android:layout_width="32dp" android:layout_height="32dp" android:layout_gravity="center" android:src="@drawable/ic_plus" app:tint="@color/text_3" />
        </FrameLayout>
    </FrameLayout>
    <TextView style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="32dp" android:gravity="center" android:text="@string/add_profile" />
</LinearLayout>'''

L['item_poster'] = f'''<LinearLayout {NS}
    android:layout_width="112dp" android:layout_height="wrap_content" android:layout_marginEnd="10dp" android:orientation="vertical">
    <FrameLayout android:id="@+id/posterBox" android:layout_width="match_parent" android:layout_height="168dp" android:background="@drawable/bg_card" android:clipToOutline="true" android:foreground="?attr/selectableItemBackground" android:outlineProvider="background">
        <ImageView android:id="@+id/image" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="centerCrop" />
        <TextView android:id="@+id/fallback" style="@style/Text.Cond" android:layout_width="match_parent" android:layout_height="match_parent" android:gravity="bottom|start" android:padding="8dp" android:textSize="15sp" tools:text="Movie title" />
        <TextView android:id="@+id/rate" android:layout_width="wrap_content" android:layout_height="20dp" android:layout_margin="6dp" android:background="@drawable/bg_poster_rate" android:drawableStart="@drawable/ic_star_fill_small" android:drawablePadding="3dp" android:fontFamily="@font/barlow_semibold" android:gravity="center_vertical" android:paddingStart="6dp" android:paddingEnd="6dp" android:textColor="@color/gold" android:textSize="11sp" tools:text="4.5" />
    </FrameLayout>
    <TextView android:id="@+id/title" style="@style/Text.Semibold" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:maxLines="1" android:ellipsize="end" android:textSize="12sp" tools:text="Movie title" />
</LinearLayout>'''

L['item_continue'] = f'''<LinearLayout {NS}
    android:layout_width="220dp" android:layout_height="wrap_content" android:layout_marginEnd="10dp" android:orientation="vertical">
    <FrameLayout android:layout_width="match_parent" android:layout_height="124dp" android:background="@drawable/bg_card" android:clipToOutline="true" android:foreground="?attr/selectableItemBackground" android:outlineProvider="background">
        <ImageView android:id="@+id/image" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="centerCrop" />
        <View android:layout_width="match_parent" android:layout_height="match_parent" android:background="@drawable/bg_player_shade" />
        <FrameLayout android:layout_width="40dp" android:layout_height="40dp" android:layout_gravity="center" android:background="@drawable/bg_round_dark">
            <ImageView android:layout_width="20dp" android:layout_height="20dp" android:layout_gravity="center" android:src="@drawable/ic_play" />
        </FrameLayout>
        <TextView android:id="@+id/title" style="@style/Text.Cond" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_gravity="bottom" android:maxLines="1" android:ellipsize="end" android:padding="10dp" android:textSize="17sp" tools:text="Delta Nine" />
    </FrameLayout>
    <TextView android:id="@+id/sub" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:layout_marginBottom="4dp" tools:text="S2 · E4" />
    <ProgressBar android:id="@+id/progress" style="?android:attr/progressBarStyleHorizontal" android:layout_width="match_parent" android:layout_height="3dp" android:max="100" android:progressDrawable="@drawable/bg_progress" tools:progress="60" />
</LinearLayout>'''

L['item_episode'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_row" android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="16dp" android:paddingTop="10dp" android:paddingEnd="8dp" android:paddingBottom="10dp">
    <FrameLayout android:layout_width="120dp" android:layout_height="68dp" android:background="@drawable/bg_card_2" android:clipToOutline="true" android:outlineProvider="background">
        <ImageView android:id="@+id/image" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="centerCrop" />
        <TextView android:id="@+id/num" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_margin="6dp" android:background="@drawable/bg_ep_num" android:fontFamily="@font/barlow_condensed_bold" android:paddingStart="6dp" android:paddingEnd="6dp" android:textColor="@color/white" android:textSize="13sp" tools:text="1" />
        <ProgressBar android:id="@+id/progress" style="?android:attr/progressBarStyleHorizontal" android:layout_width="match_parent" android:layout_height="3dp" android:layout_gravity="bottom" android:layout_marginStart="6dp" android:layout_marginEnd="6dp" android:layout_marginBottom="6dp" android:max="100" android:progressDrawable="@drawable/bg_progress" android:visibility="gone" />
    </FrameLayout>
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/title" style="@style/Text.RowTitle" android:layout_width="match_parent" android:layout_height="wrap_content" android:textSize="15sp" tools:text="Pilot" />
        <TextView android:id="@+id/sub" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" tools:text="42 min" />
    </LinearLayout>
    <ImageButton android:id="@+id/btnRemove" style="@style/IconBtn" android:src="@drawable/ic_close" android:visibility="gone" app:tint="@color/text_2" />
</LinearLayout>'''

L['item_live_cat'] = f'''<TextView {NS}
    android:id="@+id/name" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_live_cat"
    android:fontFamily="@font/barlow_semibold" android:gravity="center_vertical|start" android:minHeight="48dp" android:paddingStart="12dp" android:paddingEnd="8dp"
    android:textColor="@color/tab_tint_text2" android:textSize="13sp" tools:text="Sports" />'''

L['item_live_channel'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_row" android:gravity="center_vertical" android:minHeight="@dimen/h_row" android:orientation="horizontal" android:paddingStart="12dp" android:paddingEnd="4dp">
    <include android:id="@+id/logo" layout="@layout/view_logo" android:layout_width="@dimen/logo" android:layout_height="@dimen/logo" />
    <TextView android:id="@+id/name" style="@style/Text.RowTitle" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:textSize="15sp" tools:text="HTV1" />
    <ImageButton android:id="@+id/btnFav" style="@style/IconBtn" android:src="@drawable/ic_heart" app:tint="@color/fav_tint" />
</LinearLayout>'''

L['item_match'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="10dp" android:background="@drawable/bg_card_press" android:orientation="vertical" android:paddingStart="14dp" android:paddingTop="12dp" android:paddingEnd="14dp" android:paddingBottom="12dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="10dp" android:gravity="center_vertical">
        <ImageView android:layout_width="16dp" android:layout_height="16dp" android:src="@drawable/ic_trophy" app:tint="@color/text_2" />
        <TextView android:id="@+id/league" style="@style/Text.Caption" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="8dp" android:layout_weight="1" tools:text="Premier League" />
        <TextView android:id="@+id/liveBadge" style="@style/LiveBadge" android:text="@string/live_badge" android:visibility="gone" />
        <ImageButton android:id="@+id/btnBell" android:layout_width="32dp" android:layout_height="32dp" android:background="@drawable/bg_icon_btn" android:padding="6dp" android:src="@drawable/ic_bell" android:visibility="gone" app:tint="@color/fav_tint" />
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center_horizontal" android:orientation="vertical">
            <TextView android:id="@+id/homeCrest" style="@style/Crest" tools:text="NOR" />
            <TextView android:id="@+id/home" style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:gravity="center" android:maxLines="2" android:textSize="13sp" tools:text="Northfield" />
        </LinearLayout>
        <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" android:gravity="center_horizontal" android:orientation="vertical" android:paddingStart="8dp" android:paddingEnd="8dp">
            <TextView android:id="@+id/score" style="@style/Text.Cond.Number" android:layout_width="wrap_content" android:layout_height="wrap_content" android:textSize="34sp" tools:text="2 – 1" />
            <TextView android:id="@+id/clock" style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="4dp" android:textColor="@color/live" android:textSize="12sp" tools:text="67'" />
        </LinearLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center_horizontal" android:orientation="vertical">
            <TextView android:id="@+id/awayCrest" style="@style/Crest" tools:text="RIV" />
            <TextView android:id="@+id/away" style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:gravity="center" android:maxLines="2" android:textSize="13sp" tools:text="Rivermouth" />
        </LinearLayout>
    </LinearLayout>
    <com.google.android.material.button.MaterialButton android:id="@+id/btnWatch" style="@style/Btn.Small.Primary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:text="@string/watch_on_your_channel" android:visibility="gone" app:icon="@drawable/ic_play" />
</LinearLayout>'''

L['item_chat_bot'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingStart="16dp" android:paddingTop="5dp" android:paddingEnd="56dp" android:paddingBottom="5dp">
    <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" android:background="@drawable/bg_bubble_bot" android:orientation="vertical" android:paddingStart="14dp" android:paddingTop="10dp" android:paddingEnd="14dp" android:paddingBottom="10dp">
        <TextView android:id="@+id/text" style="@style/Text" android:layout_width="wrap_content" android:layout_height="wrap_content" android:lineSpacingMultiplier="1.2" tools:text="Hello" />
        <TextView android:id="@+id/steps" style="@style/Text.Body.Secondary" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:lineSpacingMultiplier="1.3" android:visibility="gone" />
        <com.google.android.material.chip.ChipGroup android:id="@+id/actions" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:visibility="gone" />
        <ProgressBar android:id="@+id/typing" style="?android:attr/progressBarStyleSmall" android:layout_width="20dp" android:layout_height="20dp" android:indeterminateTint="@color/text_3" android:visibility="gone" />
    </LinearLayout>
</LinearLayout>'''

L['item_chat_user'] = f'''<FrameLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingStart="56dp" android:paddingTop="5dp" android:paddingEnd="16dp" android:paddingBottom="5dp">
    <TextView android:id="@+id/text" style="@style/Text" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_gravity="end" android:background="@drawable/bg_bubble_user" android:paddingStart="14dp" android:paddingTop="10dp" android:paddingEnd="14dp" android:paddingBottom="10dp" android:textColor="@color/white" tools:text="How?" />
</FrameLayout>'''

L['item_step'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:paddingBottom="14dp">
    <TextView android:id="@+id/n" android:layout_width="26dp" android:layout_height="26dp" android:background="@drawable/bg_hero_icon" android:fontFamily="@font/barlow_condensed_bold" android:gravity="center" android:textColor="@color/accent" tools:text="1" />
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:orientation="vertical" android:paddingTop="2dp">
        <TextView android:id="@+id/title" style="@style/Text.Semibold" android:layout_width="match_parent" android:layout_height="wrap_content" tools:text="Step" />
        <TextView android:id="@+id/body" style="@style/Text.Body.Secondary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:lineSpacingMultiplier="1.15" android:visibility="gone" />
    </LinearLayout>
</LinearLayout>'''

L['item_settings_row'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_row" android:gravity="center_vertical" android:minHeight="56dp" android:orientation="horizontal" android:paddingStart="16dp" android:paddingEnd="12dp">
    <ImageView android:id="@+id/icon" android:layout_width="22dp" android:layout_height="22dp" app:tint="@color/text_2" tools:src="@drawable/ic_lang" />
    <TextView android:id="@+id/title" style="@style/Text" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="14dp" android:layout_weight="1" android:fontFamily="@font/barlow_medium" android:textSize="15sp" tools:text="Language" />
    <TextView android:id="@+id/value" style="@style/Text.Hint" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginEnd="6dp" android:visibility="gone" />
    <com.google.android.material.materialswitch.MaterialSwitch android:id="@+id/sw" style="@style/Switch" android:layout_width="wrap_content" android:layout_height="wrap_content" android:visibility="gone" />
    <ImageView android:id="@+id/trail" android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_chevron" app:tint="@color/text_3" />
</LinearLayout>'''

L['item_community'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="10dp" android:background="@drawable/bg_card" android:orientation="vertical" android:padding="12dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical">
        <FrameLayout android:layout_width="@dimen/logo" android:layout_height="@dimen/logo" android:background="@drawable/bg_logo">
            <ImageView android:id="@+id/icon" android:layout_width="20dp" android:layout_height="20dp" android:layout_gravity="center" app:tint="@color/accent" tools:src="@drawable/ic_list" />
        </FrameLayout>
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:orientation="vertical">
            <TextView android:id="@+id/title" style="@style/Text.RowTitle" android:layout_width="match_parent" android:layout_height="wrap_content" tools:text="Asia news" />
            <TextView android:id="@+id/sub" style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:maxLines="1" android:ellipsize="middle" tools:text="https://…" />
        </LinearLayout>
        <ImageButton android:id="@+id/btnSave" style="@style/IconBtn" android:src="@drawable/ic_bookmark" app:tint="@color/fav_tint" />
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="10dp">
        <com.google.android.material.button.MaterialButton android:id="@+id/btnUse" style="@style/Btn.Small.Primary" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="@string/use_link" />
        <com.google.android.material.button.MaterialButton android:id="@+id/btnCopy" style="@style/Btn.Small.Tonal" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="8dp" app:icon="@drawable/ic_copy" app:iconPadding="0dp" />
        <com.google.android.material.button.MaterialButton android:id="@+id/btnReport" style="@style/Btn.Small.Tonal" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="8dp" app:icon="@drawable/ic_flag" app:iconPadding="0dp" />
    </LinearLayout>
</LinearLayout>'''

os.makedirs(OUT, exist_ok=True)
for n, x in L.items():
    io.open(os.path.join(OUT, n + '.xml'), 'w', encoding='utf-8').write(H + x + '\n')
print(len(L), 'item layouts')
