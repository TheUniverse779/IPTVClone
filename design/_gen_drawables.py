"""Writes the shape/selector drawables that mirror design/components.css."""
import io, os

OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable')
NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'
HEAD = '<?xml version="1.0" encoding="utf-8"?>\n'

def shape(solid=None, radius=None, stroke=None, oval=False, corners=None, size=None, extra=''):
    s = f'<shape {NS}' + (' android:shape="oval"' if oval else '') + '>'
    if solid: s += f'<solid android:color="{solid}" />'
    if radius: s += f'<corners android:radius="{radius}" />'
    if corners: s += f'<corners {corners} />'
    if stroke: s += f'<stroke {stroke} />'
    if size: s += f'<size {size} />'
    return s + extra + '</shape>'

def inner(xml):  # shape without namespace, for nesting
    return xml.replace(f' {NS}', '')

def ripple(color, content='', mask=None):
    body = content
    if mask: body += f'<item android:id="@android:id/mask">{mask}</item>'
    return f'<ripple {NS} android:color="{color}">{body}</ripple>'

D = {}
_focus = inner(shape('@color/surface_2', '@dimen/r_ctl', stroke='android:width="1.5dp" android:color="@color/accent"'))
_idle = inner(shape('@color/surface_2', '@dimen/r_ctl'))
D['bg_input'] = f'<selector {NS}><item android:state_focused="true">{_focus}</item><item>{_idle}</item></selector>'
D['bg_input_error'] = shape('@color/surface_2', '@dimen/r_ctl', stroke='android:width="1.5dp" android:color="@color/live"')
D['bg_card'] = shape('@color/surface', '@dimen/r_card')
D['bg_card_2'] = shape('@color/surface_2', '@dimen/r_card')
D['bg_card_press'] = ripple('@color/surface_3', f'<item>{inner(shape("@color/surface", "@dimen/r_card"))}</item>')
D['bg_card_2_press'] = ripple('@color/surface_3', f'<item>{inner(shape("@color/surface_2", "@dimen/r_card"))}</item>')
D['bg_row'] = ripple('@color/surface_2', mask='<color android:color="@color/white" />')
D['bg_row_current'] = ripple('@color/surface_2', '<item><color android:color="@color/accent_soft" /></item>')
D['bg_icon_btn'] = ripple('@color/surface_3', mask=inner(shape('@color/white', oval=True)))
D['bg_icon_btn_boxed'] = ripple('@color/surface_3', f'<item>{inner(shape("@color/surface_2", "@dimen/r_ctl"))}</item>')
D['bg_ic_wrap'] = shape('@color/surface_2', '12dp')
D['bg_ic_wrap_3'] = shape('@color/surface_3', '12dp')
D['bg_accent_soft'] = ripple('@color/accent_soft', f'<item>{inner(shape("@color/accent_soft", "14dp"))}</item>')
D['bg_hero_icon'] = shape('@color/accent_soft', '16dp')
D['bg_hero_icon_danger'] = shape('@color/danger_bg', '16dp')
D['bg_hero_icon_ok'] = shape('@color/ok_soft', '16dp')
D['bg_dialog'] = shape('@color/surface', '20dp')
D['bg_popup'] = shape('@color/surface_2', '14dp')
D['bg_grab'] = shape('@color/line', '2dp')
D['bg_seg'] = shape('@color/surface', '14dp')
D['bg_seg_item'] = (f'<selector {NS}><item android:state_selected="true">{inner(shape("@color/accent", "10dp"))}</item>'
                    f'<item>{inner(shape("@android:color/transparent", "10dp"))}</item></selector>')
D['bg_bottombar'] = shape('@color/surface', corners='android:topLeftRadius="22dp" android:topRightRadius="22dp"')
D['bg_fab'] = (f'<layer-list {NS}><item>{inner(shape("@color/bg", oval=True))}</item>'
               f'<item android:bottom="6dp" android:left="6dp" android:right="6dp" android:top="6dp">'
               f'<ripple android:color="@color/accent_press"><item>{inner(shape("@color/accent", oval=True))}</item></ripple></item></layer-list>')
