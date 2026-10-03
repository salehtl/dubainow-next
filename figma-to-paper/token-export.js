// Figma use_figma script: export DubaiNow Next variables as Paper tokens.
// Set CHUNK to 'Primitives' | 'Color' | 'Layout' | 'Typography' | 'Material' | 'Motion'.
const CHUNK = '__CHUNK__';
const cols = await figma.variables.getLocalVariableCollectionsAsync();
const colById = Object.fromEntries(cols.map(c => [c.id, c]));
const WEIGHT = { 'Thin': 100, 'Extra Light': 200, 'ExtraLight': 200, 'Light': 300, 'Regular': 400, 'Medium': 500, 'Semi Bold': 600, 'SemiBold': 600, 'Bold': 700, 'Extra Bold': 800, 'ExtraBold': 800, 'Black': 900 };
const r2 = n => Math.round(n * 1000) / 1000;
function colorCss(c) {
  const h = x => Math.round(x * 255).toString(16).padStart(2, '0');
  if (c.a === undefined || c.a >= 0.999) return '#' + h(c.r) + h(c.g) + h(c.b);
  return `rgba(${Math.round(c.r * 255)}, ${Math.round(c.g * 255)}, ${Math.round(c.b * 255)}, ${r2(c.a)})`;
}
// Variable -> Paper token name. mode: 'Dark' adds -dark, 'Arabic' adds -ar, 'Reduced transparency' adds -reduced.
function tokenName(v, modeName) {
  const col = colById[v.variableCollectionId].name;
  const p = v.name.split('/');
  const sfx = modeName === 'Dark' ? '-dark' : modeName === 'Arabic' ? '-ar' : modeName === 'Reduced transparency' ? '-reduced' : modeName === 'Reduced' ? '-reduced' : '';
  if (col === 'Primitives') return v.resolvedType === 'COLOR' ? '--color-' + p.slice(1).join('-') : null;
  if (col === 'Color') return v.resolvedType === 'COLOR' ? '--color-' + p.join('-') + sfx : null;
  if (col === 'Layout') {
    if (v.resolvedType !== 'FLOAT') return null;
    const g = p[0], rest = p.slice(1).join('-');
    if (g === 'space') return '--spacing-' + rest;
    if (g === 'radius') return '--radius-' + rest;
    return '--spacing-' + p.join('-');
  }
  if (col === 'Typography') {
    const prop = p[p.length - 1], role = p.slice(0, -1).join('-');
    if (prop === 'family') return '--font-' + role + sfx;
    if (prop === 'style') return '--font-weight-' + role + sfx;
    if (prop === 'size') return '--text-' + role + sfx;
    if (prop === 'line-height') return '--leading-' + role + sfx;
    if (prop === 'tracking') return '--tracking-' + role + sfx;
    return null;
  }
  if (col === 'Material' || col === 'Motion') {
    if (v.resolvedType === 'COLOR') return '--color-' + p.join('-') + sfx;
    if (v.resolvedType === 'FLOAT') return '--' + col.toLowerCase() + '-' + p.join('-') + sfx;
    return null;
  }
  return null;
}
function tokenType(v) {
  const col = colById[v.variableCollectionId].name;
  if (v.resolvedType === 'COLOR') return 'color';
  if (col === 'Layout') return v.name.startsWith('radius/') ? 'radius' : 'spacing';
  if (col === 'Typography') {
    const prop = v.name.split('/').pop();
    return { family: 'fontFamily', style: 'fontWeight', size: 'fontSize', 'line-height': 'lineHeight', tracking: 'letterSpacing' }[prop];
  }
  return null;
}
async function value(v, modeId, modeName) {
  let val = v.valuesByMode[modeId];
  if (val && val.type === 'VARIABLE_ALIAS') {
    const t = await figma.variables.getVariableByIdAsync(val.id);
    const tcol = colById[t.variableCollectionId];
    // alias to a mode-less collection keeps no suffix; alias within same moded collection keeps the mode
    const tMode = tcol.modes.length > 1 ? (tcol.modes.find(m => m.name === modeName) || tcol.modes[0]).name : '';
    const tn = tokenName(t, tMode);
    if (tn) return 'var(' + tn + ')';
    // alias to a non-exported primitive (number/string): resolve the literal
    const tm = tcol.modes.length > 1 ? (tcol.modes.find(m => m.name === modeName) || tcol.modes[0]).modeId : tcol.defaultModeId;
    return value(t, tm, modeName);
  }
  const type = tokenType(v);
  if (type === 'color') return colorCss(val);
  if (type === 'fontWeight') return WEIGHT[val] || 400;
  if (type === 'fontFamily') return val;
  if (typeof val === 'number') return r2(val) + 'px';
  return val;
}
const out = [];
for (const c of cols.filter(c => c.name === CHUNK)) {
  for (const id of c.variableIds) {
    const v = await figma.variables.getVariableByIdAsync(id);
    const type = tokenType(v);
    if (!type) { out.push(['skip', v.name, v.resolvedType]); continue; }
    for (const m of c.modes) {
      const name = tokenName(v, c.modes.length > 1 ? m.name : '');
      if (!name) continue;
      out.push([type, name, await value(v, m.modeId, m.name)]);
    }
  }
}
return out;
