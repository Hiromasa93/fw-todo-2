(() => {
    'use strict';

    const root = document.documentElement;
    const button = document.getElementById('theme-toggle');
    const media = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
    const valid = value => value === 'light' || value === 'dark';
    let preference = null;
    try { preference = localStorage.getItem('theme'); } catch (_) { /* Use OS preference. */ }
    if (!valid(preference)) preference = null;

    const apply = () => {
        const theme = preference || (media && media.matches ? 'dark' : 'light');
        root.setAttribute('data-theme', theme);
        if (button) {
            const label = theme === 'dark' ? 'ライトモードに切り替え' : 'ダークモードに切り替え';
            button.setAttribute('aria-label', label);
            button.setAttribute('title', label);
            button.setAttribute('aria-pressed', String(theme === 'dark'));
        }
    };

    if (button) {
        button.addEventListener('click', () => {
            preference = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
            try { localStorage.setItem('theme', preference); } catch (_) { /* Keep this page usable. */ }
            apply();
        });
        button.hidden = false;
    }
    if (media) media.addEventListener('change', apply);
    // Synchronize other tabs, including returning to OS preference when storage is cleared.
    window.addEventListener('storage', event => {
        if (event.key !== 'theme' && event.key !== null) return;
        try {
            if (event.storageArea !== localStorage) return;
        } catch (_) { return; }
        preference = valid(event.newValue) ? event.newValue : null;
        apply();
    });
    apply();
})();
