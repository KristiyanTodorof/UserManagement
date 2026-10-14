(function () {
    var root = document.documentElement;
    var pref = root.getAttribute('data-theme') || 'system';
    var mq = window.matchMedia('(prefers-color-scheme: dark)');

    function apply() {
        var dark = pref === 'dark' || (pref === 'system' && mq.matches);
        root.setAttribute('data-bs-theme', dark ? 'dark' : 'light');   // Bootstrap's own dark mode
    }

    apply();
    if (mq.addEventListener) mq.addEventListener('change', apply);
})();