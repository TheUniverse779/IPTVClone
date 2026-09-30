"""Xtream tab fragments + detail/category/recent layouts."""
import io, os
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'layout')
H = '<?xml version="1.0" encoding="utf-8"?>\n'
NS = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools"'
HS = 'android:layout_width="match_parent" android:layout_height="wrap_content" android:clipToPadding="false" android:orientation="horizontal" android:paddingStart="16dp" android:paddingEnd="16dp" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager"'
L = {}

L['fragment_xtream_movie'] = f'''<FrameLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent">
<androidx.core.widget.NestedScrollView android:id="@+id/scroll" android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingBottom="20dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
        <com.google.android.material.chip.ChipGroup android:id="@+id/filter" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="12dp" app:selectionRequired="true" app:singleSelection="true">
            <com.google.android.material.chip.Chip android:id="@+id/fAll" style="@style/Chip" android:layout_width="wrap_content" android:layout_height="wrap_content" android:checked="true" android:text="@string/filter_all" />
            <com.google.android.material.chip.Chip android:id="@+id/fMovies" style="@style/Chip" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/filter_movies" />
            <com.google.android.material.chip.Chip android:id="@+id/fSeries" style="@style/Chip" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/filter_series" />
        </com.google.android.material.chip.ChipGroup>

        <FrameLayout android:id="@+id/hero" android:layout_width="match_parent" android:layout_height="210dp" android:layout_marginStart="16dp" android:layout_marginEnd="16dp" android:background="@drawable/bg_card" android:clipToOutline="true" android:outlineProvider="background">
            <ImageView android:id="@+id/heroImage" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="centerCrop" />
            <View android:layout_width="match_parent" android:layout_height="match_parent" android:background="@drawable/bg_hero_shade" />
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_gravity="bottom" android:orientation="vertical" android:padding="16dp">
                <TextView android:id="@+id/heroTitle" style="@style/Text.Cond.Hero" android:layout_width="match_parent" android:layout_height="wrap_content" android:maxLines="2" android:ellipsize="end" android:textSize="30sp" />
                <TextView android:id="@+id/heroMeta" style="@style/Text.Caption" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="6dp" android:layout_marginBottom="12dp" android:drawableStart="@drawable/ic_star_fill_small" android:drawablePadding="4dp" android:textColor="#BFFFFFFF" />
                <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content">
                    <com.google.android.material.button.MaterialButton android:id="@+id/heroPlay" style="@style/Btn.Small.Primary" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/watch" app:icon="@drawable/ic_play" />
                    <com.google.android.material.button.MaterialButton android:id="@+id/heroInfo" style="@style/Btn.Small.Tonal" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="8dp" android:text="@string/details" app:icon="@drawable/ic_info" />
                </LinearLayout>
            </LinearLayout>
        </FrameLayout>

        <include android:id="@+id/secContinue" layout="@layout/layout_section_header" />
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvContinue" {HS} />
        <include android:id="@+id/secMovies" layout="@layout/layout_section_header" />
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvMovies" {HS} />
        <include android:id="@+id/secSeries" layout="@layout/layout_section_header" />
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvSeries" {HS} />
        <include android:id="@+id/secGenres" layout="@layout/layout_section_header" />
        <com.google.android.material.chip.ChipGroup android:id="@+id/genres" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
<include android:id="@+id/empty" layout="@layout/layout_empty_state" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_gravity="center" android:visibility="gone" />
</FrameLayout>'''

