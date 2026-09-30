"""Common/reusable layouts. Each file mirrors a block in design/components.css."""
import io, os
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'layout')
H = '<?xml version="1.0" encoding="utf-8"?>\n'
NS = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools"'
L = {}

# ---- toolbar: back + title/subtitle + actions container ----
L['layout_toolbar'] = f'''<LinearLayout {NS}
    android:id="@+id/toolbar" android:layout_width="match_parent" android:layout_height="@dimen/h_toolbar"
    android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="8dp" android:paddingEnd="8dp">
    <ImageButton android:id="@+id/btnBack" style="@style/IconBtn" android:contentDescription="@string/back" android:src="@drawable/ic_back" />
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:paddingStart="8dp" android:paddingEnd="8dp">
        <TextView android:id="@+id/tvTitle" style="@style/Text.Cond.Toolbar" android:layout_width="match_parent" android:layout_height="wrap_content" tools:text="Title" />
        <TextView android:id="@+id/tvSubtitle" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:maxLines="1" android:ellipsize="end" android:visibility="gone" tools:text="Subtitle" tools:visibility="visible" />
    </LinearLayout>
    <LinearLayout android:id="@+id/actions" android:layout_width="wrap_content" android:layout_height="wrap_content" android:gravity="center_vertical" android:orientation="horizontal" />
</LinearLayout>'''

# ---- empty state ----
L['layout_empty_state'] = f'''<LinearLayout {NS}
    android:id="@+id/empty" android:layout_width="match_parent" android:layout_height="wrap_content"
    android:gravity="center_horizontal" android:orientation="vertical" android:paddingStart="32dp" android:paddingTop="40dp" android:paddingEnd="32dp" android:paddingBottom="40dp">
    <FrameLayout android:layout_width="88dp" android:layout_height="88dp" android:background="@drawable/bg_card" android:layout_marginBottom="16dp">
        <ImageView android:id="@+id/emptyIcon" android:layout_width="32dp" android:layout_height="32dp" android:layout_gravity="center" app:tint="@color/accent" tools:src="@drawable/ic_search" />
    </FrameLayout>
    <TextView android:id="@+id/emptyTitle" style="@style/Text.Cond" android:layout_width="wrap_content" android:layout_height="wrap_content" android:gravity="center" tools:text="Nothing here" />
    <TextView android:id="@+id/emptyBody" style="@style/Text.Body.Secondary" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:gravity="center" android:lineSpacingMultiplier="1.15" tools:text="Body" />
    <com.google.android.material.button.MaterialButton android:id="@+id/emptyAction" style="@style/Btn.Primary" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="18dp" android:visibility="gone" />
</LinearLayout>'''

