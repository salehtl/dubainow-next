# Runbook: migrate DubaiNow Next Figma components into Paper

You are migrating master components from the DubaiNow Next Figma library into the Paper file
"DubaiNow Next". The pipeline below is proven (page "11 Text inputs" → artboard "Text field" matches
Figma pixel-for-pixel). Follow it exactly. Work only on the pages you are assigned.

## Fixed facts
- Figma fileKey: `Jz8G3W1He04wHcyMoooxdM` (READ-ONLY. Never create, edit, rename or delete anything in Figma.
  Only run read-only use_figma scripts and get_screenshot.)
- Paper fileId: `01M3VK547PT2KNWSB1WPYWFJHJ` (always pass fileId explicitly). Other sessions also edit this
  Paper file: never touch pages, artboards or tokens you did not create, except the page you are told to add to.
- Design tokens already exist in Paper for every Figma variable (Light/English names; `-dark`, `-ar`,
  `-reduced` suffixes for other modes). Do not create, edit or delete tokens. If HTML references a token that
  does not exist, report it; do not invent one.
- Serializer: `/Users/saleh/Developer/Claude Workspace/DubaiNow Next/figma-to-paper/f2p.js`
  It runs inside Figma (use_figma) and returns Paper-ready HTML with token `var(--…)` references.

## Setup (once)
1. Load tools with ToolSearch (one call): `select:mcp__4cb26bfc-b897-46ef-9a45-a97b5e089689__use_figma,mcp__4cb26bfc-b897-46ef-9a45-a97b5e089689__get_screenshot,mcp__Paper__get_guide,mcp__Paper__create_page,mcp__Paper__create_artboard,mcp__Paper__write_html,mcp__Paper__update_styles,mcp__Paper__get_screenshot,mcp__Paper__get_children,mcp__Paper__move_nodes,mcp__Paper__delete_nodes,mcp__Paper__finish_working_on_nodes,mcp__Paper__get_basic_info,mcp__Paper__rename_nodes`
2. Invoke the Skill `figma:figma-use` (mandatory before use_figma). Pass `skillNames: "figma-use"` on every use_figma call.
3. Call `mcp__Paper__get_guide({topic:"paper-mcp-instructions"})` once. This is a migration: do NOT write a design
   brief, do NOT restyle anything — reproduce Figma faithfully.
4. Read f2p.js with the Read tool so you have its exact text.

## Per Figma page
A. Inventory (read-only use_figma, set the page first):
```js
const page = await figma.getNodeByIdAsync('<FIGMA_PAGE_ID>'); await figma.setCurrentPageAsync(page);
const sets = page.findAllWithCriteria({types:['COMPONENT_SET','COMPONENT']}).filter(c => c.parent.type !== 'COMPONENT_SET');
return sets.map(s => s.type+'|'+s.id+'|'+s.name+'|'+Math.round(s.width)+'x'+Math.round(s.height)+(s.type==='COMPONENT_SET'?'|kids:'+s.children.map(c=>c.id).join(','):'')).join('\n');
```
B. Paper page: create one with `create_page` using the exact Figma page name (unless told to reuse an existing page id).

## Per COMPONENT_SET
1. `create_artboard` on your Paper page: name = Figma set name; styles
   `{"width":"<W>px","height":"<H>px","backgroundColor":"var(--color-surface-canvas)","position":"relative"}`
   (W×H = the set's size). create_artboard ignores left/top, so afterwards place it with
   `update_styles` `{left, top}`: stack artboards in one column at left 0, top = previous top + previous height + 160.
2. Serialize: run use_figma with the exact f2p.js text, replacing `__PAGE__` with the Figma page id and
   `__IDS__` with a JSON array of ALL variant ids of the set (in order), and leaving `mode:'abs'`.
   The result's first line is `DONE` or `NEXT=<k>`. Everything after that line is HTML.
   If `NEXT=k`, call again with `IDS.slice(k)` until `DONE`.
3. For each chunk, call `write_html` (mode insert-children, targetNodeId = the artboard) with the HTML
   copied EXACTLY (character for character — SVG path data especially). One write_html at a time; never send
   two write_html calls in parallel (Paper silently drops one).

4. CONCURRENCY CHECK (mandatory — several agents write to this Paper file at the same time, and Paper can
   silently drop or overwrite a write when calls overlap): immediately after each write_html, call
   `get_children` on the target node and confirm every top-level node you just wrote is present (match by
   layer name). If any is missing, write that chunk again (only the missing items) and re-check. Also
   re-check the whole artboard's child count right before your final screenshot.

### If the serializer returns `TOOBIG=<id>`
One node's HTML is over the 19 KB output budget. Split it:
1. Serialize just that id with `shallow:true` (same mode as before) → an empty container with its own
   position/layout/fill. Write it. Note the Paper id of the created container (first created node).
2. Get that node's visible children ids (small read-only use_figma script), then serialize them with
   `mode:'child'` and `shallow:false`, writing into the container (targetNodeId = container id).
   Skip a first child that is a full-size background surface with no children (the container already
   carries its fill/border). Recurse the same way if a child is also TOOBIG.

## Per standalone COMPONENT (not inside a set)
- Artboard: width W+80, height H+80, styles `{"backgroundColor":"var(--color-surface-canvas)","display":"flex","flexDirection":"column","padding":"40px"}`.
- Serialize with `mode:'root'` and `IDS=['<componentId>']`; write the HTML into the artboard.

## Per documentation FRAME (foundations pages)
- Artboard: name = frame name, width = frame width, height = frame height, styles
  `{"backgroundColor":"var(--color-surface-canvas)","display":"flex","flexDirection":"column"}`.
- Serialize the frame with `mode:'root'`, `shallow:true` → its container (padding, gap, fill). Write it into the artboard.
- Serialize the frame's direct children with `mode:'child'` (IDS = child ids in order) and write them into
  that container, chunk by chunk (handle NEXT / TOOBIG as above).
- When done, set the artboard `height: "fit-content"`.

## Verify every artboard (mandatory)
- `mcp__Paper__get_screenshot` of the artboard, and Figma `get_screenshot` of the set/component
  (`maxDimension` 1600, `enableBase64Response: true`). Compare carefully: missing text, wrong layer order,
  clipped content, wrong colours, broken icons.
- Known Paper quirk: Paper sometimes reorders absolutely-positioned children so a background/surface layer
  ends up on top and hides text. If text or icons are missing, use `get_children` on the parent and
  `move_nodes` to put the background layer at index 0.
- Text that wraps differently by a word is acceptable. Fix real defects with targeted `update_styles`,
  `move_nodes` or `delete_nodes`; never delete and rebuild a whole artboard unless it is unrecoverable.
- If the artboard needs more height for content, set `height: "fit-content"` is NOT appropriate here
  (artboard mirrors the set); keep the set size.

## Finish
- `finish_working_on_nodes` with the ids of all artboards you created.
- Final report (concise): for each Figma component → Paper page name/id, artboard name/id, variant count,
  verification verdict (match / minor diffs listed / problems), plus any serializer errors or missing tokens.
