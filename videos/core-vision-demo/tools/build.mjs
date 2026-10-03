// Assembles index.html from src/template.html + src/main.js + src/cues.json + converted Paper screens.
// Usage: node tools/build.mjs
import { readFileSync, writeFileSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const read = (p) => readFileSync(new URL("../" + p, import.meta.url), "utf8");
const cues = JSON.parse(read("src/cues.json"));
const esc = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const mask = (w) => `<span class="mask"><span class="w">${esc(w)}</span></span>`;
// Words joined by a no-break space stay on one line but keep their own masks, so the gap matches.
const words = (text) =>
  text
    .split(" ")
    .map((w) => (w.includes(" ") ? `<span class="keep">${w.split(" ").map(mask).join("")}</span>` : mask(w)))
    .join("");

// Canvas hues (start, end, ink) for persona avatars.
const HUES = {
  sand: ["#FFF0DC", "#FBDDB8", "#5A3A12"],
  sky: ["#E4EEFF", "#CFDDFB", "#1E2A5E"],
  rose: ["#FBE6E4", "#F4CBC6", "#5E1F28"],
  mint: ["#E1F5EA", "#C2E7D6", "#164A35"],
  lilac: ["#EFE7FA", "#DCCFF3", "#3C2A66"],
};
const hueStyle = (h) => `background-image: linear-gradient(160deg, ${HUES[h][0]}, ${HUES[h][1]}); color: ${HUES[h][2]}`;

// Service-type badges, as on the C1 widget boards: stacked circles for All at once, the route mark for Start to finish.
const ICONS = {
  bundle: `<svg viewBox="0 0 22 12"><circle cx="5.5" cy="6" r="4.4" fill="currentColor" stroke="#e6ebff" stroke-width="1.4"/><circle cx="11" cy="6" r="4.4" fill="currentColor" stroke="#e6ebff" stroke-width="1.4"/><circle cx="16.5" cy="6" r="4.4" fill="currentColor" stroke="#e6ebff" stroke-width="1.4"/></svg>`,
  journey: `<svg viewBox="0 0 24 24"><circle cx="4.5" cy="18.5" r="2.6" fill="currentColor"/><circle cx="19.5" cy="5.5" r="2.6" fill="currentColor"/><path d="M6.5 17.5C11 16 8 9.5 12.5 8.2C14.6 7.6 15.9 7 17.4 6.2" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-dasharray="2.2 2.6"/></svg>`,
  plain: `<svg viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.5" fill="none" stroke="currentColor" stroke-width="2.4"/><path d="M15.5 15.5 21 21" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>`,
};

const personas = cues.personas
  .map(
    (p) => `        <div class="persona" id="p-${p.id}">
          <div class="p-who"><div class="p-avatar" style="${hueStyle(p.hue)}">${esc(p.initial)}</div><div><div class="p-name">${esc(p.name)}</div><div class="p-role">${esc(p.role)}</div></div></div>
          <div class="p-head">${words(p.head)}</div>
          <div class="p-sub">${esc(p.sub)}</div>
          <div class="p-service">${
            p.badge === "lang"
              ? `<span class="lang-toggle" id="lang-toggle"><span class="thumb"></span><span class="seg en">${esc(p.badgeText)}</span><span class="seg ar" dir="rtl">${esc(p.badgeTextAr)}</span></span><span class="svc-meta">${esc(p.meta)}</span>`
              : `<span class="badge badge-${p.badge}">${ICONS[p.badge] || ""}${esc(p.badgeText)}</span><span class="svc-name">${esc(p.service)}</span><span class="svc-dot"></span><span class="svc-meta">${esc(p.meta)}</span>`
          }</div>
        </div>`,
  )
  .join("\n");
const roster =
  cues.personas
    .map((p) => `<div class="r-item" id="r-${p.id}"><div class="r-av" style="${hueStyle(p.hue)}">${esc(p.initial)}</div><div class="r-name">${esc(p.name)}</div></div>`)
    .join("") + `<div class="r-bar" id="r-bar"></div>`;

const logo = read("assets/dubainow-logo.svg")
  .replace(/<title[^>]*>.*?<\/title>/s, "")
  .replace(/ role="img" aria-labelledby="t"/, "")
  .replace(/ width="[\d.]+" height="[\d.]+"/, "")
  .replace(/id="(dark|light|overlap)"/g, 'id="logo-$1"');

// SFX: one <audio> per cue, each on its own lane so nothing overlaps.
const dur = {};
const probe = (f) =>
  (dur[f] ??= parseFloat(
    execFileSync("ffprobe", ["-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", fileURLToPath(new URL(`../assets/sfx/${f}.mp3`, import.meta.url))]).toString(),
  ));
const sfx = [...Object.values(cues.taps).map((t) => ({ file: "click-soft", t, vol: 0.4 })), ...cues.sfx].sort((a, b) => a.t - b.t);
let audio = sfx
  .map((s, i) => {
    const d = Math.min(probe(s.file), cues.duration - s.t).toFixed(3);
    return `      <audio id="sfx-${i}" src="assets/sfx/${s.file}.mp3" data-start="${s.t}" data-duration="${d}" data-track-index="${20 + (i % 6)}" data-volume="${s.vol}"></audio>`;
  })
  .join("\n");
if (cues.music) {
  audio += `\n      <audio id="bgm" data-timeline-role="music" src="${cues.music.src}" data-start="0" data-duration="${cues.duration}" data-track-index="10" data-volume="${cues.music.volume}"></audio>`;
}

let html = read("src/template.html")
  .replace("/*FONTS*/", read("source/fonts.css"))
  .replace("/*TOKENS*/", read("source/paper/tokens.css"))
  .replaceAll("/*DURATION*/", String(cues.duration))
  .replace("/*END_DURATION*/", (cues.duration - 54.0).toFixed(2))
  .replace("/*TITLE_KICKER*/", esc(cues.title.kicker))
  .replace("/*TITLE_WORDS*/", words(cues.title.head))
  .replace("/*TITLE_SUB*/", esc(cues.title.sub))
  .replace("/*PERSONAS*/", personas)
  .replace("/*ROSTER*/", roster)
  .replace("/*LIB_WORDS*/", words(cues.library.head))
  .replace("/*LIB_SUB*/", esc(cues.library.sub))
  .replace("/*END_WORDS*/", words(cues.end.head))
  .replace("/*END_SUB*/", esc(cues.end.sub))
  .replace("/*END_META*/", esc(cues.end.meta))
  .replaceAll("/*ICON:bundle*/", ICONS.bundle)
  .replaceAll("/*ICON:journey*/", ICONS.journey)
  .replace("/*LOGO*/", logo)
  .replace("/*AUDIO*/", audio)
  .replace("/*CUES*/", `const CUES = ${JSON.stringify(cues)};`)
  .replace("/*MAIN*/", read("src/main.js"));
for (const name of ["khalid", "home", "receipt", "sara", "toast", "didyoumean", "w-bills", "w-house", "ar-brief", "ar-widgets"]) {
  html = html.replaceAll(`/*SCREEN:${name}*/`, read(`source/html/${name}.html`));
}
const left = html.match(/\/\*(SCREEN|ICON|[A-Z_]+)[:A-Za-z_-]*\*\//g);
if (left) throw new Error("Unfilled placeholders: " + left.join(", "));
writeFileSync(new URL("../index.html", import.meta.url), html);
console.log(`index.html: ${(html.length / 1024).toFixed(0)} KB, ${sfx.length} sfx cues`);
