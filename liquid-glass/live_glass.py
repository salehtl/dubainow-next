"""Paper-native Liquid Glass: only CSS that Paper's style model keeps.
Live on any surface: every layer reads the canvas behind it through backdrop-filter.
Layers (all siblings, so each one samples the real backdrop):
  Frost  - tint + blur/saturate/brightness of whatever is behind
  Bezel  - stronger blur + light lift, masked to the rim (stands in for edge lensing)
  Sheen  - diagonal specular wash
  Rim    - inset highlights (top-left strong, bottom-right soft) + hairline
"""
def f(v): return ('%.2f' % v).rstrip('0').rstrip('.')
V = {
  'regular': dict(tint='rgba(255,255,255,0.42)', frost='blur(6px) saturate(180%) brightness(104%)',
                  bezel='blur(11px) saturate(160%) brightness(112%) contrast(104%)',
                  shadow='0px 8px 24px rgba(0,0,0,0.12), 0px 1px 2px rgba(0,0,0,0.08), 0px 0px 0px 0.5px rgba(0,0,0,0.08)',
                  sheen=(0.24, 0.08), rim=(0.9, 0.55, 0.45, 0.12)),
  'clear':   dict(tint='rgba(255,255,255,0.04)', frost='blur(1.5px) saturate(150%) brightness(104%)',
                  bezel='blur(7px) saturate(150%) brightness(122%) contrast(108%)',
                  shadow='0px 10px 28px rgba(0,0,0,0.18), 0px 1px 2px rgba(0,0,0,0.10)',
                  sheen=(0.20, 0.06), rim=(0.95, 0.6, 0.5, 0.10)),
}
def glass(name, x, y, w, h, r, variant='regular', content=''):
    p = V[variant]
    band = max(10, min(26, 0.28 * min(w, h)))
    bt, bs = band / h * 100, band / w * 100
    mask = (f'linear-gradient(to bottom, #000 0%, rgba(0,0,0,0.35) {f(bt*0.45)}%, rgba(0,0,0,0) {f(bt)}%, rgba(0,0,0,0) {f(100-bt)}%, rgba(0,0,0,0.35) {f(100-bt*0.45)}%, #000 100%), '
            f'linear-gradient(to right, #000 0%, rgba(0,0,0,0.35) {f(bs*0.45)}%, rgba(0,0,0,0) {f(bs)}%, rgba(0,0,0,0) {f(100-bs)}%, rgba(0,0,0,0.35) {f(100-bs*0.45)}%, #000 100%)')
    s1, s2 = p['sheen']; a, b, c, d = p['rim']
    L = f'position:absolute; left:0px; top:0px; width:{f(w)}px; height:{f(h)}px; border-radius:{f(r)}px;'
    return (f'<div layer-name="{name}" style="position:absolute; left:{f(x)}px; top:{f(y)}px; width:{f(w)}px; height:{f(h)}px; border-radius:{f(r)}px; box-shadow:{p["shadow"]};">'
            f'<div layer-name="Frost" style="{L} background-color:{p["tint"]}; backdrop-filter:{p["frost"]};"></div>'
            f'<div layer-name="Bezel lens" style="{L} backdrop-filter:{p["bezel"]}; mask-image:{mask};"></div>'
            f'<div layer-name="Sheen" style="{L} background-image:linear-gradient(135deg, rgba(255,255,255,{s1}) 0%, rgba(255,255,255,0) 34%, rgba(255,255,255,0) 66%, rgba(255,255,255,{s2}) 100%);"></div>'
            f'<div layer-name="Rim light" style="{L} box-shadow:inset 1.5px 1.5px 1px -0.5px rgba(255,255,255,{a}), inset -1.5px -1.5px 1px -0.5px rgba(255,255,255,{b}), inset 0px 0px 0px 0.5px rgba(255,255,255,{c}), inset 0px 0px 12px rgba(255,255,255,{d});"></div>'
            f'{content}</div>')
