"""Liquid Glass SVG-filter shader generator for Paper.

Every glass layer is one SVG node: <defs> holds clip paths + the filter graph,
an <image> of the backdrop (same URL, offset so it lines up with the canvas)
is refracted through the glass shape, then rim light, hairline and shadow.
"""
import sys, json

def clamp(v, lo, hi): return max(lo, min(hi, v))
def f(v): return ('%.2f' % v).rstrip('0').rstrip('.')

PRESETS = {
  # frost = background blur, sat = vibrancy, tint = lift toward white (0..1)
  'regular': dict(frost=3.0, sat=1.6, tint=0.14, rimTL=2.6, rimBR=1.5, rimU=0.45),
  'clear':   dict(frost=0.6, sat=1.3, tint=0.03, rimTL=2.8, rimBR=1.6, rimU=0.5),
  'ui-light':dict(frost=2.4, sat=1.5, tint=0.16, rimTL=2.2, rimBR=1.3, rimU=0.4),
}

def shape_el(s, grow=0):
    if s['type'] == 'circle':
        return '<circle cx="%s" cy="%s" r="%s"/>' % (f(s['cx']), f(s['cy']), f(s['r'] + grow))
    return '<rect x="%s" y="%s" width="%s" height="%s" rx="%s"/>' % (
        f(s['x'] - grow), f(s['y'] - grow), f(s['w'] + 2*grow), f(s['h'] + 2*grow), f(s['r'] + grow))

def shape_fill(s, attrs):
    if s['type'] == 'circle':
        return '<circle cx="%s" cy="%s" r="%s" %s/>' % (f(s['cx']), f(s['cy']), f(s['r']), attrs)
    return '<rect x="%s" y="%s" width="%s" height="%s" rx="%s" %s/>' % (f(s['x']), f(s['y']), f(s['w']), f(s['h']), f(s['r']), attrs)

def min_dim(s): return 2*s['r'] if s['type'] == 'circle' else min(s['w'], s['h'])

