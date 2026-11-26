// Cinedor website: points every "Download" button at the newest APK and shows its version/size.
(function () {
  const RELEASES = "https://api.github.com/repos/contactbanglascreen-bit/Baizid-/releases/latest";
  const FALLBACK = "https://github.com/contactbanglascreen-bit/Baizid-/releases/latest";
  const CONFIG = "https://firestore.googleapis.com/v1/projects/cinedor-60aac/databases/(default)/documents/config/app" +
    "?key=AIzaSyCddEpMsakf5vHeLlHuJLMPUCC50uTYOpY";

  document.querySelectorAll("[data-year]").forEach((el) => (el.textContent = new Date().getFullYear()));

  function setDownload(url) {
    document.querySelectorAll("a[data-download]").forEach((a) => {
      a.href = url;
      if (url.endsWith(".apk")) a.setAttribute("download", "Cinedor.apk");
    });
  }
  setDownload(FALLBACK);

  fetch(RELEASES, { headers: { Accept: "application/vnd.github+json" } })
    .then((r) => (r.ok ? r.json() : Promise.reject(r.status)))
    .then((rel) => {
      const apk = (rel.assets || []).find((a) => /\.apk$/i.test(a.name));
      if (apk) setDownload(apk.browser_download_url);
      const version = (rel.name || rel.tag_name || "").replace(/^Cinedor\s*/i, "");
      if (version) document.querySelectorAll("[data-version]").forEach((el) => (el.textContent = "Version " + version));
      if (apk && apk.size) {
        const mb = Math.round(apk.size / 1048576);
        document.querySelectorAll("[data-size]").forEach((el) => (el.textContent = mb + " MB · Android 7.0+"));
        document.querySelectorAll("[data-size-mb]").forEach((el) => (el.textContent = mb));
      }
    })
    .catch(() => {});

  // Telegram / Facebook links set in the Cinedor Admin app (Join us popup).
  fetch(CONFIG)
    .then((r) => (r.ok ? r.json() : Promise.reject()))
    .then((doc) => {
      const json = doc.fields && doc.fields.json && doc.fields.json.stringValue;
      if (!json) return;
      const c = JSON.parse(json).community || {};
      const box = document.querySelector("[data-social]");
      const add = (url, label) => {
        if (!url || !/^https?:\/\//.test(url)) return;
        const a = document.createElement("a");
        a.href = url; a.target = "_blank"; a.rel = "noopener"; a.textContent = label;
        box.appendChild(a);
      };
      add(c.buttonUrl, "Telegram");
      add(c.secondaryUrl, "Facebook");
    })
    .catch(() => {});
})();
