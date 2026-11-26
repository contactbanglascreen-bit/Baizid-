// Cinedor website — liquid glass edition
(function () {
  "use strict";
  const REPO = "contactbanglascreen-bit/Baizid-";
  const RELEASES = "https://api.github.com/repos/" + REPO + "/releases/latest";
  const FALLBACK = "https://github.com/" + REPO + "/releases/latest";
  const TMDB = "https://api.themoviedb.org/3/trending/all/week?api_key=17509a46d9802f1f5b73bf7b9d8bad7c&page=";
  const IMG = "https://image.tmdb.org/t/p/";
  const CONFIG = "https://firestore.googleapis.com/v1/projects/cinedor-60aac/databases/(default)/documents/config/app" +
    "?key=AIzaSyCddEpMsakf5vHeLlHuJLMPUCC50uTYOpY";

  const $ = (s, r) => (r || document).querySelector(s);
  const $$ = (s, r) => Array.from((r || document).querySelectorAll(s));
  const store = {
    get(k) { try { return localStorage.getItem(k); } catch (e) { return null; } },
    set(k, v) { try { localStorage.setItem(k, v); } catch (e) {} },
  };

  /* ---------------- Language (English / বাংলা) ---------------- */
  const BN = {
    nav_trending: "ট্রেন্ডিং", nav_features: "ফিচার", nav_install: "ইনস্টল", nav_faq: "প্রশ্ন", nav_download: "ডাউনলোড",
    pill: "Android এর জন্য ফ্রি",
    h1a: "মুভি, সিরিজ আর খেলা।", h1c: "সব সুন্দর করে এক অ্যাপে।",
    how_s: "১ মিনিটের গাইড", how_b: "কিভাবে ইনস্টল করবেন", tb_home: "হোম",
    lead: "হলিউড, বলিউড, বাংলা, কোরিয়ান ড্রামা, অ্যানিমে আর লাইভ খেলা দেখুন HD তে — যেকোনো ভাষার সাবটাইটেল আর অফলাইনে দেখার জন্য ডাউনলোড সহ।",
    get_on: "ডাউনলোড করুন", safe: "অফিসিয়াল ও নিরাপদ",
    qr_t: "কম্পিউটারে আছেন?", qr_s: "ফোন দিয়ে স্ক্যান করুন",
    f1t: "নতুন এপিসোড এসেছে", f1s: "এখনই দেখতে চাপুন", f2t: "ডাউনলোড হয়েছে · 1080p", f2s: "অফলাইনে দেখার জন্য রেডি",
    st1: "আল্ট্রা HD", st2: "ভাষা", st3: "সাবটাইটেল", st4: "ফ্রি",
    tr_k: "এই সপ্তাহে", tr_h: "Cinedor এর টপ ১০",
    fe_k: "কেন Cinedor", fe_h: "দেখার মজার জন্য যা যা দরকার, সব আছে",
    b1t: "স্মুথ ভিডিও প্লেয়ার", b1p: "4K পর্যন্ত কোয়ালিটি, স্লো নেটে নিজে কোয়ালিটি ঠিক করে, যেখানে থেমেছিলেন সেখান থেকে চালু, জুম আর এক লাইনের সাবটাইটেল।",
    b1sub: "ভোর হওয়ার আগেই আমি পৌঁছে যাব।",
    b2t: "সব ভাষা", b2p: "ভাষা দিয়ে খুঁজুন — বাংলা, হিন্দি, ইংলিশ, কোরিয়ান আরও অনেক।",
    b3t: "সাবটাইটেল ও ডাবিং", b3p: "AI অনুবাদ সহ যেকোনো ভাষার সাবটাইটেল, সাথে ডাব করা অডিও।",
    b4t: "অফলাইন ডাউনলোড", b4p: "পছন্দের কোয়ালিটিতে দ্রুত ডাউনলোড। পরে দেখতে নেট লাগবে না।",
    b5t: "লাইভ খেলা", b5p: "লাইভ ম্যাচ দেখুন, খেলা শুরু হলে নোটিফিকেশন পাবেন।",
    b6t: "পিকচার-ইন-পিকচার", b6p: "চ্যাট করার সময়ও ছোট উইন্ডোতে দেখতে থাকুন।",
    b7t: "কোনো রিলিজ মিস হবে না", b7p: "নতুন মুভি আর এপিসোড আসলে পোস্টার সহ নোটিফিকেশন।",
    b8t: "My List আর রিজিউম", b8p: "পরে দেখার জন্য সেভ করুন, আর যেখানে থেমেছিলেন সেখান থেকেই আবার দেখুন।",
    b9t: "Premium, কোনো বিজ্ঞাপন নেই", b9p: "ফ্রিতে দেখুন। কোড দিয়ে Premium নিলে সব বিজ্ঞাপন বন্ধ।",
    dv_k: "Android এর জন্য তৈরি", dv_h: "ফোন হোক বা ট্যাব — একদম ঠিকঠাক",
    dv_p: "Cinedor যেকোনো স্ক্রিনে সুন্দর মানিয়ে যায়। হালকা অ্যাপ, আর আপডেট হয় অ্যাপের ভিতর থেকেই।",
    dv1: "ফোন", dv2: "ট্যাবলেট", dv2s: "বড় স্ক্রিনের লেআউট", dv3: "টিভিতে কাস্ট", dv3s: "বড় পর্দায় দেখুন",
    in_k: "মাত্র ১ মিনিট", in_h: "কিভাবে ইনস্টল করবেন", in_p: "Cinedor এই ওয়েবসাইট থেকে ইনস্টল করতে হয়, Play Store থেকে না।",
    s1t: "ডাউনলোড", s1p: "ডাউনলোড বাটনে চাপুন। APK ফাইলটা ছোট আর নিরাপদ।",
    s2t: "ইনস্টলের অনুমতি", s2p: "ফাইলটা খুলুন। জিজ্ঞেস করলে Settings চেপে “Allow from this source” চালু করুন।",
    s3t: "উপভোগ করুন", s3p: "Install চাপুন, তারপর Open। নতুন ভার্সন অ্যাপের ভিতর থেকেই আপডেট হবে।",
    fq_k: "প্রশ্ন", fq_h: "সাধারণ প্রশ্ন",
    q1: "Cinedor কি ফ্রি?", a1: "হ্যাঁ, ফ্রিতে দেখতে পারবেন। চাইলে Premium নিয়ে বিজ্ঞাপন বন্ধ করতে পারেন।",
    q2: "ইনস্টল করা কি নিরাপদ?", a2: "শুধু এই ওয়েবসাইট থেকে Cinedor ডাউনলোড করুন। অ্যাপ নিজে চেক করে এটা অফিসিয়াল কিনা, আর নিরাপদে আপডেট হয়।",
    q3: "কোন কোন ডিভাইসে চলে?", a3: "Android 7.0 বা নতুন যেকোনো ফোন আর ট্যাবলেটে। iPhone এ এখনো চলে না।",
    q4: "Android “blocked” বলছে — কী করব?", a4: "Play Store এর বাইরের অ্যাপে এটা স্বাভাবিক। Settings চেপে ব্রাউজার থেকে ইনস্টলের অনুমতি দিন, তারপর ফিরে এসে Install চাপুন।",
    q5: "কিভাবে আপডেট করব?", a5: "নতুন ভার্সন আসলে Cinedor এ Update উইন্ডো আসবে। Update now চাপুন — ডাউনলোড আর My List থেকে যাবে।",
    q6: "সমস্যা হলে কোথায় জানাব?", a6: "Cinedor খুলে Me → Feedback এ লিখুন। আমরা সেখানেই উত্তর দিই আর ঠিক হলে জানিয়ে দিই।",
    fn_h: "দেখা শুরু করতে রেডি?", fn_p: "ফ্রি, দ্রুত আর প্রতি সপ্তাহে আপডেট।", fn_b: "APK ডাউনলোড",
    legal: "Cinedor কোনো ফাইল হোস্ট করে না। নাম আর পোস্টার তাদের মালিকদের।",
  };

  const EN = {};
  $$("[data-i18n]").forEach((el) => { if (!(el.dataset.i18n in EN)) EN[el.dataset.i18n] = el.innerHTML; });
  function setLang(lang) {
    const dict = lang === "bn" ? BN : EN;
    document.documentElement.lang = lang;
    $$("[data-i18n]").forEach((el) => { const v = dict[el.dataset.i18n]; if (v != null) el.innerHTML = v; });
    store.set("cinedor-lang", lang);
  }
  const saved = store.get("cinedor-lang");
  const start = saved || ((navigator.language || "").toLowerCase().startsWith("bn") ? "bn" : "en");
  if (start === "bn") setLang("bn"); else document.documentElement.lang = "en";
  $("#lang").addEventListener("click", () => setLang(document.documentElement.lang === "bn" ? "en" : "bn"));

  $$("[data-year]").forEach((el) => (el.textContent = new Date().getFullYear()));

  /* ---------------- Download links ---------------- */
  function setDownload(url) {
    $$("a[data-download]").forEach((a) => {
      a.href = url;
      if (/\.apk$/i.test(url)) a.setAttribute("download", "Cinedor.apk");
    });
  }
  setDownload(FALLBACK);
  fetch(RELEASES, { headers: { Accept: "application/vnd.github+json" } })
    .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
    .then((rel) => {
      const apk = (rel.assets || []).find((a) => /\.apk$/i.test(a.name));
      if (apk) setDownload(apk.browser_download_url);
      const v = (rel.name || rel.tag_name || "").replace(/^Cinedor\s*/i, "").trim();
      if (v) {
        $$("[data-version]").forEach((el) => (el.textContent = v.startsWith("v") ? v : "v" + v));
        $$("[data-version-long]").forEach((el) => (el.textContent = "Version " + v.replace(/^v/, "")));
      }
      if (apk && apk.size) {
        const mb = Math.round(apk.size / 1048576);
        $$("[data-size]").forEach((el) => (el.textContent = mb + " MB · Android 7.0+"));
      }
    })
    .catch(() => {});

  /* ---------------- QR (computers) ---------------- */
  window.addEventListener("load", () => {
    const box = $("#qr");
    if (!box || !window.QRCode || getComputedStyle(box.parentElement).display === "none") return;
    new window.QRCode(box, { text: location.origin + "/", width: 96, height: 96, colorDark: "#12060c", colorLight: "#ffffff", correctLevel: window.QRCode.CorrectLevel.M });
  });

  /* ---------------- Posters ---------------- */
  const PAL = [["#ff8a5b", "#7a1a2a"], ["#6dd5ed", "#1f3c88"], ["#f6d365", "#c0392b"], ["#a18cd1", "#3b1f5f"], ["#43e97b", "#136a3a"], ["#fa709a", "#7b1e3a"], ["#30cfd0", "#330867"], ["#f093fb", "#a3215b"]];
  const grad = (i) => "linear-gradient(160deg," + PAL[i % 8][0] + "," + PAL[i % 8][1] + ")";
  const poster = (it, size, i) => (it && it.poster_path ? "url(" + IMG + size + it.poster_path + ")" : grad(i));
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

  // The background is painted ONCE into a tiny canvas and stretched to the screen.
  // Stretching a small picture gives a free, natural blur, so the phone never has to
  // re-blur anything while scrolling.
  function paintBg(items) {
    const c = $("#bgCanvas");
    // match the screen's shape so the picture is never stretched into streaks
    const W = 200, H = Math.round(200 * Math.min(2.4, Math.max(0.45, window.innerHeight / window.innerWidth)));
    c.width = W; c.height = H;
    const ctx = c.getContext("2d");
    const list = items.slice(0, 24);
    if (!list.length) return;
    const imgs = list.map((it) => { const im = new Image(); im.decoding = "async"; im.src = IMG + "w92" + it.poster_path; return im; });
    let done = 0;
    const draw = () => {
      const src = document.createElement("canvas");
      src.width = W; src.height = H;
      const s = src.getContext("2d");
      s.fillStyle = "#2a0812"; s.fillRect(0, 0, W, H);
      s.save(); s.translate(W / 2, H / 2); s.rotate(-0.17);
      const cw = 30, ch = 45, gap = 5;
      const cols = Math.ceil((W * 1.4) / (cw + gap)) + 1, rows = Math.ceil((H * 1.4) / (ch + gap)) + 1;
      for (let r = 0; r < rows; r++) for (let k = 0; k < cols; k++) {
        const im = imgs[(r * cols + k) % imgs.length];
        if (im.complete && im.naturalWidth) s.drawImage(im, (k - cols / 2) * (cw + gap), (r - rows / 2) * (ch + gap), cw, ch);
      }
      s.restore();
      ctx.clearRect(0, 0, W, H);
      if ("filter" in ctx) { ctx.filter = "blur(6px) saturate(2.1) brightness(0.95) contrast(1.1)"; ctx.drawImage(src, -10, -10, W + 20, H + 20); ctx.filter = "none"; }
      else {
        // no canvas filter (older Safari): shrink further, then stretch = blur
        const t = document.createElement("canvas"); t.width = 34; t.height = Math.round(34 * H / W);
        t.getContext("2d").drawImage(src, 0, 0, t.width, t.height);
        ctx.globalAlpha = 0.95; ctx.drawImage(t, 0, 0, W, H); ctx.globalAlpha = 1;
      }
      c.classList.add("ready");
    };
    let timer = setTimeout(draw, 3500);
    imgs.forEach((im) => {
      const fin = () => { if (++done === imgs.length) { clearTimeout(timer); draw(); } };
      im.onload = fin; im.onerror = fin;
    });
  }

  // fanned cover-flow; the centre card is biggest, the sides tilt away
  function fillFlow(items) {
    const flow = $("#flow");
    const small = window.innerWidth < 640;
    const n = small ? 5 : 7;
    const mid = (n - 1) / 2;
    const spread = small ? Math.min(78, window.innerWidth / 5.2) : Math.min(165, window.innerWidth / 8.5);
    let html = "";
    for (let i = 0; i < n; i++) {
      const d = i - mid;
      const tf = "translateX(" + d * spread + "px) translateY(" + Math.abs(d) * (small ? 14 : 22) + "px) rotateY(" + (-d * 14) + "deg) rotateZ(" + d * 3 + "deg) scale(" + (1 - Math.abs(d) * 0.07) + ")";
      html += '<div class="fp" style="z-index:' + (10 - Math.abs(d)) + ";transform:" + tf + ";background-image:" + poster(items[i], small ? "w185" : "w342", i) + '"></div>';
    }
    flow.innerHTML = html;
  }

  function fillRail(items) {
    const top = items.slice(0, 10);
    if (!top.length) { $("#trending").style.display = "none"; return; }
    $("#rail").innerHTML = top.map((it, i) => {
      const title = esc(it.title || it.name || "");
      const year = (it.release_date || it.first_air_date || "").slice(0, 4);
      const type = it.media_type === "tv" ? "Series" : "Movie";
      const rate = it.vote_average ? it.vote_average.toFixed(1) : "";
      return '<a class="card" href="#get" aria-label="' + title + '"><div class="poster" style="background-image:' + grad(i) + '"><img src="' + IMG + (window.innerWidth < 640 ? "w185" : "w342") + it.poster_path + '" alt="" loading="lazy" decoding="async">' +
        '<span class="num glass-chip">' + (i + 1) + "</span>" +
        (rate ? '<span class="rate glass-chip"><svg class="ic"><use href="#i-star"/></svg>' + rate + "</span>" : "") +
        "</div><h4>" + title + "</h4><small>" + type + (year ? " · " + year : "") + "</small></a>";
    }).join("");
  }

  let posters = [];
  fillFlow([]);
  Promise.all([1, 2].map((p) => fetch(TMDB + p).then((r) => (r.ok ? r.json() : { results: [] })).catch(() => ({ results: [] }))))
    .then((pages) => {
      posters = pages.flatMap((p) => p.results || []).filter((x) => x.poster_path && (x.media_type === "movie" || x.media_type === "tv"));
      if (!posters.length) { fillRail([]); return; }
      paintBg(posters.slice(10).concat(posters.slice(0, 10)));
      fillFlow(posters);
      fillRail(posters);
      const bd = posters.find((x) => x.backdrop_path);
      if (bd) $("[data-backdrop]").style.backgroundImage = "url(" + IMG + "w780" + bd.backdrop_path + ")";
    });
  let rt;
  let lastW = window.innerWidth;
  window.addEventListener("resize", () => {
    // phones fire resize when the address bar hides; only redraw when the width really changes
    if (window.innerWidth === lastW) return;
    lastW = window.innerWidth;
    clearTimeout(rt); rt = setTimeout(() => fillFlow(posters), 150);
  });

  $$("[data-scroll]").forEach((b) => b.addEventListener("click", () => {
    const rail = $("#rail");
    rail.scrollBy({ left: Number(b.dataset.scroll) * rail.clientWidth * 0.8, behavior: "smooth" });
  }));

  /* ---------------- Glass tilts toward the pointer ---------------- */
  if (window.matchMedia("(hover: hover)").matches && !document.documentElement.classList.contains("lite")) {
    $$(".feat, .steps li").forEach((el) => {
      el.addEventListener("pointermove", (e) => {
        const r = el.getBoundingClientRect();
        const x = (e.clientX - r.left) / r.width - 0.5, y = (e.clientY - r.top) / r.height - 0.5;
        el.style.transform = "perspective(900px) rotateX(" + (-y * 5) + "deg) rotateY(" + (x * 6) + "deg) translateY(-4px)";
      });
      el.addEventListener("pointerleave", () => { el.style.transform = ""; });
    });
  }

  /* ---------------- Reveal + tab bar highlight ---------------- */
  if ("IntersectionObserver" in window) {
    const els = $$(".sec-head, .feat, .steps li, .faq details, .final, .shelf, .stats");
    els.forEach((el) => el.classList.add("reveal"));
    const io = new IntersectionObserver((entries) => {
      entries.forEach((en) => { if (en.isIntersecting) { en.target.classList.add("in"); io.unobserve(en.target); } });
    }, { rootMargin: "0px 0px -8% 0px" });
    els.forEach((el) => io.observe(el));

    const tabs = $$(".tabbar a[href^='#']");
    const spy = new IntersectionObserver((entries) => {
      entries.forEach((en) => {
        if (!en.isIntersecting) return;
        const id = en.target.id === "get" ? "top" : en.target.id;
        tabs.forEach((t) => t.classList.toggle("on", t.getAttribute("href") === "#" + id));
      });
    }, { rootMargin: "-45% 0px -50% 0px" });
    ["get", "trending", "features", "install", "faq"].forEach((id) => { const el = document.getElementById(id); if (el) spy.observe(el); });
  }

  /* ---------------- Telegram / Facebook from the Admin app ---------------- */
  fetch(CONFIG)
    .then((r) => (r.ok ? r.json() : Promise.reject()))
    .then((doc) => {
      const json = doc.fields && doc.fields.json && doc.fields.json.stringValue;
      if (!json) return;
      const c = JSON.parse(json).community || {};
      const box = $("[data-social]");
      const add = (url, label, icon) => {
        if (!url || !/^https?:\/\//.test(url)) return;
        const a = document.createElement("a");
        a.href = url; a.target = "_blank"; a.rel = "noopener";
        a.innerHTML = '<svg class="ic"><use href="#i-' + icon + '"/></svg>' + label;
        box.appendChild(a);
      };
      add(c.buttonUrl, "Telegram", "telegram");
      add(c.secondaryUrl, "Facebook", "facebook");
    })
    .catch(() => {});
})();
