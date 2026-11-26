(() => {
    'use strict';
    const root = document.getElementById('keyboard-shortcuts');
    if (!root || root.dataset.initialized) return;
    root.dataset.initialized = 'true';
    const dialog = document.getElementById('shortcut-help');
    const search = document.querySelector('[data-shortcut-search]');
    const saveForm = document.querySelector('form[data-shortcut-save]');
    let previousFocus;
    let loaded = false;
    let loading = false;

    async function showHelp() {
        if (dialog.open) return;
        previousFocus = document.activeElement;
        dialog.showModal();
        if (loaded || loading) return;
        loading = true;
        const status = root.querySelector('[data-shortcut-status]');
        status.textContent = '読み込み中…';
        try {
            const response = await fetch(root.dataset.apiUrl, { headers: { Accept: 'application/json' } });
            if (!response.ok) throw new Error('Shortcut API failed');
            const shortcuts = await response.json();
            if (!Array.isArray(shortcuts) || shortcuts.some(item => !item ||
                    !['keys', 'description', 'scope'].every(key => typeof item[key] === 'string'))) {
                throw new Error('Invalid shortcut list');
            }
            const list = root.querySelector('[data-shortcut-list]');
            list.replaceChildren();
            shortcuts.forEach(shortcut => {
                const item = document.createElement('li');
                const key = document.createElement('kbd');
                key.textContent = shortcut.keys;
                item.append(key, `：${shortcut.description}（${shortcut.scope}）`);
                list.append(item);
            });
            status.textContent = '';
            loaded = true;
        } catch (error) {
            status.textContent = '一覧を取得できませんでした。閉じて再度開くと再試行します。';
        } finally {
            loading = false;
        }
    }

    root.querySelector('[data-shortcut-help]').addEventListener('click', showHelp);
    root.querySelector('[data-shortcut-close]').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => {
        if (previousFocus?.isConnected) previousFocus.focus();
    });

    function isInput(element) {
        return element instanceof Element && (element.isContentEditable ||
            Boolean(element.closest('input, textarea, select, [role="textbox"], [role="combobox"]')));
    }

    // All application shortcuts are dispatched through this single global listener.
    document.addEventListener('keydown', event => {
        if (event.isComposing || event.keyCode === 229 || event.defaultPrevented) return;
        if (event.key === 'Escape') {
            if (dialog.open) {
                event.preventDefault();
                dialog.close();
            } else if (isInput(document.activeElement)) {
                event.preventDefault();
                document.activeElement.blur();
            }
            return;
        }
        if (dialog.open || isInput(event.target) || isInput(document.activeElement)) return;
        if (event.metaKey) return;
        let action;
        // Chrome reserves Ctrl+N and may never dispatch it to the page.
        // Alt+N is the primary binding; retain Ctrl+N where it is delivered.
        // Exclude Ctrl+Alt (AltGr) and shifted variants.
        if (event.altKey) {
            if (!event.ctrlKey && !event.shiftKey && event.key.toLowerCase() === 'n') {
                action = () => window.location.assign(root.dataset.newUrl);
            }
        } else if (event.ctrlKey && !event.shiftKey) {
            switch (event.key.toLowerCase()) {
                case 'n': action = () => window.location.assign(root.dataset.newUrl); break;
                case 's':
                    if (saveForm) action = () => saveForm.requestSubmit(saveForm.querySelector('[data-shortcut-submit]'));
                    break;
                case 'f': action = () => search ? search.focus() : window.location.assign(root.dataset.searchUrl); break;
            }
        } else if (!event.ctrlKey) {
            if (event.key === '?') {
                action = showHelp;
            } else if (!event.shiftKey) {
                const rows = Array.from(document.querySelectorAll('[data-shortcut-task]'));
                const row = event.target instanceof Element ? event.target.closest('[data-shortcut-task]') : null;
                // Preserve native Enter behavior on links and buttons inside a row.
                if (event.key === 'Enter' && row && event.target === row) {
                    action = () => row.querySelector('[data-shortcut-detail]').click();
                } else if (['ArrowDown', 'ArrowRight', 'ArrowUp', 'ArrowLeft'].includes(event.key) && rows.length) {
                    const backwards = event.key === 'ArrowUp' || event.key === 'ArrowLeft';
                    const current = rows.indexOf(row);
                    const next = current < 0 ? (backwards ? rows.length - 1 : 0)
                        : Math.max(0, Math.min(rows.length - 1, current + (backwards ? -1 : 1)));
                    action = () => rows[next].focus();
                }
            }
        }
        if (!action) return;
        event.preventDefault();
        if (!event.repeat) action();
    });

    if (search && new URLSearchParams(window.location.search).get('focus') === 'search') search.focus();
})();
