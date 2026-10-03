(function () {
  const C = CUES;
  const T = C.taps;
  const tl = gsap.timeline({ paused: true });
  const qs = (s, r = document) => r.querySelector(s);
  const qsa = (s, r = document) => Array.from(r.querySelectorAll(s));
  const pid = (id) => qs(`[data-pid="${id}"]`);
  const byT = (root, t) => qsa("[data-t]", root).find((e) => e.dataset.t === t);

  // ---- Canvas v0.4 motion tokens: damped springs (response, damping) ---------------
  function spring(response, damping) {
    const w = (2 * Math.PI) / response;
    const z = damping;
    const wd = w * Math.sqrt(1 - z * z);
    const T = Math.log(1000) / (z * w); // settles to 0.1%
    const ease = (p) => {
      if (p >= 1) return 1;
      const t = p * T;
      return 1 - Math.exp(-z * w * t) * (Math.cos(wd * t) + ((z * w) / wd) * Math.sin(wd * t));
    };
    return { duration: +T.toFixed(3), ease };
  }
  const SNAPPY = spring(0.3, 0.86);
  const SMOOTH = spring(0.4, 0.9);
  const BOUNCY = spring(0.5, 0.7);
  const SHEET = spring(0.42, 0.98);
  const sp = (s, v) => Object.assign({ duration: s.duration, ease: s.ease }, v);

  // ---- screen setup (static DOM restructure, before any tween) ---------------------
  // Home: pull the chrome that iOS keeps fixed out of the scrolling artboard.
  const homeRoot = pid("9U5-0");
  homeRoot.style.overflow = "visible";
  const fixed = qs("#home-fixed");
  const homeStatus = pid("A38-0");
  const spacer = document.createElement("div");
  spacer.style.cssText = "height:62px;flex-shrink:0;position:relative";
  homeStatus.replaceWith(spacer);
  Object.assign(homeStatus.style, { position: "absolute", left: "0px", top: "0px", width: "402px" });
  fixed.append(homeStatus);
  ["A37-0", "9V4-0", "9UG-0", "9U7-0", "9U6-0"].forEach((id) => fixed.append(pid(id)));
  const homeIsland = pid("A37-0");

  // Sheets: the Paper boards bake a recessed copy of the screen behind the sheet.
  // Here the real Home recedes instead, so hide the baked card.
  function sheetScreen(sel) {
    const root = qs(sel).firstElementChild;
    root.style.background = "transparent";
    root.style.height = "874px";
    root.children[0].style.display = "none";
    return { root, status: root.children[1], sheet: root.children[3] };
  }
  const R = sheetScreen("#scr-receipt");
  const G = sheetScreen("#scr-gallery");

  const sRoot = qs("#scr-search").children[1];
  sRoot.style.background = "transparent";
  const sCol = sRoot.children[0];
  const sRelated = sRoot.children[1];
  const sEdge = sRoot.children[2];
  const sBar = sRoot.children[3];
  const sHomeBtn = sBar.children[0];
  const sPill = sBar.children[1];
  const sPillRow = sPill.children[sPill.children.length - 1];
  const sClear = sPillRow.children[2];
  const sCard = sCol.children[0];
  const sInfo = sCol.children[1];

  const kRoot = qs("#scr-tracker").firstElementChild;
  kRoot.style.height = "874px";
  const kSteps = kRoot.children[4];
  const kRows = Array.from(kSteps.children);

  // Live-value tickers: old value slides up, new value slides in.
  function ticker(el, next) {
    const lh = parseFloat(getComputedStyle(el).lineHeight) || el.offsetHeight;
    const prev = el.textContent.trim();
    el.innerHTML = `<span class="tick-wrap" style="height:${lh}px"><span style="height:${lh}px">${prev}</span><span style="height:${lh}px">${next}</span></span>`;
    return { kids: Array.from(el.firstElementChild.children), lh };
  }
  function tick(tk, t) {
    tl.to(tk.kids, sp(SMOOTH, { y: -tk.lh }), t);
  }
  const within = (root, t) => byT(root, t);
  const tParking = ticker(within(pid("9ZB-0"), "42"), "41");
  const tCentre = ticker(within(pid("9WV-0"), "3"), "2");
  const tCentreNext = ticker(pid("9WU-0"), "8 min");
  const tExpo = ticker(within(pid("9WK-0"), "6"), "5");
  const tExpoNext = ticker(pid("9WJ-0"), "13 min");
  const tDhuhr = ticker(pid("9Y7-0"), "in 2h 30m");

  // Search typing: one span per character, plus a caret.
  const sText = byT(sRoot, "how much do I owe");
  const query = sText.textContent.trim();
  sText.removeAttribute("data-t");
  sText.innerHTML =
    query
      .split("")
      .map((c) => `<span class="ch" style="display:none;white-space:pre">${c}</span>`)
      .join("") + `<span class="caret"></span>`;
  const sChars = qsa(".ch", sText);
  const sCaret = qs(".caret", sText);

  // The widget Sara adds from the gallery, placed where the "Add widget" slot sits.
  const vehCard = byT(G.sheet, "Vehicle renewal").parentElement.previousElementSibling;
  const vehBadge = vehCard.children[1];
  const landed = vehCard.children[0].cloneNode(true);
  Object.assign(landed.style, { position: "absolute", left: "20px", top: "1548px", opacity: "0" });
  landed.id = "landed-widget";
  homeRoot.append(landed);

  // Everything inside the device is the Paper UI: its layering (content under glass bars,
  // sheets over a receded screen) is iOS behaviour, not a layout fault.
  qsa("#screen *").forEach((e) => e.setAttribute("data-layout-allow-overlap", ""));

  // ---- helpers -------------------------------------------------------------------------
  const touch = qs("#touch");
  const tDot = qs("#touch .dot");
  const tRing = qs("#touch .ring");
  function press(el, t, s = 0.97) {
    tl.to(el, { scale: s, duration: 0.09, ease: "power1.in" }, t);
    tl.to(el, sp(SNAPPY, { scale: 1 }), t + 0.09);
  }
  function tap(t, x, y, target, s) {
    tl.set(touch, { x, y }, t - 0.32);
    tl.fromTo(tDot, { opacity: 0, scale: 1.3 }, { opacity: 1, scale: 1, duration: 0.22, ease: "power2.out", immediateRender: false }, t - 0.3);
    tl.to(tDot, { scale: 0.82, duration: 0.09, ease: "power1.in" }, t);
    tl.to(tDot, { scale: 1, duration: 0.28, ease: "back.out(2)" }, t + 0.09);
    tl.fromTo(tRing, { opacity: 0.9, scale: 0.8 }, { opacity: 0, scale: 2.3, duration: 0.55, ease: "power2.out", immediateRender: false }, t);
    tl.to(tDot, { opacity: 0, duration: 0.25, ease: "power1.out" }, t + 0.3);
    if (target) press(target, t, s);
  }
  function drag(t0, t1, from, to) {
    tl.set(touch, { x: from[0], y: from[1] }, t0 - 0.25);
    tl.fromTo(tDot, { opacity: 0, scale: 1.3 }, { opacity: 1, scale: 0.88, duration: 0.22, ease: "power2.out", immediateRender: false }, t0 - 0.22);
    tl.to(touch, { x: to[0], y: to[1], duration: t1 - t0, ease: "power2.inOut" }, t0);
    tl.to(tDot, { opacity: 0, scale: 1, duration: 0.25, ease: "power1.out" }, t1);
  }
  // Camera: frame a point of the screen (screen coords) at a scale, keeping that point where
  // it sits in the frame. The origin stays at the phone's centre so moves can pan between points.
  const cam = qs("#cam");
  const O = { x: 215, y: 451 };
  gsap.set(cam, { transformOrigin: `${O.x}px ${O.y}px` });
  function camTo(t, sx, sy, scale, dur = 0.95) {
    if (sx == null) return tl.to(cam, { x: 0, y: 0, scale: 1, duration: dur, ease: "power3.inOut" }, t);
    const px = sx + 14, py = sy + 14;
    tl.to(cam, { x: -(px - O.x) * (scale - 1), y: -(py - O.y) * (scale - 1), scale, duration: dur, ease: "power3.inOut" }, t);
  }
  function push(tIn, tOut, sx, sy, scale) {
    camTo(tIn, sx, sy, scale);
    camTo(tOut, null, null, 1, 0.85);
  }
  // iOS sheet presentation: Home recedes, the sheet rises over black.
  const scrHome = qs("#scr-home");
  const homeDim = qs("#home-dim");
  function present(sheet, status, t) {
    tl.to(scrHome, { scale: 0.92, y: 48, borderRadius: 22, transformOrigin: "50% 0%", duration: 0.5, ease: "power3.out" }, t);
    tl.to(homeDim, { opacity: 0.28, duration: 0.4, ease: "power1.out" }, t);
    tl.to([homeStatus, homeIsland], { opacity: 0, duration: 0.2 }, t);
    tl.fromTo(status, { opacity: 0 }, { opacity: 1, duration: 0.25, immediateRender: false }, t + 0.05);
    tl.fromTo(sheet, { y: 874 }, sp(SHEET, { y: 0, immediateRender: false }), t);
  }
  function dismiss(sheet, status, t) {
    tl.to(sheet, { y: 874, duration: 0.42, ease: "power3.in" }, t);
    tl.to(status, { opacity: 0, duration: 0.2 }, t + 0.15);
    tl.to(scrHome, { scale: 1, y: 0, borderRadius: 0, duration: 0.5, ease: "power3.out" }, t + 0.18);
    tl.to(homeDim, { opacity: 0, duration: 0.4 }, t + 0.18);
    tl.to([homeStatus, homeIsland], { opacity: 1, duration: 0.2 }, t + 0.3);
  }
  function count(el, from, to, t, d, fmt) {
    const o = { v: from };
    tl.fromTo(o, { v: from }, { v: to, duration: d, ease: "power2.out", immediateRender: false, onUpdate: () => (el.textContent = fmt(o.v)) }, t);
    el.textContent = fmt(from);
  }
  const fmtMoney = (v) => v.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  // Initial states for screens that arrive later (immediate, outside the timeline).
  gsap.set([R.sheet, G.sheet], { y: 874 });
  gsap.set([R.status, G.status], { opacity: 0 });
  gsap.set("#scr-tracker", { x: 402 });
  gsap.set("#scr-search .search-bg", { opacity: 0 });
  gsap.set([sCol, sRelated, sEdge, sHomeBtn, sPill, sClear, sCaret], { opacity: 0 });
  // Search's status bar, island and home indicator duplicate Home's; show them only while search is up.
  const sChrome = [sRoot.children[4], sRoot.children[5], sRoot.children[6]];
  gsap.set(sChrome, { opacity: 0 });
  // Screens leave the render tree while they are off stage (keeps the capture light).
  function onStage(sel, t0, t1) {
    gsap.set(sel, { display: "none" });
    tl.set(sel, { display: "block" }, t0);
    tl.set(sel, { display: "none" }, t1);
  }

  // ---- backdrop ambience ------------------------------------------------------------
  tl.fromTo("#blob-sky", { x: 0, y: 0 }, { x: -160, y: 90, duration: C.duration, ease: "sine.inOut" }, 0);
  tl.fromTo("#blob-lilac", { x: 0, y: 0 }, { x: 140, y: -70, duration: C.duration, ease: "sine.inOut" }, 0);
  tl.fromTo("#blob-peach", { x: 0, y: 0, scale: 1 }, { x: -120, y: -60, scale: 1.12, duration: C.duration, ease: "sine.inOut" }, 0);

  // ---- 0 · title ----------------------------------------------------------------------
  tl.fromTo("#logo", { x: 728, y: 214, scale: 3 }, { x: 0, y: 0, scale: 1, duration: 0.95, ease: "expo.inOut" }, 3.2);
  tl.fromTo("#logo-dark", { opacity: 0, y: 26 }, { opacity: 1, y: 0, duration: 0.8, ease: "expo.out" }, 0.15);
  tl.fromTo("#logo-light", { opacity: 0, x: -22 }, { opacity: 1, x: 0, duration: 0.8, ease: "expo.out" }, 0.38);
  tl.fromTo("#logo-overlap", { opacity: 0, scale: 0.4, transformOrigin: "50% 50%" }, sp(BOUNCY, { opacity: 1, scale: 1 }), 0.62);
  tl.fromTo("#title-kicker", { opacity: 0, y: 14 }, { opacity: 1, y: 0, duration: 0.9, ease: "expo.out" }, 0.55);
  tl.fromTo("#title-head .w", { yPercent: 112 }, { yPercent: 0, duration: 0.9, ease: "expo.out", stagger: 0.07 }, 0.75);
  tl.fromTo("#title-sub", { opacity: 0, y: 18 }, { opacity: 1, y: 0, duration: 0.7, ease: "power2.out" }, 1.45);
  tl.to("#title-block", { opacity: 0, y: -40, duration: 0.45, ease: "power2.in" }, 3.05);
  tl.fromTo("#bug-label", { opacity: 0, x: -14 }, { opacity: 1, x: 0, duration: 0.6, ease: "power3.out" }, 3.85);
  tl.fromTo("#rule-top", { scaleX: 0 }, { scaleX: 1, duration: 1.2, ease: "expo.inOut" }, 3.8);
  tl.fromTo("#persona", { opacity: 0, x: 14 }, { opacity: 1, x: 0, duration: 0.6, ease: "power3.out" }, 4.4);

  // ---- phone entrance + Home comes alive ------------------------------------------
  tl.fromTo("#phone-pos", { y: 820, rotation: 5 }, { y: 0, rotation: 0, duration: 1.25, ease: "expo.out" }, 3.3);
  tl.fromTo([pid("A2W-0"), pid("A25-0"), pid("AL2-0"), pid("A2E-0")], { opacity: 0, scale: 0.5 }, sp(BOUNCY, { opacity: 1, scale: 1, stagger: 0.05 }), 3.55);
  tl.fromTo(pid("A1W-0"), { opacity: 0, y: 16 }, sp(SMOOTH, { opacity: 1, y: 0 }), 3.65);
  tl.fromTo(pid("KGZ-0"), { opacity: 0, scale: 0.85, transformOrigin: "0% 50%" }, sp(BOUNCY, { opacity: 1, scale: 1 }), 3.75);
  tl.fromTo([pid("A19-0"), pid("A0J-0")], { opacity: 0, x: 90 }, sp(SMOOTH, { opacity: 1, x: 0, stagger: 0.08 }), 3.8);
  tl.fromTo(pid("HHB-0"), { opacity: 0, y: 16 }, sp(SMOOTH, { opacity: 1, y: 0 }), 3.95);
  tl.fromTo([pid("HHF-0"), pid("HHK-0"), pid("HHU-0"), pid("HHZ-0")], { opacity: 0, scale: 0.6 }, sp(BOUNCY, { opacity: 1, scale: 1, stagger: 0.05 }), 4.1);
  tl.fromTo([pid("HI5-0"), pid("9YC-0"), pid("9XF-0"), pid("9WF-0"), pid("9VG-0")], { opacity: 0, y: 44 }, sp(SMOOTH, { opacity: 1, y: 0, stagger: 0.07 }), 4.2);
  tl.fromTo([pid("9UG-0"), pid("9U7-0")], { opacity: 0, y: 40 }, sp(SMOOTH, { opacity: 1, y: 0, stagger: 0.05 }), 3.9);
  // a gentle breath on the Needs you count while the camera is in
  tl.to(pid("A21-0"), { scale: 1.25, duration: 0.22, ease: "power2.out", yoyo: true, repeat: 1 }, 6.9);
  push(6.0, 8.5, 201, 300, 1.34);

  // ---- 02 · widgets ---------------------------------------------------------------
  const scroll = qs("#home-scroll");
  const topEdge = qs("#home-topedge");
  gsap.set(topEdge, { opacity: 0 });
  tl.to(scroll, { y: -640, duration: 1.15, ease: "power3.inOut" }, 9.6);
  tl.to(topEdge, { opacity: 1, duration: 0.4 }, 9.8);
  push(10.6, 12.9, 201, 380, 1.12);
  tl.to(pid("9X5-0"), { scale: 1.8, opacity: 0.5, duration: 0.45, ease: "power1.inOut", yoyo: true, repeat: 5 }, 10.5);
  tick(tParking, 11.2);
  tl.to(pid("9Z9-0"), { scaleX: 0.92, transformOrigin: "0% 50%", duration: 0.6, ease: "power2.out" }, 11.2);
  tick(tCentre, 11.85);
  tick(tCentreNext, 11.9);
  tick(tExpo, 12.0);
  tick(tExpoNext, 12.05);
  tick(tDhuhr, 12.5);
  tap(T.salik, 348, 194, pid("9YX-0"), 0.9);

  // ---- 03 · receipt + Happiness Meter -------------------------------------------
  present(R.sheet, R.status, T.salik + 0.35);
  const paidLabel = byT(R.sheet, "Paid");
  const paidCheck = paidLabel.previousElementSibling;
  const rHero = paidLabel.parentElement;
  const rAmount = byT(R.sheet, "100.00");
  const rBlocks = Array.from(R.sheet.children).slice(3); // table, download, Happiness Meter card
  const r0 = T.salik + 0.35;
  tl.fromTo(paidCheck, { scale: 0.3, opacity: 0 }, sp(BOUNCY, { scale: 1, opacity: 1 }), r0 + 0.32);
  tl.fromTo(Array.from(rHero.children).slice(1), { y: 18 }, sp(SMOOTH, { y: 0, stagger: 0.04 }), r0 + 0.05);
  count(rAmount, 0, 100, r0 + 0.3, 0.7, fmtMoney);
  tl.fromTo(rBlocks, { y: 36 }, sp(SMOOTH, { y: 0, stagger: 0.05 }), r0 + 0.08);
  push(16.5, 18.95, 201, 745, 1.3);
  const happyLabel = byT(R.sheet, "Happy");
  const happyFace = happyLabel.previousElementSibling;
  const faceCols = Array.from(happyLabel.parentElement.parentElement.children);
  tap(T.happy, 88, 734, happyFace, 0.88);
  tl.to(happyFace, { boxShadow: "0 0 0 3px #176442", duration: 0.25, ease: "power2.out" }, T.happy + 0.1);
  tl.to(faceCols.slice(1), { opacity: 0.4, duration: 0.3 }, T.happy + 0.12);
  tap(T.close, 44, 117, R.sheet.children[1].children[0]);
  dismiss(R.sheet, R.status, T.close + 0.2);

  // ---- 04 · search ------------------------------------------------------------------
  const homeTab = pid("9UG-0");
  const homeSearch = pid("9U7-0");
  tap(T.search, 350, 813, homeSearch, 0.9);
  const s0 = T.search + 0.2;
  tl.to("#scr-search .search-bg", { opacity: 1, duration: 0.35, ease: "power1.out" }, s0);
  tl.to(homeTab, { opacity: 0, scale: 0.3, transformOrigin: "0% 50%", duration: 0.32, ease: "power2.in" }, s0);
  tl.to(homeSearch, { opacity: 0, duration: 0.15 }, s0 + 0.1);
  tl.fromTo(sHomeBtn, { opacity: 0, scale: 0.4 }, sp(BOUNCY, { opacity: 1, scale: 1, immediateRender: false }), s0 + 0.15);
  tl.set(sPill, { opacity: 1 }, s0);
  tl.fromTo(sPill, { clipPath: "inset(0px 0px 0px 232px round 28px)" }, sp(SMOOTH, { clipPath: "inset(0px 0px 0px 0px round 28px)", immediateRender: false }), s0);
  tl.fromTo(sPillRow, { opacity: 0 }, { opacity: 1, duration: 0.25, immediateRender: false }, s0 + 0.15);
  tl.set(sEdge, { opacity: 1 }, s0 + 0.3);
  tl.set(sChrome, { opacity: 1 }, s0);
  // caret blinks, then the question is typed
  const typeStart = s0 + 0.42;
  tl.set(sCaret, { opacity: 1 }, s0 + 0.2);
  tl.set(sCaret, { opacity: 1 }, typeStart);
  sChars.forEach((c, i) => tl.set(c, { display: "inline" }, typeStart + i * 0.065));
  const typed = typeStart + sChars.length * 0.065;
  tl.set(sClear, { opacity: 1 }, typeStart + 0.08);
  for (let k = 0; k < 8; k++) tl.set(sCaret, { opacity: k % 2 ? 1 : 0 }, typed + 0.3 + k * 0.3);
  // the direct answer rises
  const aT = typed + 0.2;
  tl.set(sCol, { opacity: 1 }, aT);
  tl.fromTo(sCard, { opacity: 0, y: 40 }, sp(SMOOTH, { opacity: 1, y: 0 }), aT);
  tl.fromTo(Array.from(sCard.children), { opacity: 0, y: 14 }, sp(SMOOTH, { opacity: 1, y: 0, stagger: 0.07 }), aT + 0.08);
  count(byT(sCard, "1,012.60"), 0, 1012.6, aT + 0.15, 0.8, fmtMoney);
  tl.fromTo(sInfo, { opacity: 0 }, { opacity: 1, duration: 0.4 }, aT + 0.45);
  tl.fromTo(sRelated, { opacity: 0, y: 16 }, sp(SMOOTH, { opacity: 1, y: 0, immediateRender: false }), aT + 0.55);
  camTo(s0 + 0.3, 201, 813, 1.42, 0.8);
  camTo(aT + 0.25, 201, 292, 1.24, 1.0);
  camTo(25.75, null, null, 1, 0.85);
  tl.set(scroll, { y: 0 }, 24.5); // Home is covered here; reset it to the top for the next beat
  tl.set(topEdge, { opacity: 0 }, 24.5);
  tap(T.pay, 201, 413, byT(sCard, "Pay both · AED 1,012.60").parentElement);
  tap(T.homeCircle, 48, 813, sHomeBtn, 0.9);
  const s1 = T.homeCircle + 0.15;
  tl.to([sCol, sRelated, sEdge, sCaret], { opacity: 0, duration: 0.18, ease: "power1.in" }, s1);
  tl.to(sPill, { opacity: 0, scale: 0.92, duration: 0.2, ease: "power2.in" }, s1);
  tl.to(sHomeBtn, { opacity: 0, scale: 0.6, duration: 0.2, ease: "power2.in" }, s1);
  tl.to("#scr-search .search-bg", { opacity: 0, duration: 0.32, ease: "power1.out" }, s1 + 0.2);
  tl.to(homeTab, sp(SMOOTH, { opacity: 1, scale: 1 }), s1 + 0.22);
  tl.to(homeSearch, { opacity: 1, duration: 0.2 }, s1 + 0.28);
  tl.set(sChrome, { opacity: 0 }, s1 + 0.5);

  // ---- 05 · activity: rail → request tracker ---------------------------------------
  drag(T.rail, T.rail + 0.45, [300, 390], [96, 390]);
  tl.to([pid("A19-0"), pid("A0J-0")], sp(SMOOTH, { x: -284 }), T.rail + 0.05);
  tap(T.cont, 156, 476, pid("A0M-0"));
  const p0 = T.cont + 0.15;
  tl.fromTo("#scr-tracker", { x: 402 }, { x: 0, duration: 0.5, ease: "power3.out", immediateRender: false }, p0);
  tl.to(scrHome, { x: -110, duration: 0.5, ease: "power3.out" }, p0);
  tl.to(homeDim, { opacity: 0.12, duration: 0.4 }, p0);
  const rowDot = (r) => r.children[0].children[0];
  const rowLine = (r) => r.children[0].children[1];
  tl.fromTo([rowDot(kRows[0]), rowDot(kRows[1])], { scale: 0.4 }, sp(BOUNCY, { scale: 1, stagger: 0.22 }), p0 + 0.45);
  tl.fromTo([rowLine(kRows[0]), rowLine(kRows[1])], { scaleY: 0, transformOrigin: "50% 0%" }, { scaleY: 1, duration: 0.35, ease: "power2.out", stagger: 0.22 }, p0 + 0.6);
  tl.fromTo(rowDot(kRows[2]), { scale: 0.4 }, sp(BOUNCY, { scale: 1 }), p0 + 1.05);
  tl.to(rowDot(kRows[2]), { scale: 1.16, duration: 0.4, ease: "sine.inOut", yoyo: true, repeat: 3 }, p0 + 1.7);
  tl.fromTo(kRows[2].children[1].children[1], { opacity: 0, y: 10 }, sp(SMOOTH, { opacity: 1, y: 0 }), p0 + 1.2);
  tl.fromTo(kRows.slice(3), { opacity: 0.25 }, { opacity: 1, duration: 0.4, stagger: 0.1 }, p0 + 1.3);
  push(p0 + 1.25, 32.05, 201, 640, 1.26);
  tap(T.scan, 160, 677, byT(kRoot, "Scan passport").parentElement);
  tap(T.back, 44, 88, kRoot.children[1].children[0]);
  const b0 = T.back + 0.15;
  tl.to("#scr-tracker", { x: 402, duration: 0.45, ease: "power3.inOut" }, b0);
  tl.to(scrHome, { x: 0, duration: 0.45, ease: "power3.inOut" }, b0);
  tl.to(homeDim, { opacity: 0, duration: 0.4 }, b0);

  // ---- 06 · your Home: pull past the end → gallery → add ----------------------------
  const maxScroll = -961.5;
  tl.to(scroll, { y: maxScroll, duration: 1.15, ease: "power3.inOut" }, 33.55);
  tl.to(topEdge, { opacity: 1, duration: 0.4 }, 33.7);
  drag(T.pull, T.pull + 0.45, [201, 700], [201, 560]);
  tl.to(scroll, { y: maxScroll - 74, duration: 0.45, ease: "power2.out" }, T.pull);
  tl.to(pid("9V8-0"), { rotation: 180, duration: 0.3, ease: "power2.out" }, T.pull + 0.15);
  present(G.sheet, G.status, T.pull + 0.45);
  tl.to(scroll, sp(SMOOTH, { y: maxScroll }), T.pull + 0.55);
  tl.set(pid("9V8-0"), { rotation: 0 }, T.pull + 1.2);
  tap(T.add, 214, 323, vehBadge, 0.85);
  tl.to(vehCard, { scale: 1.04, duration: 0.3, ease: "power2.out" }, T.add + 0.1);
  dismiss(G.sheet, G.status, T.add + 0.35);
  const l0 = T.add + 0.5;
  tl.fromTo(landed, { opacity: 0, y: -90, scale: 1.06 }, sp(BOUNCY, { opacity: 1, y: 0, scale: 1, immediateRender: false }), l0);
  tl.to([pid("9VA-0"), pid("9V6-0")], sp(SMOOTH, { y: 187 }), l0);

  // ---- end card ---------------------------------------------------------------------
  const e0 = 39.3;
  tl.to(["#bug-label", "#persona", "#rule-top"], { opacity: 0, duration: 0.35 }, e0 - 0.2);
  tl.to("#progress", { opacity: 0, duration: 0.35 }, e0 - 0.2);
  tl.to("#logo", { x: 0, y: 248, scale: 2.6, duration: 1.0, ease: "expo.inOut" }, e0);
  tl.fromTo("#end-head .w", { yPercent: 112 }, { yPercent: 0, duration: 0.9, ease: "expo.out", stagger: 0.07 }, e0 + 0.45);
  tl.fromTo("#end-meta", { opacity: 0, y: 16 }, { opacity: 1, y: 0, duration: 0.7, ease: "power2.out" }, e0 + 1.1);
  tl.to(cam, { scale: 0.97, y: -8, duration: C.duration - e0, ease: "sine.inOut" }, e0);

  // ---- chapter copy + progress ------------------------------------------------------
  const fills = qsa("#progress .fill");
  const counts = qsa("#progress-count span");
  tl.fromTo("#progress", { opacity: 0 }, { opacity: 1, duration: 0.5 }, 4.0);
  C.chapters.forEach((ch, i) => {
    const el = qs(`#ch-${i}`);
    tl.fromTo(el, { opacity: 0, y: 0 }, { opacity: 1, duration: 0.01, immediateRender: false }, ch.in);
    tl.fromTo(qs(".ch-label", el), { opacity: 0, x: -24 }, { opacity: 1, x: 0, duration: 0.5, ease: "power3.out" }, ch.in);
    tl.fromTo(qs(".tick", el), { scaleX: 0 }, { scaleX: 1, duration: 0.6, ease: "expo.out" }, ch.in + 0.05);
    tl.fromTo(qsa(".w", el), { yPercent: 112 }, { yPercent: 0, duration: 0.8, ease: "expo.out", stagger: 0.06 }, ch.in + 0.08);
    tl.fromTo(qs(".ch-sub", el), { opacity: 0, y: 18 }, { opacity: 1, y: 0, duration: 0.6, ease: "power2.out" }, ch.in + 0.4);
    tl.to(el, { opacity: 0, y: -28, duration: 0.38, ease: "power2.in" }, ch.out);
    tl.fromTo(fills[i], { scaleX: 0 }, { scaleX: 1, duration: ch.out - ch.in + 0.4, ease: "none" }, ch.in);
    tl.set(counts[i], { opacity: 1 }, ch.in);
    if (i < C.chapters.length - 1) tl.set(counts[i], { opacity: 0 }, C.chapters[i + 1].in);
  });

  onStage("#scr-receipt", T.salik + 0.3, T.close + 0.75);
  onStage("#scr-search", T.search + 0.15, T.homeCircle + 0.8);
  onStage("#scr-tracker", T.cont + 0.1, T.back + 0.7);
  onStage("#scr-gallery", T.pull + 0.4, T.add + 0.85);
  // Home is fully covered while search and the tracker are up.
  tl.set("#home-scroll", { visibility: "hidden" }, T.search + 0.6);
  tl.set("#home-scroll", { visibility: "visible" }, T.homeCircle + 0.35);
  tl.set("#home-scroll", { visibility: "hidden" }, T.cont + 0.7);
  tl.set("#home-scroll", { visibility: "visible" }, T.back + 0.12);

  window.__timelines["main"] = tl;
})();