# ---- section header ----
L['layout_section_header'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="bottom" android:orientation="horizontal"
    android:paddingStart="16dp" android:paddingTop="20dp" android:paddingEnd="16dp" android:paddingBottom="10dp">
    <TextView android:id="@+id/secTitle" style="@style/Text.Cond.Section" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" tools:text="Section" />
    <TextView android:id="@+id/secAction" style="@style/Text.Link" android:layout_width="wrap_content" android:layout_height="wrap_content" android:background="?attr/selectableItemBackground" android:padding="2dp" android:visibility="gone" tools:text="See all" tools:visibility="visible" />
</LinearLayout>'''

# ---- action card (icon wrap + title + sub + chevron) ----
L['item_action_card'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_card_press"
    android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="14dp" android:paddingTop="14dp" android:paddingEnd="12dp" android:paddingBottom="14dp">
    <FrameLayout android:id="@+id/icWrap" android:layout_width="44dp" android:layout_height="44dp" android:background="@drawable/bg_ic_wrap">
        <ImageView android:id="@+id/icon" android:layout_width="24dp" android:layout_height="24dp" android:layout_gravity="center" app:tint="@color/accent" tools:src="@drawable/ic_link" />
    </FrameLayout>
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="14dp" android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/title" style="@style/Text.RowTitle" android:layout_width="match_parent" android:layout_height="wrap_content" tools:text="Playlist URL" />
        <TextView android:id="@+id/sub" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" tools:text="Paste a .m3u link" />
    </LinearLayout>
    <ImageView android:id="@+id/trail" android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_chevron" app:tint="@color/text_3" />
</LinearLayout>'''

# ---- guide link strip (accent-soft) ----
L['layout_guide_link'] = f'''<LinearLayout {NS}
    android:id="@+id/guideLink" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_accent_soft"
    android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="14dp" android:paddingTop="12dp" android:paddingEnd="12dp" android:paddingBottom="12dp">
    <ImageView android:id="@+id/glIcon" android:layout_width="24dp" android:layout_height="24dp" android:src="@drawable/ic_help" app:tint="@color/accent" />
    <TextView android:id="@+id/glText" style="@style/Text" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="10dp" android:layout_marginEnd="8dp" android:layout_weight="1" android:textSize="13sp" android:lineSpacingMultiplier="1.15" tools:text="No link yet?" />
    <ImageView android:id="@+id/glChevron" android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_chevron" app:tint="@color/text_2" />
</LinearLayout>'''

# ---- switch row ----
L['layout_switch_row'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:minHeight="52dp" android:orientation="horizontal">
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/swTitle" style="@style/Text" android:layout_width="match_parent" android:layout_height="wrap_content" tools:text="Lock" />
        <TextView android:id="@+id/swSub" style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:visibility="gone" />
    </LinearLayout>
    <com.google.android.material.materialswitch.MaterialSwitch android:id="@+id/sw" style="@style/Switch" android:layout_width="wrap_content" android:layout_height="wrap_content" />
</LinearLayout>'''

# ---- channel list row (item_channel_list) ----
L['item_channel_list'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_row"
    android:gravity="center_vertical" android:minHeight="@dimen/h_row" android:orientation="horizontal"
    android:paddingStart="12dp" android:paddingTop="8dp" android:paddingEnd="4dp" android:paddingBottom="8dp">
    <TextView android:id="@+id/num" style="@style/Text.Cond" android:layout_width="30dp" android:layout_height="wrap_content" android:gravity="end" android:textColor="@color/text_3" android:textSize="16sp" tools:text="1" />
    <include android:id="@+id/logo" layout="@layout/view_logo" android:layout_width="@dimen/logo" android:layout_height="@dimen/logo" android:layout_marginStart="12dp" />
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/name" style="@style/Text.RowTitle" android:layout_width="match_parent" android:layout_height="wrap_content" tools:text="HTV Sports" />
        <TextView android:id="@+id/sub" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:maxLines="1" android:ellipsize="end" tools:text="Sports · 1080p" />
    </LinearLayout>
    <ImageView android:id="@+id/lockIcon" android:layout_width="16dp" android:layout_height="16dp" android:src="@drawable/ic_lock" android:visibility="gone" app:tint="@color/text_3" />
    <ImageButton android:id="@+id/btnFav" style="@style/IconBtn" android:contentDescription="@string/favourite" android:src="@drawable/ic_heart" app:tint="@color/fav_tint" />
    <ImageButton android:id="@+id/btnMore" style="@style/IconBtn" android:contentDescription="@string/more" android:src="@drawable/ic_more" app:tint="@color/text_2" />
</LinearLayout>'''

# ---- channel grid tile ----
L['item_channel_grid'] = f'''<FrameLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_margin="5dp" android:background="@drawable/bg_card_press">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_horizontal" android:orientation="vertical" android:paddingStart="8dp" android:paddingTop="12dp" android:paddingEnd="8dp" android:paddingBottom="10dp">
        <include android:id="@+id/logo" layout="@layout/view_logo" android:layout_width="@dimen/logo" android:layout_height="@dimen/logo" />
        <TextView android:id="@+id/name" style="@style/Text.Semibold" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:ellipsize="end" android:gravity="center" android:lines="2" android:textSize="12sp" tools:text="HTV Sports" />
    </LinearLayout>
    <ImageButton android:id="@+id/btnFav" style="@style/IconBtn" android:layout_width="30dp" android:layout_height="30dp" android:layout_gravity="top|end" android:layout_margin="2dp" android:padding="7dp" android:src="@drawable/ic_heart" app:tint="@color/fav_tint" />
</FrameLayout>'''

# ---- logo view: image over colored initials ----
L['view_logo'] = f'''<FrameLayout {NS} android:layout_width="@dimen/logo" android:layout_height="@dimen/logo">
    <TextView android:id="@+id/initials" android:layout_width="match_parent" android:layout_height="match_parent" android:background="@drawable/bg_logo" android:fontFamily="@font/barlow_condensed_bold" android:gravity="center" android:textColor="@color/white" android:textSize="14sp" tools:text="HTV" />
    <ImageView android:id="@+id/image" android:layout_width="match_parent" android:layout_height="match_parent" android:padding="4dp" android:scaleType="fitCenter" />
</FrameLayout>'''

# ---- group row ----
L['item_group'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="56dp" android:background="@drawable/bg_row" android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="16dp" android:paddingEnd="12dp">
    <TextView android:id="@+id/name" style="@style/Text" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:fontFamily="@font/barlow_medium" android:maxLines="1" android:ellipsize="end" android:textSize="16sp" tools:text="Sports" />
    <TextView android:id="@+id/count" style="@style/Text.Cond" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginEnd="8dp" android:textColor="@color/text_2" android:textSize="16sp" tools:text="12" />
    <ImageView android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_chevron" app:tint="@color/text_3" />
</LinearLayout>'''

# ---- playlist card ----
L['item_playlist_card'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="10dp" android:background="@drawable/bg_card_press"
    android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="12dp" android:paddingTop="12dp" android:paddingEnd="4dp" android:paddingBottom="12dp">
    <FrameLayout android:layout_width="48dp" android:layout_height="48dp">
        <FrameLayout android:layout_width="48dp" android:layout_height="48dp" android:background="@drawable/bg_ic_wrap">
            <ImageView android:id="@+id/icon" android:layout_width="24dp" android:layout_height="24dp" android:layout_gravity="center" android:src="@drawable/ic_list" app:tint="@color/accent" />
        </FrameLayout>
        <FrameLayout android:id="@+id/lockBadge" android:layout_width="22dp" android:layout_height="22dp" android:layout_gravity="bottom|end" android:layout_marginEnd="-4dp" android:layout_marginBottom="-4dp" android:background="@drawable/bg_badge_round" android:visibility="gone">
            <ImageView android:layout_width="12dp" android:layout_height="12dp" android:layout_gravity="center" android:src="@drawable/ic_lock" />
        </FrameLayout>
    </FrameLayout>
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/name" style="@style/Text.RowTitle" android:layout_width="match_parent" android:layout_height="wrap_content" tools:text="Vietnam free TV" />
        <TextView android:id="@+id/meta" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:maxLines="1" android:ellipsize="end" tools:text="42 channels · 2 hours ago" />
    </LinearLayout>
    <ImageButton android:id="@+id/btnFav" style="@style/IconBtn" android:src="@drawable/ic_heart" app:tint="@color/fav_tint" />
    <ImageButton android:id="@+id/btnMore" style="@style/IconBtn" android:src="@drawable/ic_more" />
</LinearLayout>'''

# ---- single stream row ----
L['item_single'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_row" android:gravity="center_vertical" android:minHeight="@dimen/h_row" android:orientation="horizontal" android:paddingStart="16dp" android:paddingEnd="4dp">
    <FrameLayout android:layout_width="@dimen/logo" android:layout_height="@dimen/logo" android:background="@drawable/bg_logo">
        <ImageView android:layout_width="20dp" android:layout_height="20dp" android:layout_gravity="center" android:src="@drawable/ic_link" app:tint="@color/accent" />
    </FrameLayout>
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/name" style="@style/Text.RowTitle" android:layout_width="match_parent" android:layout_height="wrap_content" tools:text="Concert live" />
        <TextView android:id="@+id/url" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:maxLines="1" android:ellipsize="middle" tools:text="https://example.org/live.m3u8" />
    </LinearLayout>
    <ImageButton android:id="@+id/btnMore" style="@style/IconBtn" android:src="@drawable/ic_more" />
</LinearLayout>'''

# ---- simple option row (sheets) ----
L['item_option'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_row" android:gravity="center_vertical" android:minHeight="52dp" android:orientation="horizontal" android:paddingStart="12dp" android:paddingEnd="12dp">
    <ImageView android:id="@+id/icon" android:layout_width="20dp" android:layout_height="20dp" android:layout_marginEnd="12dp" android:visibility="gone" app:tint="@color/text" />
    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical" android:paddingTop="6dp" android:paddingBottom="6dp">
        <TextView android:id="@+id/title" style="@style/Text" android:layout_width="match_parent" android:layout_height="wrap_content" android:textSize="15sp" tools:text="Option" />
        <TextView android:id="@+id/sub" style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:visibility="gone" />
    </LinearLayout>
    <ImageView android:id="@+id/check" android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_check" android:visibility="gone" app:tint="@color/accent" />
</LinearLayout>'''

# ---- sheet header (grab + title + close) ----
L['layout_sheet_header'] = f'''<LinearLayout {NS}
    android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
    <View android:layout_width="36dp" android:layout_height="4dp" android:layout_gravity="center_horizontal" android:layout_marginTop="10dp" android:layout_marginBottom="2dp" android:background="@drawable/bg_grab" />
    <LinearLayout android:layout_width="match_parent" android:layout_height="52dp" android:gravity="center_vertical" android:paddingStart="8dp" android:paddingEnd="8dp">
        <TextView android:id="@+id/sheetStart" style="@style/Btn.Text.Muted" android:layout_width="wrap_content" android:layout_height="40dp" android:gravity="center" android:minWidth="40dp" android:paddingStart="8dp" android:paddingEnd="8dp" android:textSize="14sp" />
        <TextView android:id="@+id/sheetTitle" style="@style/Text.Cond" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:maxLines="1" android:ellipsize="end" tools:text="Title" />
        <ImageButton android:id="@+id/sheetEnd" style="@style/IconBtn" android:src="@drawable/ic_close" />
    </LinearLayout>
</LinearLayout>'''

os.makedirs(OUT, exist_ok=True)
for n, x in L.items():
    io.open(os.path.join(OUT, n + '.xml'), 'w', encoding='utf-8').write(H + x + '\n')
print(len(L), 'common layouts')
