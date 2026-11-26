document.querySelectorAll('[data-tag-picker]').forEach((picker) => {
    const input = picker.querySelector('[role="combobox"]');
    const list = picker.querySelector('[role="listbox"]');
    const chips = picker.querySelector('[data-tag-chips]');
    const status = picker.querySelector('[role="status"]');
    const add = picker.querySelector('[data-add-tag]');
    const form = picker.closest('form');
    let saving = false;
    let timer;
    let request;
    let generation = 0;
    let active = -1;
    let candidates = [];
    let composing = false;

    function close() {
        clearTimeout(timer);
        request?.abort();
        generation++;
        list.hidden = true;
        list.replaceChildren();
        input.setAttribute('aria-expanded', 'false');
        input.removeAttribute('aria-activedescendant');
        active = -1;
        candidates = [];
    }

    function select(tag) {
        if ([...chips.querySelectorAll('input')].some((item) => item.value === String(tag.id))) {
            input.value = '';
            close();
            add.disabled = true;
            status.textContent = `${tag.name}は選択済みです`;
            return;
        }
        const chip = document.createElement('span');
        chip.className = 'tag-chip';
        const label = document.createElement('span');
        label.textContent = tag.name;
        const remove = document.createElement('button');
        remove.type = 'button';
        remove.dataset.removeTag = '';
        remove.setAttribute('aria-label', `${tag.name}を削除`);
        remove.textContent = '×';
        const hidden = document.createElement('input');
        hidden.type = 'hidden';
        hidden.name = 'tagIds';
        hidden.value = tag.id;
        chip.append(label, remove, hidden);
        chips.append(chip);
        input.value = '';
        add.disabled = true;
        close();
        status.textContent = `${tag.name}を選択しました`;
        input.focus();
    }

    async function addTag() {
        if (saving || composing) return false;
        const name = input.value.trim();
        if (!name) return true;
        close();
        saving = true;
        input.readOnly = true;
        add.disabled = true;
        status.textContent = 'タグを登録中…';
        try {
            const response = await fetch(new URL(picker.dataset.createUrl, window.location.href), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({ name })
            });
            if (!response.ok) throw new Error('Tag creation failed');
            select(await response.json());
            return true;
        } catch (error) {
            status.textContent = 'タグを登録できませんでした。入力内容を確認して再度追加してください。';
            return false;
        } finally {
            saving = false;
            input.readOnly = false;
            add.disabled = !input.value.trim();
        }
    }

    function schedule() {
        if (saving) return;
        close();
        add.disabled = composing || !input.value.trim();
        status.textContent = '';
        const query = input.value.trim();
        if (!query || composing) return;
        const current = generation;
        timer = setTimeout(async () => {
            request = new AbortController();
            status.textContent = '検索中…';
            try {
                const url = new URL(picker.dataset.searchUrl, window.location.href);
                url.searchParams.set('q', query);
                const response = await fetch(url, { signal: request.signal, headers: { Accept: 'application/json' } });
                if (!response.ok) throw new Error('Tag search failed');
                const tags = await response.json();
                if (current !== generation) return;
                const selected = new Set([...chips.querySelectorAll('input')].map((item) => item.value));
                candidates = tags.filter((tag) => !selected.has(String(tag.id)));
                candidates.forEach((tag, index) => {
                    const option = document.createElement('li');
                    option.id = `tag-option-${index}`;
                    option.setAttribute('role', 'option');
                    option.setAttribute('aria-selected', 'false');
                    option.textContent = tag.name;
                    option.addEventListener('mousedown', (event) => event.preventDefault());
                    option.addEventListener('click', () => select(tag));
                    list.append(option);
                });
                list.hidden = candidates.length === 0;
                input.setAttribute('aria-expanded', String(candidates.length > 0));
                status.textContent = candidates.length ? `${candidates.length}件の候補があります` : '候補がありません。「タグを追加」で登録できます。';
            } catch (error) {
                if (error.name !== 'AbortError' && current === generation) {
                    status.textContent = 'タグ候補を取得できませんでした。再入力してください。';
                }
            }
        }, 300);
    }

    input.addEventListener('input', schedule);
    add.addEventListener('click', addTag);
    form.addEventListener('submit', async (event) => {
        if (!saving && !input.value.trim()) return;
        event.preventDefault();
        if (saving || composing) return;
        const submitter = event.submitter;
        if (await addTag()) form.requestSubmit(submitter);
    });
    input.addEventListener('compositionstart', () => { composing = true; add.disabled = true; close(); });
    input.addEventListener('compositionend', () => { composing = false; schedule(); });
    input.addEventListener('blur', close);
    input.addEventListener('keydown', (event) => {
        if (event.isComposing) return;
        if (event.key === 'Escape') { close(); status.textContent = ''; }
        if (event.key === 'Enter') {
            event.preventDefault();
            if (saving) return;
            if (active >= 0) select(candidates[active]);
            else addTag();
        }
        if (!candidates.length || !['ArrowDown', 'ArrowUp'].includes(event.key)) return;
        event.preventDefault();
        active = active < 0 ? (event.key === 'ArrowDown' ? 0 : candidates.length - 1)
            : (active + (event.key === 'ArrowDown' ? 1 : -1) + candidates.length) % candidates.length;
        [...list.children].forEach((option, index) => option.setAttribute('aria-selected', String(index === active)));
        input.setAttribute('aria-activedescendant', list.children[active].id);
        list.children[active].scrollIntoView({ block: 'nearest' });
    });
    chips.addEventListener('click', (event) => {
        const button = event.target.closest('[data-remove-tag]');
        if (!button) return;
        button.closest('.tag-chip').remove();
        input.focus();
        schedule();
    });
});
