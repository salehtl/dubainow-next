// Paper get_jsx (inline-styles) → static HTML fragment.
// Usage: node tools/jsx2html.mjs <in.jsx> <out.html> [tree.txt]
// The optional tree (get_tree_summary, depth 10) is zipped onto the JSX so every
// element carries its Paper layer name (data-n) and node id (data-pid).
// Leaf text elements also get data-t="<their text>" for easy selection.
import { readFileSync, writeFileSync } from "node:fs";
import { parse } from "@babel/parser";

const [, , inPath, outPath, treePath] = process.argv;
const src = readFileSync(inPath, "utf8");
const ast = parse("const __ = " + src.trim() + ";", { plugins: ["jsx"] });
const root = ast.program.body[0].declarations[0].init;

// --- Paper tree summary -------------------------------------------------
function parseTree(text) {
  const lines = text.split("\n").filter(Boolean);
  const stack = [];
  let top = null;
  for (const line of lines) {
    const depth = line.match(/^ */)[0].length / 2;
    const m = line.trim().match(/^(\S+) "((?:[^"\\]|\\.)*)" \(([^)]+)\)/);
    if (!m) continue;
    const node = { type: m[1], name: m[2], id: m[3], children: [] };
    stack.length = depth;
    if (depth === 0) top = node;
    else stack[depth - 1].children.push(node);
    stack[depth] = node;
  }
  return top;
}
const tree = treePath ? parseTree(readFileSync(treePath, "utf8")) : null;

// --- helpers --------------------------------------------------------------
const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const escAttr = (s) => esc(s).replace(/"/g, "&quot;");
const kebab = (k) =>
  k
    .replace(/^Webkit/, "-webkit-")
    .replace(/^Moz/, "-moz-")
    .replace(/^ms([A-Z])/, "-ms-$1")
    .replace(/[A-Z]/g, (c) => "-" + c.toLowerCase())
    .replace(/^-(webkit|moz|ms)--/, "-$1-");

const KEEP_CAMEL = new Set([
  "viewBox", "preserveAspectRatio", "gradientUnits", "gradientTransform", "patternUnits",
  "patternContentUnits", "patternTransform", "maskUnits", "maskContentUnits", "clipPathUnits",
  "stdDeviation", "baseFrequency", "numOctaves", "filterUnits", "primitiveUnits", "markerWidth",
  "markerHeight", "markerUnits", "refX", "refY", "textLength", "lengthAdjust", "spreadMethod",
  "tableValues", "kernelMatrix", "edgeMode", "xChannelSelector", "yChannelSelector",
  "diffuseConstant", "specularConstant", "specularExponent", "surfaceScale", "startOffset",
]);
const attrName = (n) => {
  if (n === "className") return "class";
  if (n === "htmlFor") return "for";
  if (n === "xlinkHref") return "xlink:href";
  if (n === "xmlnsXlink") return "xmlns:xlink";
  if (KEEP_CAMEL.has(n)) return n;
  return kebab(n);
};

function literal(node) {
  switch (node.type) {
    case "StringLiteral": return node.value;
    case "NumericLiteral": return node.value;
    case "TemplateLiteral": return node.quasis.map((q) => q.value.cooked).join("");
    case "UnaryExpression": return node.operator === "-" ? -literal(node.argument) : literal(node.argument);
    default: throw new Error("Unsupported literal " + node.type);
  }
}
function styleToCss(obj) {
  return obj.properties
    .map((p) => {
      const key = p.key.name ?? p.key.value;
      return `${kebab(key)}: ${literal(p.value)}`;
    })
    .join("; ");
}

let named = 0;
let mismatches = [];
function emit(el, tnode, insideSvg) {
  if (el.type === "JSXText") {
    const t = el.value.replace(/\s+/g, " ").trim();
    return t ? esc(t) : "";
  }
  if (el.type === "JSXExpressionContainer") {
    if (el.expression.type === "JSXEmptyExpression") return "";
    return esc(literal(el.expression));
  }
  if (el.type === "JSXFragment") return el.children.map((c) => emit(c, null, insideSvg)).join("");
  const tag = el.openingElement.name.name;
  const isSvg = tag === "svg" || insideSvg;
  const attrs = [];
  for (const a of el.openingElement.attributes) {
    const name = a.name.name;
    if (name === "style") {
      attrs.push(`style="${escAttr(styleToCss(a.value.expression))}"`);
    } else if (a.value == null) {
      attrs.push(attrName(name));
    } else {
      const v = a.value.type === "StringLiteral" ? a.value.value : literal(a.value.expression);
      attrs.push(`${attrName(name)}="${escAttr(v)}"`);
    }
  }
  const elementKids = el.children.filter((c) => c.type === "JSXElement");
  const textKids = el.children
    .filter((c) => c.type === "JSXText")
    .map((c) => c.value.replace(/\s+/g, " ").trim())
    .filter(Boolean);
  if (tnode && !insideSvg) {
    attrs.push(`data-n="${escAttr(tnode.name)}"`, `data-pid="${escAttr(tnode.id)}"`);
    named++;
  }
  if (!isSvg && elementKids.length === 0 && textKids.length) {
    const t = textKids.join(" ");
    if (t.length <= 48) attrs.push(`data-t="${escAttr(t)}"`);
  }
  let childTree = null;
  if (tnode && tag !== "svg" && !insideSvg) {
    if (tnode.children.length === elementKids.length) childTree = tnode.children;
    else if (tnode.children.length) mismatches.push(`${tnode.name} (${tnode.id}): tree ${tnode.children.length} vs jsx ${elementKids.length}`);
  }
  let k = 0;
  const inner = el.children
    .map((c) => (c.type === "JSXElement" ? emit(c, childTree ? childTree[k++] : null, isSvg) : emit(c, null, isSvg)))
    .join("");
  const open = `<${tag}${attrs.length ? " " + attrs.join(" ") : ""}>`;
  return `${open}${inner}</${tag}>`;
}

const html = emit(root.type === "JSXElement" ? root : root.expression ?? root, tree, false);
writeFileSync(outPath, html);
console.log(`${outPath}: ${html.length} chars, ${named} named` + (mismatches.length ? `\n  mismatches:\n  - ${mismatches.join("\n  - ")}` : ""));
