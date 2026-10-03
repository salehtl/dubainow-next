# Sweep: borders that take up layout space

Problem: some Figma frames have a 1px INSIDE stroke with `strokesIncludedInLayout = true` (the stroke takes
layout space). The migration wrote these as `box-shadow: inset 0 0 0 1px <colour>`, which takes no space, so
those cards/controls are 2px smaller in Paper and some text wraps differently. Fix: replace that inset
shadow with a real border of the same colour.

## Fixed facts
- Paper fileId `01M3VK547PT2KNWSB1WPYWFJHJ` (pass it always). Figma fileKey `Jz8G3W1He04wHcyMoooxdM` is READ-ONLY.
- Setup: one ToolSearch call loading Paper get_children, get_tree_summary, find_nodes, get_computed_styles,
  update_styles, get_screenshot, finish_working_on_nodes, get_guide and Figma use_figma + get_screenshot.
  Invoke Skill `figma:figma-use` before any use_figma (pass `skillNames: "figma-use"`). Call Paper
  get_guide("paper-mcp-instructions") once. Other agents may be working on other pages — stay on yours.

## Procedure per affected node
1. Locate the Paper node: inside the named artboard, the node with the listed layer name (variant frames are
   named exactly like the Figma variant, e.g. "State=Ready"; nested names like "Control" repeat once per
   variant — fix every occurrence in the listed artboard).
2. `get_computed_styles` → read its `boxShadow`. Find the stroke part: `inset 0 0 0 1px <C>` (C is usually
   `var(--color-border-…)`). If unsure, confirm the colour token with a read-only Figma script
   (node.strokes[0].boundVariables.color → variable name; token = `--color-` + name with `/`→`-`).
3. `update_styles`: `border: "1px solid <C>"` and `boxShadow` = the remaining shadows without that part
   (or `"none"` if nothing remains). Batch many nodes into one update_styles call where the values match.
4. Verify by measuring: the node's height in Paper (get_tree_summary or get_node_info) should now equal
   the Figma node height; screenshot the artboard and compare to Figma.
5. `finish_working_on_nodes` for touched artboards.

## Report
Per artboard: nodes fixed (count), before/after size of one sample, verdict.