D['bg_logo'] = shape('@color/surface_3', '10dp')
D['bg_live'] = shape('@color/live', '5dp')
D['bg_dot_white'] = shape('@color/white', oval=True, size='android:width="6dp" android:height="6dp"')
D['bg_status_ok'] = shape('@color/ok_soft', '12dp')
D['bg_status_bad'] = shape('@color/danger_bg', '12dp')
D['bg_tag_new'] = shape('@color/accent', '11dp', stroke='android:width="3dp" android:color="@color/bg"')
D['bg_tag_exp'] = shape('@color/danger_bg', '11dp', stroke='android:width="3dp" android:color="@color/bg"')
D['bg_badge_round'] = shape('@color/surface_3', oval=True, stroke='android:width="3dp" android:color="@color/bg"')
D['bg_badge_round_2'] = shape('@color/surface_2', oval=True, stroke='android:width="3dp" android:color="@color/bg"')
D['bg_profile_add'] = shape('@android:color/transparent', '24dp', stroke='android:width="2dp" android:color="@color/line" android:dashWidth="6dp" android:dashGap="5dp"')
D['bg_profile_new_ring'] = shape('@android:color/transparent', '28dp', stroke='android:width="2dp" android:color="@color/accent"')
D['bg_dashed_card'] = shape('@android:color/transparent', '@dimen/r_card', stroke='android:width="1.5dp" android:color="@color/line" android:dashWidth="6dp" android:dashGap="5dp"')
D['bg_warn_bar'] = shape('@color/danger_bg', '12dp')
D['bg_splash'] = (f'<layer-list {NS}><item><shape><gradient android:type="radial" android:centerY="0.38" '
                  f'android:gradientRadius="420dp" android:startColor="#2A1A10" android:endColor="@color/bg" /></shape></item></layer-list>')
D['bg_brandmark'] = shape('@color/accent', '28dp')
D['bg_player_shade'] = (f'<shape {NS}><gradient android:angle="270" android:startColor="#99000000" '
                        f'android:centerColor="#00000000" android:endColor="#B3000000" /></shape>')
D['bg_round_dark'] = shape('#59000000', oval=True)
D['bg_pill_dark'] = shape('#A6000000', '999dp')
D['bg_side_panel'] = shape('#F5151419')
D['bg_bubble_bot'] = shape('@color/surface', corners='android:topLeftRadius="18dp" android:topRightRadius="18dp" android:bottomRightRadius="18dp" android:bottomLeftRadius="6dp"')
D['bg_bubble_user'] = shape('@color/accent', corners='android:topLeftRadius="18dp" android:topRightRadius="18dp" android:bottomLeftRadius="18dp" android:bottomRightRadius="6dp"')
D['bg_progress'] = (f'<layer-list {NS}><item android:id="@android:id/background">{inner(shape("#2EFFFFFF", "2dp"))}</item>'
                    f'<item android:id="@android:id/progress"><clip>{inner(shape("@color/accent", "2dp"))}</clip></item></layer-list>')
D['cursor'] = shape('@color/accent', size='android:width="2dp"')
D['bg_paste_chip'] = ripple('@color/line', f'<item>{inner(shape("@color/surface_3", "8dp"))}</item>')
D['bg_date_item'] = (f'<selector {NS}><item android:state_selected="true">{inner(shape("@color/accent", "14dp"))}</item>'
                     f'<item>{inner(shape("@color/surface", "14dp"))}</item></selector>')
D['bg_live_cat'] = (f'<selector {NS}><item android:state_selected="true"><layer-list><item><color android:color="@color/bg" /></item>'
                    f'<item android:gravity="start|fill_vertical" android:width="3dp"><color android:color="@color/accent" /></item></layer-list></item>'
                    f'<item><color android:color="@color/surface" /></item></selector>')
D['bg_list_card'] = shape('@color/surface', '@dimen/r_card')
D['bg_poster_rate'] = shape('#8C000000', '6dp')
D['bg_ep_num'] = shape('#8C000000', '6dp')
D['bg_crest'] = shape('@color/surface_3', oval=True)
D['bg_avatar'] = shape('@color/surface_3', '24dp')
D['bg_ok_dot'] = shape('@color/ok', oval=True, stroke='android:width="3dp" android:color="@color/surface"')
D['divider_inset'] = f'<inset {NS} android:insetLeft="16dp"><shape><solid android:color="@color/surface_2" /><size android:height="1dp" /></shape></inset>'

os.makedirs(OUT, exist_ok=True)
for name, xml in D.items():
    io.open(os.path.join(OUT, name + '.xml'), 'w', encoding='utf-8').write(HEAD + xml + '\n')
print(len(D), 'drawables')
