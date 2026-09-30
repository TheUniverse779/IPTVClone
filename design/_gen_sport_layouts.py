"""Sport + Community layouts."""
import io, os
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'layout')
H = '<?xml version="1.0" encoding="utf-8"?>\n'
NS = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools"'
L = {}

L['fragment_sport'] = f'''<androidx.swiperefreshlayout.widget.SwipeRefreshLayout {NS} android:id="@+id/refresh" android:layout_width="match_parent" android:layout_height="match_parent">
<androidx.core.widget.NestedScrollView android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingBottom="110dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
        <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
        <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content" android:scrollbars="none">
            <com.google.android.material.chip.ChipGroup android:id="@+id/sports" android:layout_width="wrap_content" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="4dp" app:selectionRequired="true" app:singleLine="true" app:singleSelection="true" />
        </HorizontalScrollView>
        <ProgressBar android:id="@+id/loading" style="?android:attr/progressBarStyleSmall" android:layout_width="24dp" android:layout_height="24dp" android:layout_gravity="center_horizontal" android:layout_marginTop="24dp" android:indeterminateTint="@color/accent" />
        <include android:id="@+id/secLive" layout="@layout/layout_section_header" />
        <LinearLayout android:id="@+id/live" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingStart="16dp" android:paddingEnd="16dp" />
        <include android:id="@+id/secMine" layout="@layout/layout_section_header" />
        <LinearLayout android:id="@+id/mine" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingStart="16dp" android:paddingEnd="16dp" />
        <TextView android:id="@+id/mineHint" style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" android:text="@string/follow_hint" />
        <include android:id="@+id/secUpcoming" layout="@layout/layout_section_header" />
        <LinearLayout android:id="@+id/upcoming" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingStart="16dp" android:paddingEnd="16dp" />
        <include android:id="@+id/empty" layout="@layout/layout_empty_state" android:visibility="gone" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>'''

L['activity_sport_matches'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <androidx.recyclerview.widget.RecyclerView android:id="@+id/dates" android:layout_width="match_parent" android:layout_height="wrap_content" android:clipToPadding="false" android:orientation="horizontal" android:paddingStart="16dp" android:paddingEnd="16dp" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
    <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:scrollbars="none">
        <com.google.android.material.chip.ChipGroup android:id="@+id/leagues" android:layout_width="wrap_content" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" app:selectionRequired="true" app:singleLine="true" app:singleSelection="true" />
    </HorizontalScrollView>
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_marginTop="12dp" android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv" android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="16dp" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
        <ProgressBar android:id="@+id/loading" style="?android:attr/progressBarStyleSmall" android:layout_width="24dp" android:layout_height="24dp" android:layout_gravity="center_horizontal" android:layout_marginTop="24dp" android:indeterminateTint="@color/accent" />
        <include android:id="@+id/empty" layout="@layout/layout_empty_state" android:visibility="gone" />
    </FrameLayout>
</LinearLayout>'''

L['item_date'] = f'''<LinearLayout {NS} android:layout_width="56dp" android:layout_height="64dp" android:layout_marginEnd="8dp" android:background="@drawable/bg_date_item" android:gravity="center" android:orientation="vertical">
    <TextView android:id="@+id/dow" style="@style/Text" android:layout_width="wrap_content" android:layout_height="wrap_content" android:duplicateParentState="true" android:maxLines="1" android:textColor="@color/seg_text" android:textSize="11sp" />
    <TextView android:id="@+id/day" android:layout_width="wrap_content" android:layout_height="wrap_content" android:duplicateParentState="true" android:fontFamily="@font/barlow_condensed_bold" android:textColor="@color/date_day" android:textSize="22sp" />
</LinearLayout>'''

L['activity_my_match'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv" android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="16dp" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
        <include android:id="@+id/empty" layout="@layout/layout_empty_state" android:visibility="gone" />
    </FrameLayout>
</LinearLayout>'''

L['activity_match_detail'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <androidx.core.widget.NestedScrollView android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="24dp">
            <include android:id="@+id/card" layout="@layout/item_match" />
            <com.google.android.material.button.MaterialButton android:id="@+id/btnWatchMain" style="@style/Btn.Primary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="4dp" android:text="@string/watch_on_your_channel" app:icon="@drawable/ic_play" />
            <TextView android:id="@+id/secEvents" style="@style/Text.Cond.Section" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="20dp" android:layout_marginBottom="10dp" android:text="@string/match_events" />
            <LinearLayout android:id="@+id/events" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_card" android:orientation="vertical" android:padding="14dp" />
            <LinearLayout android:id="@+id/venueCard" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="14dp" android:background="@drawable/bg_card" android:orientation="vertical" android:padding="16dp">
                <TextView style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/venue" />
                <TextView android:id="@+id/venue" style="@style/Text.Body.Secondary" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="4dp" />
            </LinearLayout>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</LinearLayout>'''

L['activity_community'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <include android:id="@+id/seg" layout="@layout/view_segmented_3" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_marginEnd="16dp" android:layout_marginBottom="12dp" />
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv" android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingStart="16dp" android:paddingEnd="16dp" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
        <include android:id="@+id/empty" layout="@layout/layout_empty_state" android:visibility="gone" />
    </FrameLayout>
    <TextView android:id="@+id/note" style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:padding="16dp" android:text="@string/comm_note" />
</LinearLayout>'''

L['dialog_share'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_dialog" android:orientation="vertical" android:paddingStart="20dp" android:paddingTop="24dp" android:paddingEnd="20dp" android:paddingBottom="16dp">
    <TextView style="@style/Text.Cond.Dialog" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="14dp" android:text="@string/share_to_community" />
    <include android:id="@+id/seg" layout="@layout/view_segmented_3" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="14dp" />
    <TextView android:id="@+id/l1" style="@style/FieldLabel" android:layout_width="wrap_content" android:layout_height="wrap_content" />
    <EditText android:id="@+id/e1" style="@style/Input" android:importantForAutofill="no" android:inputType="text" />
    <TextView android:id="@+id/l2" style="@style/FieldLabel" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="14dp" />
    <EditText android:id="@+id/e2" style="@style/Input" android:importantForAutofill="no" android:inputType="textUri|textNoSuggestions" />
    <TextView android:id="@+id/l3" style="@style/FieldLabel" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="14dp" android:text="@string/password" />
    <EditText android:id="@+id/e3" style="@style/Input" android:importantForAutofill="no" android:inputType="text" />
    <TextView style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:text="@string/share_note" />
    <include android:id="@+id/buttons" layout="@layout/view_two_buttons" />
</LinearLayout>'''

for n, x in L.items():
    io.open(os.path.join(OUT, n + '.xml'), 'w', encoding='utf-8').write(H + x + '\n')
C = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'color')
io.open(os.path.join(C, 'date_day.xml'), 'w', encoding='utf-8').write(H + '''<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/white" android:state_selected="true" />
    <item android:color="@color/text" />
</selector>
''')
print(len(L), 'layouts')