L['fragment_xtream_live'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
    <include android:id="@+id/search" layout="@layout/view_search_box" android:layout_width="match_parent" android:layout_height="44dp" android:layout_marginStart="16dp" android:layout_marginEnd="16dp" android:layout_marginBottom="10dp" />
    <LinearLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvCats" android:layout_width="108dp" android:layout_height="match_parent" android:background="@color/surface" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvLive" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
    </LinearLayout>
</LinearLayout>'''

L['fragment_xtream_search'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
    <include android:id="@+id/search" layout="@layout/view_search_box" android:layout_width="match_parent" android:layout_height="44dp" android:layout_marginStart="16dp" android:layout_marginEnd="16dp" android:layout_marginBottom="10dp" />
    <include android:id="@+id/secRecent" layout="@layout/layout_section_header" />
    <com.google.android.material.chip.ChipGroup android:id="@+id/recent" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" />
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv" android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingStart="11dp" android:paddingEnd="11dp" android:paddingBottom="16dp" />
        <include android:id="@+id/empty" layout="@layout/layout_empty_state" android:visibility="gone" />
    </FrameLayout>
</LinearLayout>'''

L['fragment_xtream_favorite'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
    <include android:id="@+id/seg" layout="@layout/view_segmented_2" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_marginEnd="16dp" android:layout_marginBottom="12dp" />
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvVod" android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingStart="11dp" android:paddingEnd="11dp" android:paddingBottom="16dp" />
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvLive" android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingStart="16dp" android:paddingEnd="8dp" android:visibility="gone" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
        <include android:id="@+id/empty" layout="@layout/layout_empty_state" android:visibility="gone" />
    </FrameLayout>
</LinearLayout>'''

L['activity_xtream_category'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content" android:scrollbars="none">
        <com.google.android.material.chip.ChipGroup android:id="@+id/cats" android:layout_width="wrap_content" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="12dp" app:selectionRequired="true" app:singleLine="true" app:singleSelection="true" />
    </HorizontalScrollView>
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv" android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingStart="11dp" android:paddingEnd="11dp" android:paddingBottom="16dp" />
        <include android:id="@+id/empty" layout="@layout/layout_empty_state" android:visibility="gone" />
    </FrameLayout>
</LinearLayout>'''

DETAIL_HEAD = '''
        <FrameLayout android:layout_width="match_parent" android:layout_height="240dp">
            <ImageView android:id="@+id/backdrop" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="centerCrop" />
            <View android:layout_width="match_parent" android:layout_height="match_parent" android:background="@drawable/bg_backdrop_shade" />
            <LinearLayout android:id="@+id/topBar" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="@dimen/h_toolbar" android:gravity="center_vertical" android:paddingStart="8dp" android:paddingEnd="8dp">
                <ImageButton android:id="@+id/btnBack" style="@style/IconBtn" android:background="@drawable/bg_round_dark" android:contentDescription="@string/back" android:src="@drawable/ic_back" />
                <Space android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1" />
                <ImageButton android:id="@+id/btnFav" style="@style/IconBtn" android:background="@drawable/bg_round_dark" android:contentDescription="@string/favourite" android:src="@drawable/ic_heart" />
            </LinearLayout>
        </FrameLayout>
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="-86dp" android:paddingStart="16dp" android:paddingEnd="16dp">
            <FrameLayout android:layout_width="104dp" android:layout_height="156dp" android:background="@drawable/bg_card" android:clipToOutline="true" android:elevation="8dp" android:outlineProvider="background">
                <ImageView android:id="@+id/poster" android:layout_width="match_parent" android:layout_height="match_parent" android:scaleType="centerCrop" />
            </FrameLayout>
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="14dp" android:layout_weight="1" android:orientation="vertical" android:paddingTop="40dp">
                <TextView android:id="@+id/title" style="@style/Text.Cond.Hero" android:layout_width="match_parent" android:layout_height="wrap_content" android:maxLines="3" android:ellipsize="end" android:textSize="28sp" />
                <TextView android:id="@+id/meta" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="6dp" />
            </LinearLayout>
        </LinearLayout>'''

L['activity_movie_detail'] = f'''<androidx.core.widget.NestedScrollView {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingBottom="24dp">{DETAIL_HEAD}
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="18dp" android:paddingStart="16dp" android:paddingEnd="16dp">
            <com.google.android.material.button.MaterialButton android:id="@+id/btnPlay" style="@style/Btn.Primary" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="@string/play" app:icon="@drawable/ic_play" />
            <com.google.android.material.button.MaterialButton android:id="@+id/btnTrailer" style="@style/Btn.Tonal" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="10dp" android:text="@string/trailer" app:icon="@drawable/ic_play_circle" />
        </LinearLayout>
        <ProgressBar android:id="@+id/loading" style="?android:attr/progressBarStyleSmall" android:layout_width="24dp" android:layout_height="24dp" android:layout_gravity="center_horizontal" android:layout_marginTop="16dp" android:indeterminateTint="@color/accent" />
        <TextView android:id="@+id/plot" style="@style/Text.Body.Secondary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="16dp" android:lineSpacingMultiplier="1.3" android:paddingStart="16dp" android:paddingEnd="16dp" />
        <LinearLayout android:id="@+id/facts" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:orientation="vertical" android:paddingStart="16dp" android:paddingEnd="16dp" />
        <include android:id="@+id/secSimilar" layout="@layout/layout_section_header" />
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rvSimilar" {HS} />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>'''

L['activity_series_detail'] = f'''<androidx.core.widget.NestedScrollView {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingBottom="24dp">{DETAIL_HEAD}
        <com.google.android.material.button.MaterialButton android:id="@+id/btnPlay" style="@style/Btn.Primary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_marginTop="18dp" android:layout_marginEnd="16dp" android:text="@string/play" app:icon="@drawable/ic_play" />
        <TextView android:id="@+id/plot" style="@style/Text.Body.Secondary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="14dp" android:lineSpacingMultiplier="1.3" android:maxLines="4" android:ellipsize="end" android:paddingStart="16dp" android:paddingEnd="16dp" />
        <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="16dp" android:scrollbars="none">
            <com.google.android.material.chip.ChipGroup android:id="@+id/seasons" android:layout_width="wrap_content" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" app:selectionRequired="true" app:singleLine="true" app:singleSelection="true" />
        </HorizontalScrollView>
        <ProgressBar android:id="@+id/loading" style="?android:attr/progressBarStyleSmall" android:layout_width="24dp" android:layout_height="24dp" android:layout_gravity="center_horizontal" android:layout_marginTop="16dp" android:indeterminateTint="@color/accent" />
        <LinearLayout android:id="@+id/episodes" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:orientation="vertical" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>'''

L['activity_xtream_recent'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv" android:layout_width="match_parent" android:layout_height="match_parent" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
        <include android:id="@+id/empty" layout="@layout/layout_empty_state" android:visibility="gone" />
    </FrameLayout>
</LinearLayout>'''

for n, x in L.items():
    io.open(os.path.join(OUT, n + '.xml'), 'w', encoding='utf-8').write(H + x + '\n')

D = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable')
io.open(os.path.join(D, 'bg_hero_shade.xml'), 'w', encoding='utf-8').write(H + '<shape xmlns:android="http://schemas.android.com/apk/res/android"><gradient android:angle="90" android:startColor="#D9000000" android:centerColor="#40000000" android:centerY="0.45" android:endColor="#00000000" /></shape>\n')
io.open(os.path.join(D, 'bg_backdrop_shade.xml'), 'w', encoding='utf-8').write(H + '<shape xmlns:android="http://schemas.android.com/apk/res/android"><gradient android:angle="90" android:startColor="@color/bg" android:centerColor="#80151419" android:centerY="0.35" android:endColor="#33151419" /></shape>\n')
print(len(L), 'xtream layouts')
