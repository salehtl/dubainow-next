(function () {
  const C = CUES;
  const T = C.taps;
  const P = Object.fromEntries(C.personas.map((p) => [p.id, p]));
  const tl = gsap.timeline({ paused: true });
  const qs = (s, r = document) => r.querySelector(s);
  const qsa = (s, r = document) => Array.from(r.querySelectorAll(s));
  const pid = (id, r = document) => qs(`[data-pid="${id}"]`, r);
  const byT = (root, t) => qsa("[data-t]", root).find((e) => e.dataset.t === t);
  const findText = (root, t) => qsa("div", root).find((e) => e.children.length === 0 && e.textContent.trim() === t);
  const setText = (el, t) => {
    el.textContent = t;
    el.removeAttribute("data-t");
  };

  // Screen-space points (402×874 screen coordinates) measured from the converted Paper screens.
  const M = {
    kCard: { x: 156, y: 336 },
    kScroll: -640,
    kWidget: { x: 201, y: 289 },
    fPayAll: { x: 134, y: 230 },
    rHappy: { x: 88, y: 752 },
    rFaces: { x: 201, y: 760 },
    sScroll: -560,
    sWidget: { x: 201, y: 327 },
    sBadge: { x: 27, y: 247 },
    sRenew: { x: 321, y: 274 },
    mCard: { x: 246, y: 345 },
    mSearch: { x: 52, y: 813 },
    mResult: { x: 201, y: 214 },
  };

  // ---- Canvas v0.4 motion tokens: damped springs (response, damping) ---------------
  function spring(response, damping) {
    const w = (2 * Math.PI) / response;
    const z = damping;
    const wd = w * Math.sqrt(1 - z * z);
    const T = Math.log(1000) / (z * w);
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

  // ---- Home screens: lift the chrome iOS keeps fixed out of the scrolling artboard -------
  function setupHome(id) {
    const scr = qs("#" + id);
    const scroll = qs(".home-scroll", scr);
    const root = scroll.firstElementChild;
    root.style.overflow = "visible";
    const fixed = qs(".home-fixed", scr);
    const child = (n) => Array.from(root.children).find((c) => c.dataset.n === n);
    const status = child("Status bar");
    const spacer = document.createElement("div");
    spacer.style.cssText = "height:62px;flex-shrink:0;position:relative";
    status.replaceWith(spacer);
    Object.assign(status.style, { position: "absolute", left: "0px", top: "0px", width: "402px" });
    fixed.append(status);
    const island = child("Dynamic Island");
    const tab = child("Tab bar · Liquid Glass");
    const search = child("Search · Liquid Glass");
    [island, child("Soft scroll edge · Bottom"), tab, search, child("Home indicator")].forEach((e) => fixed.append(e));
    return { scr, scroll, root, fixed, status, island, tab, search, topedge: qs(".topedge", scr), dim: qs(".dim", scr) };
  }
  const K = setupHome("scr-khalid");
  const F = setupHome("scr-faisal");
  const S = setupHome("scr-sara");

  // ---- Faisal: the Home layout with a café owner's data ---------------------------------
  setText(pid("A2G-0"), "F");
  pid("A2F-0").style.backgroundImage = "linear-gradient(150deg, #E4EEFF 0%, #B9CCF6 60%, #8FAAEA 100%)";
  pid("A2G-0").style.color = "#1E2A5E";
  setText(pid("A22-0"), "3");
  setText(pid("A1Z-0"), "3 due this week");
  setText(pid("KH4-0"), "Pay all · AED 2,035.20");
  setText(pid("A1L-0"), "1,486.20");
  setText(pid("A1H-0"), "Al Quoz 3 · Account 2017 4420");
  {
    const dewa = pid("A19-0");
    const du = dewa.cloneNode(true);
    const inDu = (n) => qs(`[data-n="${n}"]`, du);
    setText(inDu("Entity name"), "du");
    setText(inDu("Due tomorrow"), "Due in 3 days");
    setText(inDu("Value"), "399.00");
    setText(inDu("Title"), "Internet and phone bill");
    setText(inDu("Meta"), "Al Quoz 3 · Account 0045 2231");
    qs("svg", inDu("Glyph")).innerHTML =
      '<path d="M3.5 9.6a12 12 0 0 1 17 0M6.6 12.8a7.6 7.6 0 0 1 10.8 0M9.7 15.9a3.2 3.2 0 0 1 4.6 0" fill="none" stroke="var(--color-hue-sand-ink)" stroke-width="2.2" stroke-linecap="round"/><circle cx="12" cy="19" r="1.5" fill="var(--color-hue-sand-ink)"/>';
    qsa("[data-pid]", du).forEach((e) => e.removeAttribute("data-pid"));
    du.removeAttribute("data-pid");
    du.id = "faisal-du";
    pid("A0J-0").replaceWith(du);
  }

  // ---- Receipt sheet (Faisal's Pay all) ------------------------------------------------
  function sheetScreen(sel) {
    const root = qs(sel).firstElementChild;
    root.style.background = "transparent";
    root.style.height = "874px";
    root.children[0].style.display = "none"; // the board's baked recessed screen; the real Home recedes instead
    return { root, status: root.children[1], sheet: root.children[3] };
  }
  const R = sheetScreen("#scr-receipt");
  setText(findText(R.sheet, "Topped up your Salik account"), "Paid 2 bills and 1 fine");
  const rAmount = byT(R.sheet, "100.00");
  const rRows = Array.from(R.sheet.children[3].children).filter((_, i) => i % 2 === 0);
  [
    ["DEWA · café", "AED 1,486.20"],
    ["du · café", "AED 399.00"],
    ["Traffic fine · van 2", "AED 150.00"],
    ["Paid with", "Apple Pay"],
  ].forEach(([label, value], i) => {
    const row = rRows[i];
    setText(row.children[0], label);
    setText(row.children[1], value);
    Object.assign(row.children[1].style, { fontFamily: "var(--font-sans)", fontSize: "15px", fontWeight: "600", lineHeight: "20px" });
  });
  setText(findText(R.sheet, "How was topping up Salik?"), "How was paying everything at once?");
  setText(findText(R.sheet, "One tap. It goes to Salik and the Happiness Meter."), "One tap. It goes to DubaiNow and the Happiness Meter.");

  // ---- Sara: the suggested Family visas widget + toast ---------------------------------
  const sSlot = pid("JVS-0");
  const sWidget = pid("JVT-0");
  const sSuggest = pid("JWU-0");
  const sBadge = pid("JWX-0");
  const sRenew = pid("JW4-0");
  const sRings = ["JWA-0", "JWI-0"].map((id) => qsa("circle", pid(id))[1]);
  const toastWrap = qs("#toast-wrap");
  const toast = toastWrap.firstElementChild;
  setText(findText(toast, "Added to Favorites"), "Family visas added to Home");
  qs("svg", toast).innerHTML = '<path d="M6.5 12.5l3.6 3.6 7.4-7.6" fill="none" stroke="#FFFFFF" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/>';

  // ---- Mahmoud: the same Home in English, then in Arabic ------------------------------------
  // Arabic copy, sizes and isolates come from the AR masters (page 20 "Home system and records ·
  // Light · Arabic", C1 "Widgets · Launch set · Arabic", 13 "Navigation chrome … Arabic").
  const LRI = "⁦";
  const PDI = "⁩";
  const iso = (v) => LRI + v + PDI;
  const findIncl = (root, t) => qsa("div", root).find((e) => e.children.length === 0 && e.textContent.includes(t));
  const MINT = "linear-gradient(150deg, #E1F5EA 0%, #B4E1CA 60%, #86CCA9 100%)";
  const meScr = qs("#scr-mahmoud-en");
  const enRoot = qs(".home-scroll", meScr).firstElementChild;
  {
    const enP = (id) => pid(id, enRoot);
    setText(enP("A2G-0"), "M");
    enP("A2F-0").style.backgroundImage = MINT;
    enP("A2G-0").style.color = "#164A35";
    setText(enP("A22-0"), "2");
    setText(enP("A1Z-0"), "1 due this week");
    enP("KGY-0").style.display = "none"; // Pay all only shows when two or more payments are due
    setText(enP("A1L-0"), "186.40");
    setText(enP("A1H-0"), "Al Qusais 1 · Account 3302 7718");
  }
  const arScr = qs("#scr-mahmoud-ar");
  const arRoot = enRoot.cloneNode(true);
  qs(".home-scroll", arScr).append(arRoot);
  arRoot.style.direction = "rtl";
  const arP = (id) => pid(id, arRoot);
  const AR = (el, text, size, line, weight) => {
    setText(el, text);
    Object.assign(el.style, { fontFamily: "var(--font-arabic)", fontSize: size + "px", lineHeight: line + "px", letterSpacing: "0px", textTransform: "none" });
    if (weight) el.style.fontWeight = String(weight);
    return el;
  };
  const flipHue = (el) => (el.style.backgroundImage = el.style.backgroundImage.replace("160deg", "200deg"));
  // Needs you
  AR(arP("A23-0"), "يحتاج انتباهك", 24, 30, 700);
  AR(arP("A1Z-0"), "فاتورة تستحق هذا الأسبوع", 15, 22, 400);
  AR(arP("A1X-0"), "عرض الكل", 17, 24, 500);
  {
    const brief = qs("#frag-ar-brief").firstElementChild.cloneNode(true);
    findText(brief, "bolt").outerHTML = '<svg width="18" height="18" viewBox="0 0 24 24"><path d="M13 2.5 5.5 13.5h5.5l-1 8 7.5-11h-5.5l1-8Z" fill="var(--color-hue-sand-ink)"/></svg>';
    setText(findIncl(brief, "412.60"), iso("186.40"));
    setText(findIncl(brief, "البرشاء"), "القصيص 1 · حساب " + iso("3302 7718"));
    brief.style.flexShrink = "0";
    arP("A19-0").replaceWith(brief);
  }
  AR(arP("A14-0"), "الإقامة", 15, 20, 700);
  AR(arP("A12-0"), iso("3") + " أيام متبقية", 14, 20, 700);
  AR(arP("A0Y-0"), "من " + iso("5") + " خطوات", 17, 22, 700);
  AR(arP("A0S-0"), "ارفع نسخة من جواز سفرك", 18, 26, 700);
  AR(arP("A0R-0"), "تجديد الإقامة", 15, 22, 400);
  AR(arP("A0N-0"), "متابعة", 17, 22, 700);
  arP("A0O-0").style.transform = "scaleX(-1)";
  flipHue(arP("A0T-0"));
  // Favorites: most-used first, on the right
  AR(arP("HHC-0"), "المفضلة", 24, 30, 700);
  AR(arP("HHD-0"), "تعديل", 17, 24, 500);
  [["HHJ-0", "دفع المواقف"], ["HHT-0", "سالك"], ["HHY-0", "المركبات"], ["HI3-0", "التبرع"]].forEach(([id, t]) => AR(arP(id), t, 14, 20, 400));
  ["HHG-0", "HHL-0", "HHV-0", "HI0-0"].forEach((id) => flipHue(arP(id)));
  // Your widgets: the Arabic launch set
  AR(arP("HI6-0"), "أدواتك", 24, 30, 700);
  AR(arP("HI7-0"), "تعديل", 17, 24, 500);
  {
    const gantry = arP("9Z1-0").cloneNode(true);
    const grid = qs("#frag-ar-widgets").firstElementChild.cloneNode(true);
    grid.style.width = "362px";
    findText(grid, "toll").replaceWith(gantry);
    arP("9YC-0").replaceWith(grid);
    ["9XF-0", "9WF-0", "9VG-0"].forEach((id) => arP(id).remove());
  }
  AR(arP("9VB-0"), "إضافة أداة", 17, 24, 700);
  arP("9V6-0").style.display = "none";
  qsa("[data-pid]", arRoot).forEach((e) => e.removeAttribute("data-pid"));

  const ME = setupHome("scr-mahmoud-en");
  const MA = setupHome("scr-mahmoud-ar");
  MA.status.style.direction = "rtl";
  Object.assign(MA.tab.style, { left: "auto", right: "20px", direction: "rtl" });
  Object.assign(MA.search.style, { left: "20px", right: "auto" });
  [["Home", "الرئيسية", 700], ["Services", "الخدمات", 500], ["Activity", "النشاط", 500]].forEach(([en, ar, w]) => AR(findText(MA.tab, en), ar, 13, 18, w));

  // ---- Arabic search: the Did-you-mean layout, right to left -----------------------------------
  const sRoot = qs("#scr-search").children[1];
  sRoot.style.background = "transparent";
  sRoot.style.direction = "rtl";
  const [sCol, sEdge, sBar, sStatus, sIsland, sIndicator] = Array.from(sRoot.children);
  const sChrome = [sStatus, sIsland, sIndicator];
  const [sHead, sCard, sAlso, sNot] = Array.from(sCol.children);
  const sHomeBtn = sBar.children[0];
  const sPill = sBar.children[1];
  const sPillRow = sPill.children[sPill.children.length - 1];
  const sClear = sPillRow.children[2];
  sHead.style.display = "none";
  sCol.style.paddingTop = "26px";
  const icon = sCard.children[0].children[0];
  icon.style.backgroundImage = "linear-gradient(in oklab 200deg, var(--color-hue-peach-start) 0%, var(--color-hue-peach-end) 100%)";
  qs("svg", icon).innerHTML =
    '<rect x="3.5" y="7.5" width="17" height="12" rx="2.6" fill="none" stroke="var(--color-hue-peach-ink)" stroke-width="1.8"/><path d="M8.5 7.5V6a1.8 1.8 0 0 1 1.8-1.8h3.4A1.8 1.8 0 0 1 15.5 6v1.5M3.5 12.5h17" fill="none" stroke="var(--color-hue-peach-ink)" stroke-width="1.8" stroke-linecap="round"/>';
  AR(findText(sCard, "Renew your vehicle registration"), "الإبلاغ عن مشكلة مع جهة العمل", 19, 27, 700).style.textWrap = "balance";
  const sAlias = AR(findText(sCard, "Also called Mulkiya renewal"), "يُعرف أيضًا باسم «صوت العامل»", 13, 18, 700);
  AR(findText(sCard, "RTA · 5 min"), "اللجنة الدائمة لشؤون العمال", 13, 18, 600);
  sCard.children[1].style.display = "none";
  AR(findText(sCard, "Start now"), "ابدأ الآن", 17, 24, 700);
  AR(findText(sAlso, "Also matching"), "نتائج مشابهة", 14, 20, 700);
  AR(findText(sAlso, "Replace a lost vehicle registration card"), "الاستعلام عن حالة التأشيرة", 18, 28, 500);
  AR(findText(sAlso, "RTA · also called lost Mulkiya"), "الإدارة العامة للإقامة وشؤون الأجانب", 15, 24, 400);
  AR(findText(sAlso, "Transfer vehicle ownership"), "دفع غرامات مخالفة الإقامة", 18, 28, 500);
  AR(findText(sAlso, "RTA"), "يُعرف أيضًا باسم «غرامات الإقامة»", 15, 24, 400);
  // inset separators start from the reading edge, so in Arabic the inset sits on the right
  Array.from(sAlso.children[1].children).forEach((c) => {
    if (c.style.paddingLeft === "16px") Object.assign(c.style, { paddingLeft: "0px", paddingRight: "16px" });
  });
  AR(findText(sNot, "Not what you need?"), "ليس ما تبحث عنه؟", 14, 20, 700);
  AR(findText(sNot, "Browse by topic"), "تصفّح حسب الفئة", 16, 22, 700);
  AR(findText(sNot, "Get help"), "احصل على مساعدة", 16, 22, 700);
  const QUERY = "صوت العامل";
  const sText = byT(sBar, "mulkia");
  sText.removeAttribute("data-t");
  Object.assign(sText.style, { fontFamily: "var(--font-arabic)", fontSize: "18px", lineHeight: "24px", direction: "rtl" });
  sText.innerHTML = '<span class="q"></span><span class="caret"></span>';
  const sQ = qs(".q", sText);
  const sCaret = qs(".caret", sText);

  // Everything inside the device is Paper UI: its layering (content under glass, sheets over a
  // receded screen) is iOS behaviour, not a layout fault.
  qsa("#screen *").forEach((e) => {
    e.setAttribute("data-layout-allow-overlap", "");
    e.setAttribute("data-layout-allow-occlusion", "");
  });

  // ---- helpers -------------------------------------------------------------------------
  const touch = qs("#touch");
  const tDot = qs("#touch .dot");
  const tRing = qs("#touch .ring");
  function press(el, t, s = 0.96) {
    tl.to(el, { scale: s, duration: 0.09, ease: "power1.in" }, t);
    tl.to(el, sp(SNAPPY, { scale: 1 }), t + 0.09);
  }
  function tap(t, x, y, target, s) {
    tl.set(touch, { x, y }, t - 0.34);
    tl.fromTo(tDot, { opacity: 0, scale: 1.3 }, { opacity: 1, scale: 1, duration: 0.24, ease: "power2.out", immediateRender: false }, t - 0.32);
    tl.to(tDot, { scale: 0.82, duration: 0.09, ease: "power1.in" }, t);
    tl.to(tDot, { scale: 1, duration: 0.3, ease: "back.out(2)" }, t + 0.09);
    tl.fromTo(tRing, { opacity: 0.9, scale: 0.8 }, { opacity: 0, scale: 2.3, duration: 0.6, ease: "power2.out", immediateRender: false }, t);
    tl.to(tDot, { opacity: 0, duration: 0.28, ease: "power1.out" }, t + 0.32);
    if (target) press(target, t, s);
  }
  const cam = qs("#cam");
  const O = { x: 215, y: 451 };
  gsap.set(cam, { transformOrigin: `${O.x}px ${O.y}px` });
  function camTo(t, sx, sy, scale, dur = 1.0, ease = "power3.inOut") {
    if (sx == null) return tl.to(cam, { x: 0, y: 0, scale: 1, duration: dur, ease }, t);
    const px = sx + 14;
    const py = sy + 14;
    tl.to(cam, { x: -(px - O.x) * (scale - 1), y: -(py - O.y) * (scale - 1), scale, duration: dur, ease }, t);
  }
  function present(home, sheet, status, t) {
    tl.to(home.scr, { scale: 0.92, y: 48, borderRadius: 22, transformOrigin: "50% 0%", duration: 0.52, ease: "power3.out" }, t);
    tl.to(home.dim, { opacity: 0.28, duration: 0.4, ease: "power1.out" }, t);
    tl.to([home.status, home.island], { opacity: 0, duration: 0.2 }, t);
    tl.fromTo(status, { opacity: 0 }, { opacity: 1, duration: 0.25, immediateRender: false }, t + 0.05);
    tl.fromTo(sheet, { y: 874 }, sp(SHEET, { y: 0, immediateRender: false }), t);
  }
  function count(el, to, t, d, fmt) {
    const o = { v: 0 };
    el.textContent = fmt(0);
    tl.fromTo(o, { v: 0 }, { v: to, duration: d, ease: "power2.out", immediateRender: false, onUpdate: () => (el.textContent = fmt(o.v)) }, t);
  }
  const fmtMoney = (v) => v.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  function words(el, t, stagger = 0.05) {
    tl.fromTo(qsa(".w", el), { yPercent: 112 }, { yPercent: 0, duration: 0.85, ease: "expo.out", stagger }, t);
  }
  function onStage(el, t0, t1) {
    gsap.set(el, { display: "none" });
    tl.set(el, { display: "block" }, t0);
    tl.set(el, { display: "none" }, t1);
  }

  // Initial states (immediate, outside the timeline).
  gsap.set(R.sheet, { y: 874 });
  gsap.set(R.status, { opacity: 0 });
  gsap.set(["#stack-faisal", "#stack-sara", "#stack-mahmoud"], { y: 874 });
  gsap.set("#scr-search .search-bg", { opacity: 0 });
  gsap.set([sCol, sEdge, sHomeBtn, sPill, sClear, sCaret], { opacity: 0 });
  gsap.set(sChrome, { opacity: 0 });
  gsap.set(toastWrap, { opacity: 0, y: 30 });
  sRings.forEach((c) => c.setAttribute("stroke-dasharray", "0 126"));
  gsap.set(sSlot, { opacity: 0, scale: 0.92, transformOrigin: "50% 40%" });
  const sBelow = [pid("JQC-0"), pid("JPF-0"), pid("JN5-0")];
  gsap.set(sBelow, { y: -215 });
  gsap.set(sBadge, { scale: 0 });

  // ---- backdrop ambience ----------------------------------------------------------------
  tl.fromTo("#blob-sky", { x: 0, y: 0 }, { x: -170, y: 90, duration: C.duration, ease: "sine.inOut" }, 0);
  tl.fromTo("#blob-lilac", { x: 0, y: 0 }, { x: 150, y: -80, duration: C.duration, ease: "sine.inOut" }, 0);
  tl.fromTo("#blob-peach", { x: 0, y: 0, scale: 1 }, { x: -130, y: -60, scale: 1.12, duration: C.duration, ease: "sine.inOut" }, 0);

  // ---- title -------------------------------------------------------------------------------
  tl.fromTo("#logo", { x: 737.6, y: 226, scale: 2.6 }, { x: 0, y: 0, scale: 1, duration: 0.95, ease: "expo.inOut" }, 3.3);
  tl.fromTo("#logo-dark", { opacity: 0, y: 26 }, { opacity: 1, y: 0, duration: 0.85, ease: "expo.out" }, 0.3);
  tl.fromTo("#logo-light", { opacity: 0, x: -22 }, { opacity: 1, x: 0, duration: 0.85, ease: "expo.out" }, 0.5);
  tl.fromTo("#logo-overlap", { opacity: 0, scale: 0.4, transformOrigin: "50% 50%" }, sp(BOUNCY, { opacity: 1, scale: 1 }), 0.74);
  tl.fromTo("#title-kicker", { opacity: 0, y: 14 }, { opacity: 1, y: 0, duration: 0.9, ease: "expo.out" }, 0.8);
  words(qs("#title-head"), 1.0, 0.07);
  tl.fromTo("#title-sub", { opacity: 0, y: 18 }, { opacity: 1, y: 0, duration: 0.8, ease: "power2.out" }, 1.7);
  tl.to("#title-block", { opacity: 0, y: -40, duration: 0.45, ease: "power2.in" }, 3.2);
  tl.fromTo("#bug-label", { opacity: 0, x: -14 }, { opacity: 1, x: 0, duration: 0.6, ease: "power3.out" }, 3.95);
  tl.fromTo("#rule-top", { scaleX: 0 }, { scaleX: 1, duration: 1.2, ease: "expo.inOut" }, 3.85);

  // ---- persona copy + roster --------------------------------------------------------------
  const order = C.personas.map((p) => p.id);
  const ends = order.map((_, i) => (i < order.length - 1 ? C.personas[i + 1].t0 : C.library.t0));
  C.personas.forEach((p, i) => {
    const el = qs(`#p-${p.id}`);
    const t = p.t0 + (i === 0 ? 0.12 : 0.02);
    tl.fromTo(el, { opacity: 0, y: 0 }, { opacity: 1, duration: 0.01, immediateRender: false }, t);
    tl.fromTo(qs(".p-who", el), { opacity: 0, x: -24 }, { opacity: 1, x: 0, duration: 0.6, ease: "power3.out" }, t);
    words(qs(".p-head", el), t + 0.1);
    tl.fromTo(qs(".p-sub", el), { opacity: 0, y: 18 }, { opacity: 1, y: 0, duration: 0.7, ease: "power2.out" }, t + 0.45);
    tl.fromTo(qs(".p-service", el), { opacity: 0, y: 14 }, { opacity: 1, y: 0, duration: 0.6, ease: "power2.out" }, t + 0.7);
    tl.to(el, { opacity: 0, y: -26, duration: 0.36, ease: "power2.in" }, ends[i] - 0.5);
  });
  const rItems = order.map((id) => qs(`#r-${id}`));
  const RSTEP = 184;
  gsap.set(rItems, { width: 150 });
  tl.fromTo("#roster", { opacity: 0, y: 12 }, { opacity: 1, y: 0, duration: 0.6, ease: "power2.out" }, 4.5);
  C.personas.forEach((p, i) => {
    const t = i === 0 ? 4.5 : p.t0 - 0.35;
    tl.to(rItems, { opacity: (k) => (k === i ? 1 : 0.38), duration: 0.4, ease: "power1.inOut" }, t);
    if (i === 0) tl.fromTo("#r-bar", { x: 0, scaleX: 0 }, { scaleX: 1, duration: 0.6, ease: "expo.out" }, t + 0.1);
    else tl.to("#r-bar", { x: RSTEP * i, duration: 0.6, ease: "expo.inOut" }, t);
  });
  tl.to("#roster", { opacity: 0, y: 12, duration: 0.4, ease: "power2.in" }, C.library.t0 - 0.35);

  // ---- phone: entrance, persona swipes, exit --------------------------------------------
  tl.fromTo("#phone-pos", { y: 900, rotation: 4 }, { y: 0, rotation: 0, duration: 1.4, ease: "expo.out" }, 3.38);
  const stacks = order.map((id) => qs(`#stack-${id}`));
  C.personas.forEach((p, i) => {
    if (i === 0) return;
    const t = p.t0 - 0.62;
    tl.to(stacks[i - 1], { y: -874, duration: 0.72, ease: "power3.inOut" }, t);
    tl.fromTo(stacks[i], { y: 874 }, { y: 0, duration: 0.72, ease: "power3.inOut", immediateRender: false }, t);
  });
  C.personas.forEach((p, i) => onStage(stacks[i], i === 0 ? 0 : p.t0 - 0.7, i < order.length - 1 ? ends[i] + 0.2 : ends[i] + 0.9));
  tl.to("#phone-pos", { y: 1180, rotation: -3, duration: 0.8, ease: "power3.in" }, 47.7);

  // ---- Khalid · Home comes alive, then the journey -------------------------------------------
  tl.fromTo([pid("K5T-0"), pid("K52-0"), pid("K5I-0"), pid("K5B-0")], { opacity: 0, scale: 0.5 }, sp(BOUNCY, { opacity: 1, scale: 1, stagger: 0.05 }), 3.5);
  tl.fromTo(pid("K4T-0"), { opacity: 0, y: 16 }, sp(SMOOTH, { opacity: 1, y: 0 }), 3.58);
  tl.fromTo([pid("K6L-0"), pid("K46-0")], { opacity: 0, x: 90 }, sp(SMOOTH, { opacity: 1, x: 0, stagger: 0.08 }), 3.64);
  tl.fromTo(pid("K3B-0"), { opacity: 0, y: 16 }, sp(SMOOTH, { opacity: 1, y: 0 }), 3.74);
  tl.fromTo([pid("K36-0"), pid("K2W-0"), pid("K2R-0"), pid("K2M-0")], { opacity: 0, scale: 0.6 }, sp(BOUNCY, { opacity: 1, scale: 1, stagger: 0.05 }), 3.8);
  tl.fromTo([pid("K2H-0"), pid("K79-0"), pid("K0B-0")], { opacity: 0, y: 44 }, sp(SMOOTH, { opacity: 1, y: 0, stagger: 0.07 }), 3.9);
  tl.fromTo([K.tab, K.search], { opacity: 0, y: 40 }, sp(SMOOTH, { opacity: 1, y: 0, stagger: 0.05 }), 3.66);
  camTo(5.9, M.kCard.x, M.kCard.y, 1.24, 1.1);
  tl.to(pid("K6V-0"), { scale: 1.12, duration: 0.3, ease: "power2.out", yoyo: true, repeat: 1 }, 7.0);
  camTo(8.15, null, null, 1, 1.0);
  tl.to(K.scroll, { y: M.kScroll, duration: 1.25, ease: "power3.inOut" }, 8.2);
  tl.to(K.topedge, { opacity: 1, duration: 0.4 }, 8.4);
  tl.fromTo([pid("K7S-0"), pid("K7Y-0"), pid("K84-0")], { scale: 0.3 }, sp(BOUNCY, { scale: 1, stagger: 0.16 }), 9.85);
  tl.fromTo(pid("K8A-0"), { scale: 0.3 }, sp(BOUNCY, { scale: 1 }), 10.35);
  tl.to(pid("K8A-0"), { scale: 1.35, duration: 0.42, ease: "sine.inOut", yoyo: true, repeat: 3 }, 10.9);
  tl.to(pid("K8R-0"), { scale: 1.08, duration: 0.42, ease: "sine.inOut", yoyo: true, repeat: 3, transformOrigin: "0% 50%" }, 10.9);
  camTo(9.95, M.kWidget.x, M.kWidget.y, 1.2, 1.1);
  camTo(13.6, null, null, 1, 1.0);

  // ---- Faisal · Pay all -----------------------------------------------------------------------
  const f0 = P.faisal.t0;
  camTo(f0 + 1.3, M.fPayAll.x, M.fPayAll.y + 30, 1.26, 1.0);
  tap(T.payAll, M.fPayAll.x, M.fPayAll.y, pid("KGZ-0"), 0.94);
  camTo(T.payAll + 0.3, null, null, 1, 0.75);
  const r0 = T.payAll + 0.38;
  present(F, R.sheet, R.status, r0);
  const paid = findText(R.sheet, "Paid");
  tl.fromTo(paid.previousElementSibling, { scale: 0.3, opacity: 0 }, sp(BOUNCY, { scale: 1, opacity: 1 }), r0 + 0.42);
  tl.fromTo(Array.from(paid.parentElement.children).slice(1), { y: 18 }, sp(SMOOTH, { y: 0, stagger: 0.04 }), r0 + 0.05);
  count(rAmount, 2035.2, r0 + 0.4, 0.9, fmtMoney);
  tl.fromTo(Array.from(R.sheet.children).slice(3), { y: 36 }, sp(SMOOTH, { y: 0, stagger: 0.05 }), r0 + 0.08);
  tl.fromTo(rRows, { opacity: 0, x: 18 }, { opacity: 1, x: 0, duration: 0.45, ease: "power2.out", stagger: 0.09 }, r0 + 0.55);
  camTo(r0 + 2.4, M.rFaces.x, M.rFaces.y, 1.3, 1.1);
  const happy = findText(R.sheet, "Happy");
  const happyFace = happy.previousElementSibling;
  const faces = Array.from(happy.parentElement.parentElement.children);
  tap(T.happy, M.rHappy.x, M.rHappy.y, happyFace, 0.88);
  tl.to(happyFace, { boxShadow: "0 0 0 3px #176442", duration: 0.25, ease: "power2.out" }, T.happy + 0.1);
  tl.to(faces.slice(1), { opacity: 0.4, duration: 0.3 }, T.happy + 0.12);
  camTo(T.happy + 1.5, null, null, 1, 1.0);

  // ---- Sara · the family's visas, suggested and renewed together ----------------------------------
  const s0 = P.sara.t0;
  tl.to(S.scroll, { y: M.sScroll, duration: 1.25, ease: "power3.inOut" }, s0 + 1.0);
  tl.to(S.topedge, { opacity: 1, duration: 0.4 }, s0 + 1.2);
  const sArrive = 31.15;
  camTo(sArrive - 0.2, M.sWidget.x, M.sWidget.y, 1.2, 1.1);
  tl.to(sBelow, sp(SMOOTH, { y: 0 }), sArrive - 0.05);
  tl.to(sSlot, sp(BOUNCY, { opacity: 1, scale: 1 }), sArrive + 0.08);
  tl.to(sBadge, sp(BOUNCY, { scale: 1 }), sArrive + 0.25);
  sRings.forEach((c, i) => tl.to(c, { attr: { "stroke-dasharray": "102 126" }, duration: 1.1, ease: "power2.out" }, sArrive + 0.3 + i * 0.12));
  tap(T.addSuggestion, M.sBadge.x, M.sBadge.y, sBadge, 0.85);
  tl.to(sBadge, { scale: 0, duration: 0.25, ease: "back.in(2)" }, T.addSuggestion + 0.12);
  tl.to(sSuggest, { opacity: 0, duration: 0.25 }, T.addSuggestion + 0.15);
  tl.to(sBelow, sp(SMOOTH, { y: -28 }), T.addSuggestion + 0.35);
  tl.to(sWidget, { scale: 1.03, duration: 0.18, ease: "power2.out", yoyo: true, repeat: 1 }, T.addSuggestion + 0.12);
  tl.to(toastWrap, sp(SHEET, { opacity: 1, y: 0 }), T.addSuggestion + 0.25);
  tl.to(toastWrap, { opacity: 0, y: 30, duration: 0.35, ease: "power2.in" }, T.addSuggestion + 2.75);
  tap(T.renew, M.sRenew.x, M.sRenew.y, sRenew, 0.92);
  camTo(T.renew + 0.9, null, null, 1, 1.0);

  // ---- Mahmoud · the same Home, in Arabic; then search that speaks it ------------------------------
  const W = C.wipe;
  const glow = qs("#wipe-glow");
  onStage(arScr, W - 0.1, ends[3] + 0.9);
  onStage(qs("#scr-search"), T.arSearch - 0.05, ends[3] + 0.9);
  tl.set(meScr, { display: "none" }, W + 1.05);
  gsap.set(arScr, { clipPath: "inset(0px 0px 0px 402px)" });
  tl.to(arScr, { clipPath: "inset(0px 0px 0px 0px)", duration: 0.95, ease: "power2.inOut" }, W);
  tl.set(arScr, { clipPath: "none" }, W + 1.0);
  tl.fromTo(glow, { x: 402 }, { x: 0, duration: 0.95, ease: "power2.inOut", immediateRender: false }, W);
  tl.to(glow, { opacity: 1, duration: 0.15 }, W);
  tl.to(glow, { opacity: 0, duration: 0.25 }, W + 0.78);
  const toggle = qs("#lang-toggle");
  tl.to(qs(".thumb", toggle), sp(SMOOTH, { x: 128 }), W + 0.05);
  tl.to(qs(".seg.en", toggle), { color: "#55555c", duration: 0.3 }, W + 0.05);
  tl.to(qs(".seg.ar", toggle), { color: "#1d1d21", duration: 0.3 }, W + 0.05);
  camTo(W + 1.15, M.mCard.x, M.mCard.y, 1.22, 1.0);
  camTo(W + 3.0, null, null, 1, 0.85);

  tap(T.arSearch, M.mSearch.x, M.mSearch.y, MA.search, 0.9);
  const q0 = T.arSearch + 0.2;
  tl.to("#scr-search .search-bg", { opacity: 1, duration: 0.35, ease: "power1.out" }, q0);
  tl.set(sChrome, { opacity: 1 }, q0);
  tl.to(MA.tab, { opacity: 0, scale: 0.3, transformOrigin: "100% 50%", duration: 0.32, ease: "power2.in" }, q0);
  tl.to(MA.search, { opacity: 0, duration: 0.15 }, q0 + 0.1);
  tl.fromTo(sHomeBtn, { opacity: 0, scale: 0.4 }, sp(BOUNCY, { opacity: 1, scale: 1, immediateRender: false }), q0 + 0.15);
  tl.set(sPill, { opacity: 1 }, q0);
  tl.fromTo(sPill, { clipPath: "inset(0px 232px 0px 0px round 28px)" }, sp(SMOOTH, { clipPath: "inset(0px 0px 0px 0px round 28px)", immediateRender: false }), q0);
  tl.fromTo(sPillRow, { opacity: 0 }, { opacity: 1, duration: 0.25, immediateRender: false }, q0 + 0.15);
  tl.set(sEdge, { opacity: 1 }, q0 + 0.3);
  camTo(q0 + 0.2, 201, 813, 1.4, 0.8);
  tl.set(sCaret, { opacity: 1 }, q0 + 0.2);
  const typeStart = q0 + 0.42;
  const typeDur = QUERY.length * 0.075;
  const qo = { n: 0 };
  sQ.textContent = "";
  tl.fromTo(qo, { n: 0 }, { n: QUERY.length, duration: typeDur, ease: "none", immediateRender: false, onUpdate: () => (sQ.textContent = QUERY.slice(0, Math.floor(qo.n + 0.0001))) }, typeStart);
  const typed = typeStart + typeDur;
  tl.set(sClear, { opacity: 1 }, typeStart + 0.08);
  for (let k = 0; k < 8; k++) tl.set(sCaret, { opacity: k % 2 ? 1 : 0 }, typed + 0.3 + k * 0.3);
  const aT = typed + 0.2;
  tl.set(sCol, { opacity: 1 }, aT);
  tl.fromTo(sCard, { opacity: 0, y: 36 }, sp(SMOOTH, { opacity: 1, y: 0 }), aT);
  tl.fromTo([sAlso, sNot], { opacity: 0, y: 24 }, sp(SMOOTH, { opacity: 1, y: 0, stagger: 0.1 }), aT + 0.22);
  camTo(aT + 0.1, M.mResult.x, M.mResult.y, 1.28, 1.0);
  tl.to(sAlias, { scale: 1.1, transformOrigin: "100% 50%", duration: 0.32, ease: "power2.out", yoyo: true, repeat: 1 }, 46.45);
  camTo(47.0, null, null, 1, 0.7);

  // ---- library: three kinds of service ----------------------------------------------------------
  const L0 = C.library.t0;
  tl.fromTo("#lib-copy", { opacity: 0 }, { opacity: 1, duration: 0.01, immediateRender: false }, L0 + 0.05);
  words(qs("#lib-copy .p-head"), L0 + 0.1);
  tl.fromTo("#lib-copy .p-sub", { opacity: 0, y: 18 }, { opacity: 1, y: 0, duration: 0.7, ease: "power2.out" }, L0 + 0.5);
  const libRows = qsa(".lib-row");
  tl.fromTo(libRows, { opacity: 0, y: 60 }, sp(SMOOTH, { opacity: 1, y: 0, stagger: 0.16 }), L0 + 0.18);
  tl.fromTo(qsa(".lib-card"), { scale: 0.96 }, sp(BOUNCY, { scale: 1, stagger: 0.16 }), L0 + 0.24);
  tl.to(["#lib-copy", ...libRows], { opacity: 0, y: -24, duration: 0.4, ease: "power2.in", stagger: 0.04 }, C.end.t0 - 0.5);

  // ---- end card --------------------------------------------------------------------------------
  const e0 = C.end.t0;
  tl.to(["#bug-label", "#rule-top"], { opacity: 0, duration: 0.35 }, e0 - 0.4);
  tl.to("#logo", { x: 742.4, y: 324, scale: 2.4, duration: 1.0, ease: "expo.inOut" }, e0 - 0.15);
  words(qs("#end-head"), e0 + 0.45, 0.08);
  tl.fromTo("#end-sub", { opacity: 0, y: 16 }, { opacity: 1, y: 0, duration: 0.8, ease: "power2.out" }, e0 + 1.0);
  tl.fromTo("#end-meta", { opacity: 0, y: 12 }, { opacity: 1, y: 0, duration: 0.8, ease: "power2.out" }, e0 + 1.45);

  window.__timelines["main"] = tl;
})();
