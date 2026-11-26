// Cinedor website
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
    h1a: "আনলিমিটেড মুভি,", h1b: "সিরিজ আর আরও অনেক কিছু।", h1c: "সব এক অ্যাপে।",
    lead: "হলিউড, বলিউড, বাংলা, কোরিয়ান ড্রামা, অ্যানিমে আর লাইভ খেলা দেখুন HD তে — যেকোনো ভাষার সাবটাইটেল আর অফলাইনে দেখার জন্য ডাউনলোড সহ।",
    get_on: "ডাউনলোড করুন", safe: "অফিসিয়াল ও নিরাপদ",
    qr_t: "কম্পিউটারে আছেন?", qr_s: "ফোনের ক্যামেরা দিয়ে স্ক্যান করে ডাউনলোড করুন।",
    f1t: "নতুন এপিসোড এসেছে", f1s: "এখনই দেখতে চাপুন", f2t: "ডাউনলোড হয়েছে · 1080p", f2s: "অফলাইনে দেখার জন্য রেডি",
    st1: "আল্ট্রা HD কোয়ালিটি", st2: "ভাষা", st3: "সাবটাইটেলের ভাষা", st4: "একদম ফ্রি",
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
  $$("[data-i18n]").forEach((el) => { EN[el.dataset.i18n] = el.innerHTML; });

  function setLang(lang) {
    const dict = lang === "bn" ? BN : EN;
    document.documentElement.lang = lang;
    $$("[data-i18n]").forEach((el) => {
      const v = dict[el.dataset.i18n];
      if (v != null) el.innerHTML = v;
    });
    store.set("cinedor-lang", lang);
  }
  const savedLang = store.get("cinedor-lang");
  const startLang = savedLang || ((navigator.language || "").toLowerCase().startsWith("bn") ? "bn" : "en");
  if (startLang === "bn") setLang("bn"); else document.documentElement.lang = "en";
  $("#lang").addEventListener("click", () => setLang(document.documentElement.lang === "bn" ? "en" : "bn"));

  /* ---------------- Nav ---------------- */
  const nav = $("#nav");
  const burger = $("#burger");
  const sticky = $("#sticky");
  function onScroll() {
    const y = window.scrollY;
    nav.classList.toggle("solid", y > 10 || nav.classList.contains("open"));
    sticky.classList.toggle("show", y > window.innerHeight * 0.8 && (window.innerHeight + y) < document.body.scrollHeight - 200);
  }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
  function setMenu(open) {
    nav.classList.toggle("open", open);
    burger.setAttribute("aria-expanded", String(open));
    burger.innerHTML = '<svg class="ic"><use href="#i-' + (open ? "close" : "menu") + '"/></svg>';
    onScroll();
  }
  burger.addEventListener("click", () => setMenu(!nav.classList.contains("open")));
  $$("#menu a").forEach((a) => a.addEventListener("click", () => setMenu(false)));

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
        $$("[data-size-short]").forEach((el) => (el.textContent = "Free · " + mb + " MB"));
      }
    })
    .catch(() => {});

  /* ---------------- QR code (for computers) ---------------- */
  function makeQr() {
    const box = $("#qr");
    if (!box || !window.QRCode) return;
    box.innerHTML = "";
    new window.QRCode(box, { text: location.origin + "/", width: 128, height: 128, colorDark: "#0b0708", colorLight: "#ffffff", correctLevel: window.QRCode.CorrectLevel.M });
  }
  window.addEventListener("load", makeQr);

  /* ---------------- Posters ---------------- */
  const PLACEHOLDER = [
    ["#ff8a5b", "#7a1a2a"], ["#6dd5ed", "#1f3c88"], ["#f6d365", "#c0392b"], ["#a18cd1", "#3b1f5f"],
    ["#43e97b", "#136a3a"], ["#fa709a", "#7b1e3a"], ["#30cfd0", "#330867"], ["#f093fb", "#a3215b"],
  ];
  const grad = (i) => "linear-gradient(160deg," + PLACEHOLDER[i % 8][0] + "," + PLACEHOLDER[i % 8][1] + ")";

  function fillWall(items) {
    $$(".wall-row").forEach((row, r) => {
      const list = [];
      for (let i = 0; i < 14; i++) {
        const it = items.length ? items[(i * 4 + r * 3) % items.length] : null;
        list.push(it ? "url(" + IMG + "w185" + it.poster_path + ")" : grad(i + r));
      }
      const html = list.concat(list).map((bg) => '<div class="wp" style="background-image:' + bg + '"></div>').join("");
      row.innerHTML = html;
    });
  }
  function fillPhone(items) {
    $$("[data-phone-row]").forEach((row, r) => {
      let html = "";
      for (let i = 0; i < 3; i++) {
        const it = items[1 + r * 3 + i];
        html += '<div class="pp" style="background-image:' + (it ? "url(" + IMG + "w185" + it.poster_path + ")" : grad(i + r * 3)) + '"></div>';
      }
      row.innerHTML = html;
    });
    const top = items.find((x) => x.backdrop_path);
    if (top) {
      $("[data-backdrop]").style.backgroundImage = "url(" + IMG + "w500" + top.backdrop_path + ")";
      $("[data-hero-title]").textContent = top.title || top.name || "";
    }
    const second = items.filter((x) => x.backdrop_path)[1];
    if (second) $("[data-backdrop2]").style.backgroundImage = "url(" + IMG + "w780" + second.backdrop_path + ")";
  }
  function esc(s) { return String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
  function fillRail(items) {
    const rail = $("#rail");
    const top = items.slice(0, 10);
    if (!top.length) { rail.classList.add("empty"); $("#trending").style.display = "none"; return; }
    rail.innerHTML = top.map((it, i) => {
      const title = esc(it.title || it.name || "");
      const year = (it.release_date || it.first_air_date || "").slice(0, 4);
      const type = it.media_type === "tv" ? "Series" : "Movie";
      const rate = it.vote_average ? it.vote_average.toFixed(1) : "";
      return '<a class="rank" href="#get" aria-label="' + title + '">' +
        '<span class="n">' + (i + 1) + "</span>" +
        '<div class="poster" style="background-image:url(' + IMG + "w342" + it.poster_path + ')">' +
        (rate ? '<span class="rate"><svg class="ic"><use href="#i-star"/></svg>' + rate + "</span>" : "") +
        "</div><h4>" + title + "</h4><small>" + type + (year ? " · " + year : "") + "</small></a>";
    }).join("");
  }
  fillWall([]);
  fillPhone([]);
  Promise.all([1, 2].map((p) => fetch(TMDB + p).then((r) => (r.ok ? r.json() : { results: [] })).catch(() => ({ results: [] }))))
    .then((pages) => {
      const items = pages.flatMap((p) => p.results || []).filter((x) => x.poster_path && (x.media_type === "movie" || x.media_type === "tv"));
      if (!items.length) { fillRail([]); return; }
      fillWall(items);
      fillPhone(items);
      fillRail(items);
    });

  $$("[data-scroll]").forEach((b) => b.addEventListener("click", () => {
    const rail = $("#rail");
    rail.scrollBy({ left: Number(b.dataset.scroll) * rail.clientWidth * 0.8, behavior: "smooth" });
  }));

  /* ---------------- Tile glow follows the pointer ---------------- */
  $$(".tile").forEach((t) => t.addEventListener("pointermove", (e) => {
    const r = t.getBoundingClientRect();
    t.style.setProperty("--mx", (e.clientX - r.left) + "px");
    t.style.setProperty("--my", (e.clientY - r.top) + "px");
  }));

  /* ---------------- Reveal on scroll ---------------- */
  const revealEls = $$(".sec-head, .tile, .steps li, .faq details, .dev-card, .final-in, .rail");
  if ("IntersectionObserver" in window) {
    revealEls.forEach((el) => el.classList.add("reveal"));
    const io = new IntersectionObserver((entries) => {
      entries.forEach((en) => { if (en.isIntersecting) { en.target.classList.add("in"); io.unobserve(en.target); } });
    }, { rootMargin: "0px 0px -8% 0px" });
    revealEls.forEach((el) => io.observe(el));
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
