"""Import forms (form_import_url / _xtream / _single) and the labelled-input building block."""
import io, os
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'layout')
H = '<?xml version="1.0" encoding="utf-8"?>\n'
NS = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools"'

def field(pid, label, hint='', icon=None, paste=False, required=True, helper=None, password=False, input_type=None):
    ic = f'<ImageView android:layout_width="20dp" android:layout_height="20dp" android:layout_marginEnd="8dp" android:src="@drawable/{icon}" app:tint="@color/text_2" />' if icon else ''
    pst = f'<TextView android:id="@+id/{pid}Paste" android:layout_width="wrap_content" android:layout_height="28dp" android:layout_marginStart="8dp" android:background="@drawable/bg_paste_chip" android:fontFamily="@font/barlow_semibold" android:gravity="center" android:paddingStart="10dp" android:paddingEnd="10dp" android:text="@string/paste" android:textColor="@color/text" android:textSize="12sp" />' if paste else ''
    eye = f'<ImageButton android:id="@+id/{pid}Eye" android:layout_width="32dp" android:layout_height="32dp" android:background="?attr/selectableItemBackgroundBorderless" android:src="@drawable/ic_eye" app:tint="@color/text_2" />' if password else ''
    itype = input_type or ('textPassword' if password else 'text')
    req = '<TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginStart="3dp" android:text="@string/required_mark" android:textColor="@color/accent" />' if required else ''
    hlp = f'<TextView android:id="@+id/{pid}Helper" style="@style/FieldHelper" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="{helper}" />' if helper else f'<TextView android:id="@+id/{pid}Helper" style="@style/FieldHelper" android:layout_width="match_parent" android:layout_height="wrap_content" android:visibility="gone" />'
    return f'''
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="16dp" android:orientation="vertical">
        <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginBottom="6dp">
            <TextView style="@style/Text.Label" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="{label}" />{req}
        </LinearLayout>
        <LinearLayout android:id="@+id/{pid}Box" android:layout_width="match_parent" android:layout_height="@dimen/h_input" android:background="@drawable/bg_input" android:gravity="center_vertical" android:orientation="horizontal" android:paddingStart="14dp" android:paddingEnd="10dp">
            {ic}
            <EditText android:id="@+id/{pid}" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:background="@null" android:fontFamily="@font/barlow" android:hint="{hint}" android:importantForAutofill="no" android:inputType="{itype}" android:singleLine="true" android:textColor="@color/text" android:textColorHint="@color/text_3" android:textCursorDrawable="@drawable/cursor" android:textSize="14sp" />
            {pst}{eye}
        </LinearLayout>
        {hlp}
    </LinearLayout>'''

def switch_row(pid, title, sub=None):
    s = f'<TextView style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="2dp" android:text="{sub}" />' if sub else ''
    return f'''
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical" android:minHeight="52dp">
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
            <TextView style="@style/Text" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="{title}" />{s}
        </LinearLayout>
        <com.google.android.material.materialswitch.MaterialSwitch android:id="@+id/{pid}" style="@style/Switch" android:layout_width="wrap_content" android:layout_height="wrap_content" />
    </LinearLayout>'''

def guide(pid):
    return f'<include android:id="@+id/{pid}" layout="@layout/layout_guide_link" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="14dp" />'

def wrap(body):
    return f'<LinearLayout {NS} android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">{body}\n</LinearLayout>'

L = {}
L['form_import_url'] = wrap(
    field('etUrl', '@string/playlist_url', '@string/playlist_url_hint', 'ic_link', paste=True, helper='@string/playlist_url_helper', input_type='textUri|textNoSuggestions')
    + field('etName', '@string/playlist_name', required=False)
    + switch_row('swLock', '@string/lock_with_passcode', '@string/lock_with_passcode_sub')
    + switch_row('swAuto', '@string/auto_update', '@string/auto_update_sub')
    + guide('guideUrl')
    + '''
    <com.google.android.material.button.MaterialButton android:id="@+id/btnAdd" style="@style/Btn.Primary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:text="@string/add_playlist" />
    <TextView android:id="@+id/tvLicense" style="@style/Text.Hint" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="10dp" android:gravity="center" />''')

L['form_import_xtream'] = wrap(
    field('etProfileName', '@string/profile_name', required=False)
    + field('etServer', '@string/server_url', '@string/server_hint', 'ic_globe', paste=True, helper='@string/server_helper', input_type='textUri|textNoSuggestions')
    + field('etUser', '@string/username', icon='ic_user')
    + field('etPass', '@string/password', icon='ic_lock', password=True)
    + switch_row('swLockProfile', '@string/lock_profile', '@string/lock_profile_sub')
    + '''
    <LinearLayout android:id="@+id/accountCard" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:background="@drawable/bg_card_press" android:gravity="center_vertical" android:padding="16dp" android:visibility="gone">
        <ImageView android:layout_width="24dp" android:layout_height="24dp" android:src="@drawable/ic_info" />
        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_marginStart="12dp" android:layout_weight="1" android:orientation="vertical">
            <TextView style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/account_info" />
            <TextView android:id="@+id/accountSub" style="@style/Text.Hint" android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>
        <TextView android:id="@+id/accountStatus" style="@style/Text.Semibold" android:layout_width="wrap_content" android:layout_height="24dp" android:gravity="center" android:paddingStart="10dp" android:paddingEnd="10dp" android:textSize="12sp" />
    </LinearLayout>'''
    + guide('communityPick') + guide('guideXtream')
    + '''
    <com.google.android.material.button.MaterialButton android:id="@+id/btnLogin" style="@style/Btn.Primary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:text="@string/login_sync" />
    <com.google.android.material.button.MaterialButton android:id="@+id/btnDelete" style="@style/Btn.Text" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:text="@string/delete_profile" android:textColor="@color/danger_text" android:visibility="gone" />''')

L['form_import_single'] = wrap(
    field('etStream', '@string/stream_link', '@string/stream_link_hint', 'ic_play_circle', paste=True, helper='@string/stream_link_helper', input_type='textUri|textNoSuggestions')
    + field('etStreamName', '@string/stream_name_optional', '@string/stream_name_hint', required=False)
    + switch_row('swSave', '@string/save_to_list', '@string/save_to_list_sub')
    + guide('guideSingle')
    + '''
    <com.google.android.material.button.MaterialButton android:id="@+id/btnPlay" style="@style/Btn.Primary" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="24dp" android:text="@string/play_now" app:icon="@drawable/ic_play" />''')

for n, x in L.items():
    io.open(os.path.join(OUT, n + '.xml'), 'w', encoding='utf-8').write(H + x + '\n')
print(len(L), 'forms')
