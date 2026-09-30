"""Guide / FAQ / Chatbot / WebView sheet / Settings / Feedback layouts."""
import io, os
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'layout')
H = '<?xml version="1.0" encoding="utf-8"?>\n'
NS = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools"'
L = {}

L['activity_how_to_add'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <HorizontalScrollView android:layout_width="match_parent" android:layout_height="wrap_content" android:scrollbars="none">
        <com.google.android.material.chip.ChipGroup android:id="@+id/types" android:layout_width="wrap_content" android:layout_height="wrap_content" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="14dp" app:selectionRequired="true" app:singleLine="true" app:singleSelection="true" />
    </HorizontalScrollView>
    <androidx.core.widget.NestedScrollView android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="24dp">
            <TextView android:id="@+id/title" style="@style/Text.Cond.Hero" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="14dp" android:textSize="24sp" />
            <LinearLayout android:id="@+id/steps" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" />
            <TextView style="@style/Text.Cond.Section" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:layout_marginBottom="10dp" android:text="@string/suggested_sites" />
            <LinearLayout android:id="@+id/sites" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" />
            <TextView style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="14dp" android:lineSpacingMultiplier="1.2" android:text="@string/third_party_note" />
            <com.google.android.material.button.MaterialButton android:id="@+id/btnForm" style="@style/Btn.Primary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" />
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</LinearLayout>'''

L['sheet_web_guide'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
    <include android:id="@+id/header" layout="@layout/layout_sheet_header" />
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:paddingStart="8dp" android:paddingEnd="8dp" android:paddingBottom="8dp">
        <ImageButton android:id="@+id/btnWebBack" style="@style/IconBtn" android:layout_width="36dp" android:layout_height="36dp" android:padding="8dp" android:src="@drawable/ic_back" />
        <LinearLayout android:layout_width="0dp" android:layout_height="36dp" android:layout_marginStart="6dp" android:layout_marginEnd="6dp" android:layout_weight="1" android:background="@drawable/bg_ic_wrap" android:gravity="center_vertical" android:paddingStart="10dp" android:paddingEnd="10dp">
            <ImageView android:layout_width="14dp" android:layout_height="14dp" android:src="@drawable/ic_lock" app:tint="@color/text_2" />
            <TextView android:id="@+id/addr" style="@style/Text.Caption" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginStart="6dp" android:ellipsize="middle" android:singleLine="true" />
        </LinearLayout>
        <ImageButton android:id="@+id/btnReload" style="@style/IconBtn" android:layout_width="36dp" android:layout_height="36dp" android:padding="8dp" android:src="@drawable/ic_refresh" />
    </LinearLayout>
    <com.google.android.material.progressindicator.LinearProgressIndicator android:id="@+id/progress" android:layout_width="match_parent" android:layout_height="wrap_content" app:indicatorColor="@color/accent" app:trackColor="@android:color/transparent" app:trackThickness="2dp" />
    <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <WebView android:id="@+id/web" android:layout_width="match_parent" android:layout_height="match_parent" />
        <LinearLayout android:id="@+id/snack" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_gravity="bottom" android:layout_margin="12dp" android:background="@drawable/bg_snack" android:elevation="6dp" android:gravity="center_vertical" android:paddingStart="16dp" android:paddingEnd="4dp" android:visibility="gone">
            <TextView android:id="@+id/snackText" style="@style/Text.Semibold" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:maxLines="2" android:ellipsize="middle" android:textColor="@color/bg" />
            <com.google.android.material.button.MaterialButton android:id="@+id/snackAction" style="@style/Btn.Text" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/use_this_link" android:textColor="@color/accent_press" />
        </LinearLayout>
    </FrameLayout>
</LinearLayout>'''

L['activity_faq'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <androidx.core.widget.NestedScrollView android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="24dp">
            <LinearLayout android:id="@+id/items" android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_list_card" android:clipToOutline="true" android:orientation="vertical" />
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="16dp" android:background="@drawable/bg_card" android:gravity="center_vertical" android:padding="16dp">
                <ImageView android:layout_width="24dp" android:layout_height="24dp" android:src="@drawable/ic_chat" />
                <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:orientation="vertical">
                    <TextView style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/still_need_help" />
                    <TextView style="@style/Text.Hint" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/ask_or_feedback" />
                </LinearLayout>
                <com.google.android.material.button.MaterialButton android:id="@+id/btnAsk" style="@style/Btn.Small.Tonal" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/ask_assistant" />
            </LinearLayout>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</LinearLayout>'''

L['item_faq'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/bg_row" android:orientation="vertical" android:paddingStart="16dp" android:paddingTop="14dp" android:paddingEnd="12dp" android:paddingBottom="14dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical">
        <TextView android:id="@+id/q" style="@style/Text.Semibold" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:textSize="15sp" />
        <ImageView android:id="@+id/chev" android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_chevron" app:tint="@color/text_3" />
    </LinearLayout>
    <TextView android:id="@+id/a" style="@style/Text.Body.Secondary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:lineSpacingMultiplier="1.25" android:visibility="gone" />
</LinearLayout>'''

L['activity_chatbot'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:clipToPadding="false" android:paddingTop="8dp" android:paddingBottom="8dp" app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" app:stackFromEnd="true" />
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@color/bg" android:elevation="4dp" android:gravity="center_vertical" android:paddingStart="12dp" android:paddingTop="10dp" android:paddingEnd="12dp" android:paddingBottom="12dp">
        <EditText android:id="@+id/et" style="@style/Input" android:layout_width="0dp" android:layout_weight="1" android:hint="@string/type_question" android:imeOptions="actionSend" android:importantForAutofill="no" android:inputType="text" />
        <FrameLayout android:id="@+id/btnSend" android:layout_width="48dp" android:layout_height="48dp" android:layout_marginStart="8dp" android:background="@drawable/bg_brandmark" android:backgroundTint="@color/accent" android:foreground="?attr/selectableItemBackground">
            <ImageView android:layout_width="24dp" android:layout_height="24dp" android:layout_gravity="center" android:src="@drawable/ic_send" />
        </FrameLayout>
    </LinearLayout>
</LinearLayout>'''

L['item_chat_suggest'] = f'''<TextView {NS} android:id="@+id/text" style="@style/Text" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginStart="16dp" android:layout_marginTop="4dp" android:layout_marginEnd="56dp" android:layout_marginBottom="4dp" android:background="@drawable/bg_card_2_press" android:paddingStart="14dp" android:paddingTop="10dp" android:paddingEnd="14dp" android:paddingBottom="10dp" android:textColor="@color/text_2" />'''

L['fragment_settings'] = f'''<androidx.core.widget.NestedScrollView {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:clipToPadding="false" android:paddingBottom="110dp">
    <LinearLayout android:id="@+id/root" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
        <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
        <LinearLayout android:id="@+id/groups" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" />
        <TextView android:id="@+id/version" style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:gravity="center" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>'''

L['activity_settings'] = f'''<FrameLayout {NS} android:id="@+id/container" android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" />'''

L['activity_feedback'] = f'''<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="match_parent" android:background="@color/bg" android:orientation="vertical">
    <include android:id="@+id/toolbar" layout="@layout/layout_toolbar" />
    <androidx.core.widget.NestedScrollView android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:paddingStart="16dp" android:paddingEnd="16dp" android:paddingBottom="24dp">
            <TextView style="@style/Text.Body.Secondary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="16dp" android:lineSpacingMultiplier="1.2" android:text="@string/feedback_intro" />
            <com.google.android.material.chip.ChipGroup android:id="@+id/types" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="16dp" app:selectionRequired="true" app:singleSelection="true" />
            <TextView style="@style/FieldLabel" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/email_optional" />
            <EditText android:id="@+id/etEmail" style="@style/Input" android:hint="you@email.com" android:importantForAutofill="no" android:inputType="textEmailAddress" />
            <TextView style="@style/FieldLabel" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="16dp" android:text="@string/message" />
            <EditText android:id="@+id/etMsg" style="@style/Input" android:layout_height="140dp" android:gravity="top|start" android:hint="@string/message_hint" android:importantForAutofill="no" android:inputType="textMultiLine|textCapSentences" android:paddingTop="12dp" android:singleLine="false" />
            <com.google.android.material.button.MaterialButton android:id="@+id/btnSend" style="@style/Btn.Primary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="20dp" android:text="@string/send" />
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</LinearLayout>'''

for n, x in L.items():
    io.open(os.path.join(OUT, n + '.xml'), 'w', encoding='utf-8').write(H + x + '\n')
D = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable')
io.open(os.path.join(D, 'bg_snack.xml'), 'w', encoding='utf-8').write(H + '<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="@color/snack_bg" /><corners android:radius="12dp" /></shape>\n')
print(len(L), 'layouts')