def glass_svg(name, url, bg_w, bg_h, box, shapes, preset='regular', pad=24,
              refraction=1.0, dispersion=0.05, bezel=None, shadow=(0.16, 10, 6),
              hairline='rgba(255,255,255,0.28)', extra='', under='', src_extra=''):
    """box = (x, y, w, h) of the glass bounds in canvas coords; shapes in the same canvas coords."""
    P = dict(PRESETS[preset])
    bx, by, bw, bh = box
    ox, oy = bx - pad, by - pad            # svg origin on canvas
    W, H = bw + 2*pad, bh + 2*pad
    loc = []
    for s in shapes:
        s = dict(s)
        if s['type'] == 'circle': s['cx'] -= ox; s['cy'] -= oy
        else: s['x'] -= ox; s['y'] -= oy
        loc.append(s)
    md = min(min_dim(s) for s in loc)
    sb = bezel if bezel else clamp(0.11 * md, 4, 12)          # bezel softness (px)
    d = max(1.0, sb / 4.0)                                    # gradient tap distance
    K = 3.2
    sc = clamp(0.38 * md, 16, 52) * refraction                # refraction strength (px)
    sR, sG, sB = sc * (1 - dispersion), sc, sc * (1 + 1.4 * dispersion)
    m = 6
    ss = clamp(0.02 * md, 1.2, 2.4)                            # specular rim softness
    t = P['tint']
    clip = ''.join(shape_el(s) for s in loc)
    clipo = ''.join(shape_el(s, m) for s in loc)
    so, sbl, sdy = shadow
    shadows = ''.join(shape_fill(dict(s, **({'cy': s['cy'] + sdy} if s['type']=='circle' else {'y': s['y'] + sdy})),
                      'fill="#000000" fill-opacity="%s" filter="url(#lgShadow)"' % f(so)) for s in loc) if so > 0 else ''
    hair = ''.join(shape_fill(s, 'fill="none" stroke="%s" stroke-width="1"' % hairline) for s in loc) if hairline else ''
    cm = lambda vals: ' '.join(vals.split())
    filt = f'''<filter id="lgFilter" x="0" y="0" width="{f(W)}" height="{f(H)}" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
<feMorphology in="SourceAlpha" operator="erode" radius="{m}" result="shape"/>
<feGaussianBlur in="shape" stdDeviation="{f(sb)}" result="hb"/>
<feColorMatrix in="hb" type="matrix" values="0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1" result="hR"/>
<feColorMatrix in="hb" type="matrix" values="0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 1" result="hG"/>
<feOffset in="hR" dx="-{f(d)}" result="xa"/>
<feOffset in="hG" dx="{f(d)}" result="xb"/>
<feComposite in="xa" in2="xb" operator="arithmetic" k2="1" k3="1" result="xab"/>
<feColorMatrix in="xab" type="matrix" values="{f(K)} -{f(K)} 0 0 0.5 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1" result="gx"/>
<feOffset in="hR" dy="-{f(d)}" result="ya"/>
<feOffset in="hG" dy="{f(d)}" result="yb"/>
<feComposite in="ya" in2="yb" operator="arithmetic" k2="1" k3="1" result="yab"/>
<feColorMatrix in="yab" type="matrix" values="0 0 0 0 0 {f(K)} -{f(K)} 0 0 0.5 0 0 0 0 0 0 0 0 0 1" result="gy"/>
<feComposite in="gx" in2="gy" operator="arithmetic" k2="1" k3="1" result="dmap"/>
<feDisplacementMap in="SourceGraphic" in2="dmap" scale="{f(sR)}" xChannelSelector="R" yChannelSelector="G" result="dR"/>
<feDisplacementMap in="SourceGraphic" in2="dmap" scale="{f(sG)}" xChannelSelector="R" yChannelSelector="G" result="dG"/>
<feDisplacementMap in="SourceGraphic" in2="dmap" scale="{f(sB)}" xChannelSelector="R" yChannelSelector="G" result="dB"/>
<feColorMatrix in="dR" type="matrix" values="1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0" result="cR"/>
<feColorMatrix in="dG" type="matrix" values="0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 1 0" result="cG"/>
<feColorMatrix in="dB" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 1 0" result="cB"/>
<feComposite in="cR" in2="cG" operator="arithmetic" k2="1" k3="1" result="cRG"/>
<feComposite in="cRG" in2="cB" operator="arithmetic" k2="1" k3="1" result="refr"/>
<feGaussianBlur in="refr" stdDeviation="{f(P['frost'])}" result="frost"/>
<feColorMatrix in="frost" type="saturate" values="{f(P['sat'])}" result="vib"/>
<feComponentTransfer in="vib" result="tint"><feFuncR type="linear" slope="{f(1-t)}" intercept="{f(t)}"/><feFuncG type="linear" slope="{f(1-t)}" intercept="{f(t)}"/><feFuncB type="linear" slope="{f(1-t)}" intercept="{f(t)}"/></feComponentTransfer>
<feGaussianBlur in="shape" stdDeviation="{f(ss)}" result="hs"/>
<feComponentTransfer in="hs" result="rim"><feFuncR type="linear" slope="0" intercept="1"/><feFuncG type="linear" slope="0" intercept="1"/><feFuncB type="linear" slope="0" intercept="1"/><feFuncA type="table" tableValues="0 0 0 0 0 {f(P['rimU'])} {f(P['rimU']*0.66)} {f(P['rimU']*0.36)} {f(P['rimU']*0.16)} {f(P['rimU']*0.05)} 0"/></feComponentTransfer>
<feColorMatrix in="hs" type="matrix" values="0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1" result="sR"/>
<feColorMatrix in="hs" type="matrix" values="0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 1" result="sG"/>
<feOffset in="sR" dx="-1" dy="-1" result="sa"/>
<feOffset in="sG" dx="1" dy="1" result="sb"/>
<feComposite in="sa" in2="sb" operator="arithmetic" k2="1" k3="1" result="sab"/>
<feColorMatrix in="sab" type="matrix" values="0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 {f(P['rimTL'])} -{f(P['rimTL'])} 0 0 0" result="hiTL"/>
<feColorMatrix in="sab" type="matrix" values="0 0 0 0 1 0 0 0 0 1 0 0 0 0 1 -{f(P['rimBR'])} {f(P['rimBR'])} 0 0 0" result="hiBR"/>
<feComposite in="hiTL" in2="hiBR" operator="arithmetic" k2="1" k3="1" result="hiDir"/>
<feComposite in="hiDir" in2="rim" operator="arithmetic" k2="1" k3="1" result="hi"/>
<feComposite in="hi" in2="tint" operator="over" result="lit"/>
<feComposite in="lit" in2="shape" operator="in"/>
</filter>'''
    img = f'<image href="{url}" x="{f(-ox)}" y="{f(-oy)}" width="{f(bg_w)}" height="{f(bg_h)}" preserveAspectRatio="none"'
    if src_extra:
        source = f'<g clip-path="url(#lgClipOuter)">{img}/>{src_extra}</g>'
    else:
        source = f'{img} clip-path="url(#lgClipOuter)"/>'
    svg = f'''<svg layer-name="{name}" width="{f(W)}" height="{f(H)}" viewBox="0 0 {f(W)} {f(H)}" style="position:absolute; left:{f(ox)}px; top:{f(oy)}px;" xmlns="http://www.w3.org/2000/svg">
<defs>
<clipPath id="lgClip">{clip}</clipPath>
<clipPath id="lgClipOuter">{clipo}</clipPath>
<filter id="lgShadow" x="0" y="0" width="{f(W)}" height="{f(H)}" filterUnits="userSpaceOnUse"><feGaussianBlur stdDeviation="{f(sbl)}"/></filter>
{filt}
</defs>
{under}{shadows}
<g clip-path="url(#lgClip)">
<g filter="url(#lgFilter)">
{source}
</g>
</g>
{hair}{extra}
</svg>'''
    meta = dict(name=name, preset=preset, bezel=round(sb,2), refraction_px=round(sc,1), dispersion=dispersion, frost=P['frost'], sat=P['sat'], tint=t)
    return svg, meta

if __name__ == '__main__':
    spec = json.loads(sys.argv[1])
    svg, meta = glass_svg(**spec)
    print(svg)
    print(json.dumps(meta), file=sys.stderr)
