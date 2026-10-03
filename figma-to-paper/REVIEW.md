# Review: DubaiNow Next Figma → Paper migration

Purpose: several agents wrote to the same Paper file in parallel, and Paper can silently drop or overwrite
writes that overlap (node ids reused). Prove each assigned Paper page is complete and faithful, and repair
what is not.

## Fixed facts
- Figma fileKey `Jz8G3W1He04wHcyMoooxdM` — READ-ONLY (use_figma read scripts + get_screenshot only).
- Paper fileId `01M3VK547PT2KNWSB1WPYWFJHJ` — always pass fileId. Only touch the pages assigned to you.
- Serializer and runbook (for re-writing anything missing):
  `/Users/saleh/Developer/Claude Workspace/DubaiNow Next/figma-to-paper/f2p.js`
  `/Users/saleh/Developer/Claude Workspace/DubaiNow Next/figma-to-paper/RUNBOOK.md`
- Setup: load tools with ToolSearch in one call (use_figma, Figma get_screenshot, Paper get_basic_info,
  get_children, get_tree_summary, get_screenshot, get_node_info, write_html, update_styles, move_nodes,
  delete_nodes, set_text_content, finish_working_on_nodes, get_guide). Invoke Skill `figma:figma-use` before
  use_figma and pass `skillNames: "figma-use"`. Call Paper get_guide("paper-mcp-instructions") once.

## For each assigned Paper page
1. `get_basic_info` with the pageId → list artboards. Map each artboard to its Figma source by name
   (artboard name = Figma component / component set / frame name).
2. Completeness:
   - Component set: Paper artboard direct children must equal the Figma set's variants (count AND names,
     e.g. "State=Error"). Get Figma names with a read-only script (`set.children.map(c=>c.name)`).
   - Standalone component: exactly one child (the component's root frame).
   - Documentation frame: the container's children must match the Figma frame's visible children (names).
   - Look for strays: duplicated variants, nodes from another component/page, empty frames, orphan nodes on
     the page outside any artboard.
3. Fidelity: Paper `get_screenshot` of each artboard vs Figma `get_screenshot` of the source
   (maxDimension 1600, enableBase64Response true). For tall artboards screenshot sub-sections (children) so
   text is readable. Check: missing/duplicated text, wrong content in a variant, layers hiding others
   (Paper can reorder absolute layers — fix with move_nodes to index 0 for background surfaces), wrong
   colours (should be tokens), broken/stretched icons, clipped content, glass rendered as dark-grey slabs.
   One-word wrap differences are acceptable.
   Known quirks found during migration — check for them explicitly:
   - Small icon-sized frames were sometimes exported as SVG with `<filter>` effects; Paper does not render
     SVG filters, so shadows show as hard dark outlines. Fix by re-serializing that node with the current
     f2p.js (it no longer flattens frames that have effects) and replacing it.
   - Paper sometimes drops `position:absolute` from children of an absolutely positioned frame, so they
     stack below it instead of overlapping. Restore with update_styles (position/left/top from the HTML or Figma x/y).
   - Hard line breaks inside text values can be lost; restore with set_text_content (+ white-space pre-wrap).
   - Fixed-height text that Figma centres vertically renders top-aligned in Paper (e.g. calendar day numbers);
     fix with `height: fit-content` on the text so its auto-layout parent centres it.
   - Apple glass surfaces whose only fills use blend modes may be transparent; give them
     `background-color: var(--color-glass-panel-tint)`. Small glass backings exported as SVG render as dark
     circles; replace with the `--color-glass-*` glass recipe.
   - Empty frames with no fill and no children are dropped by Paper on write — that is expected, not a defect.
   - Large glass capsules (Tab bar, App navigation) may still carry leftover black/white 25% fills on their
     "Fill + Shadow" layer under the glass tint, making them greyer than Figma. Remove those fills
     (background none) so only the `--color-glass-*` glass recipe remains.
   - Scaled instances (e.g. App navigation Search button ≈1.28×) may have token sizes instead of their real
     scaled sizes; compare sizes with Figma and set literal px where they differ.
   - Figma renders slightly more saturated (legacy colour profile) — not a defect if token values match.
   - Double alpha (fixed in f2p.js on 2026-10-01): Figma copies a bound colour variable's alpha into the
     paint opacity, so older output wrapped already-transparent tokens in `color-mix(var(--x) N%, transparent)`.
     If N equals the token's own alpha, replace with plain `var(--x)`.
   - Strokes with `strokesIncludedInLayout` must be real `border`s (fixed in f2p.js), not inset shadows.
   - `align-items` must be explicit (`flex-start` for Figma MIN) or Paper stretches children (fixed in f2p.js).
4. Repair: targeted fixes only (update_styles, move_nodes, set_text_content, delete strays). If a variant is
   missing or corrupted, re-serialize just that variant (RUNBOOK procedure) and write it, then confirm with
   get_children. Never rebuild a whole artboard if a targeted fix works. One write_html at a time and
   verify each write with get_children (other reviewers may be writing to other pages).
5. `finish_working_on_nodes` for every artboard you touched.

## Report (concise)
Per artboard: name, expected vs found children, verdict (OK / fixed: … / still broken: …).
List any strays deleted and anything you could not fix.
