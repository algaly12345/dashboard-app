/* Estate Control — small interaction layer, framework-free. */
(function () {
  "use strict";

  // ---- Count-up animation for KPI numbers ----
  function animateValue(el, end) {
    var start = 0;
    var duration = 900;
    var startTime = null;
    function step(ts) {
      if (!startTime) startTime = ts;
      var progress = Math.min((ts - startTime) / duration, 1);
      var eased = 1 - Math.pow(1 - progress, 3);
      var value = Math.round(start + (end - start) * eased);
      el.textContent = value.toLocaleString();
      if (progress < 1) requestAnimationFrame(step);
      else el.textContent = end.toLocaleString();
    }
    requestAnimationFrame(step);
  }

  function initCountUp() {
    document.querySelectorAll(".kpi-value").forEach(function (el) {
      var raw = el.textContent.trim().replace(/,/g, "");
      var end = parseInt(raw, 10);
      if (!isNaN(end)) animateValue(el, end);
    });
  }

  // ---- Animated bar fills (they start at width:0 in CSS) ----
  function initBars() {
    document.querySelectorAll(".bar-fill[style]").forEach(function (el) {
      var target = el.style.width;
      el.style.width = "0%";
      requestAnimationFrame(function () {
        requestAnimationFrame(function () {
          el.style.width = target;
        });
      });
    });
  }

  // ---- Ripple effect on primary buttons / icon buttons ----
  function initRipples() {
    document.querySelectorAll(".btn-gold, .btn-icon").forEach(function (btn) {
      btn.style.position = btn.style.position || "relative";
      btn.style.overflow = "hidden";
      btn.addEventListener("click", function (e) {
        var rect = btn.getBoundingClientRect();
        var ripple = document.createElement("span");
        var size = Math.max(rect.width, rect.height);
        ripple.className = "ripple";
        ripple.style.width = ripple.style.height = size + "px";
        ripple.style.left = (e.clientX - rect.left - size / 2) + "px";
        ripple.style.top = (e.clientY - rect.top - size / 2) + "px";
        btn.appendChild(ripple);
        setTimeout(function () { ripple.remove(); }, 650);
      });
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    initCountUp();
    initBars();
    initRipples();
  });
})();
