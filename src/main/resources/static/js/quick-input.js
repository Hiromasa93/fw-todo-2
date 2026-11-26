(() => {
    const bar = document.querySelector('[data-quick-input]');
    if (!bar) return;
    const form = bar.querySelector('form');
    const input = form.querySelector('[name="title"]');
    const button = form.querySelector('button');
    const error = form.querySelector('[role="alert"]');
    const status = form.querySelector('[role="status"]');
    let saving = false;
    let composing = false;
    let revision = 0;

    function clearError() {
        error.textContent = '';
        error.hidden = true;
        input.removeAttribute('aria-invalid');
    }

    function showError(message) {
        error.textContent = message;
        error.hidden = false;
        input.setAttribute('aria-invalid', 'true');
    }

    function insertTodo(todo) {
        const row = bar.querySelector('[data-quick-row]').content.firstElementChild.cloneNode(true);
        const url = `${bar.dataset.todoBase}/${encodeURIComponent(todo.id)}`;
        const checkbox = row.querySelector('.todo-select');
        checkbox.value = todo.id;
        checkbox.setAttribute('aria-label', `${todo.title}を選択`);
        const detail = row.querySelector('[data-shortcut-detail]');
        detail.textContent = todo.title;
        detail.href = url;
        row.querySelector('[data-quick-category]').textContent = todo.category;
        row.querySelector('[data-quick-edit]').href = `${url}/edit`;
        row.querySelector('[data-quick-delete]').href = `${url}/delete`;
        const pin = row.querySelector('[data-quick-pin]');
        pin.action = `${url}/pin`;
        const bulkForm = document.getElementById('bulk-delete-form');
        bulkForm.querySelectorAll('input[type="hidden"]').forEach(field => pin.append(field.cloneNode(true)));
        const csrf = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfParameter = document.querySelector('meta[name="_csrf_parameter"]')?.content;
        if (csrf && csrfParameter && !Array.from(pin.elements).some(field => field.name === csrfParameter)) {
            const field = document.createElement('input');
            field.type = 'hidden'; field.name = csrfParameter; field.value = csrf;
            pin.append(field);
        }
        document.querySelector('.todo-table tbody').prepend(row);
        document.querySelector('.todo-table').hidden = false;
        bulkForm.hidden = false;
        const empty = document.querySelector('[data-empty-todos]');
        if (empty) empty.hidden = true;
        row.addEventListener('animationend', () => row.classList.remove('todo-row-enter'), { once: true });
        document.dispatchEvent(new Event('todo:added'));
    }

    input.addEventListener('input', () => { revision++; clearError(); status.textContent = ''; });
    input.addEventListener('compositionstart', () => { composing = true; });
    input.addEventListener('compositionend', () => { composing = false; });
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || composing) return;
        clearError();
        const title = input.value.trim();
        if (!title) { showError('やることを入力してください'); return; }
        if (title.length > 255) { showError('やることは255文字以内で入力してください'); return; }
        saving = true;
        button.disabled = true;
        form.setAttribute('aria-busy', 'true');
        const sentRevision = revision;
        status.textContent = '追加中…';
        try {
            const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
            const token = document.querySelector('meta[name="_csrf"]')?.content;
            const header = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (!token || !header) throw new Error('画面を再読み込みしてから追加してください。');
            headers[header] = token;
            const response = await fetch(bar.dataset.apiUrl, {
                method: 'POST', credentials: 'same-origin', headers, body: JSON.stringify({ title })
            });
            if (!response.ok) {
                if (response.status === 403) throw new Error('画面の有効期限が切れました。再読み込みしてから追加してください。');
                const body = await response.json().catch(() => ({}));
                const messages = response.status === 400 && Array.isArray(body.errors)
                    ? body.errors.map(item => item.message).filter(message => typeof message === 'string') : [];
                throw new Error(messages.join('、') || '追加できませんでした。もう一度お試しください。');
            }
            const todo = await response.json();
            insertTodo(todo);
            if (revision === sentRevision) input.value = '';
            status.textContent = '追加しました。';
            input.focus();
        } catch (failure) {
            status.textContent = '';
            if (revision === sentRevision) {
                showError(failure instanceof TypeError || failure instanceof SyntaxError
                    ? '通信に失敗しました。もう一度お試しください。' : failure.message);
            }
        } finally {
            saving = false;
            button.disabled = false;
            form.removeAttribute('aria-busy');
        }
    });
})();
