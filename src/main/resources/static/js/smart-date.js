document.querySelectorAll('[data-smart-date]').forEach(picker => {
    const input = picker.querySelector('input');
    const date = picker.closest('form').querySelector('[name="dueDate"]');
    const preview = picker.querySelector('[role="status"]');
    let timer;
    let request;
    let generation = 0;
    let composing = false;

    function cancel() {
        clearTimeout(timer);
        request?.abort();
        generation++;
    }

    function schedule() {
        cancel();
        const text = input.value.trim();
        preview.textContent = '';
        input.removeAttribute('aria-invalid');
        input.setCustomValidity(text ? '日付の解析が完了するまでお待ちください。' : '');
        if (!text || composing) return;
        const current = generation;
        preview.textContent = '日付を解析中…';
        timer = setTimeout(async () => {
            request = new AbortController();
            const url = new URL(picker.dataset.parseUrl, window.location.href);
            url.searchParams.set('input', text);
            try {
                const response = await fetch(url, { signal: request.signal, headers: { Accept: 'application/json' } });
                const result = await response.json();
                if (current !== generation) return;
                if (!response.ok || !result.success) {
                    throw new Error(response.status === 400 && result.message
                        ? result.message : '日付を取得できませんでした。もう一度入力してください。');
                }
                if (typeof result.date !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(result.date)) {
                    throw new Error('日付を取得できませんでした。もう一度入力してください。');
                }
                date.value = result.date;
                input.setCustomValidity('');
                preview.textContent = `期限：${result.date}（反映済み）`;
            } catch (error) {
                if (current !== generation || error.name === 'AbortError') return;
                const message = error instanceof TypeError || error instanceof SyntaxError
                    ? '日付を取得できませんでした。もう一度入力してください。' : error.message;
                input.setCustomValidity(message);
                input.setAttribute('aria-invalid', 'true');
                preview.textContent = message;
            }
        }, 300);
    }

    input.addEventListener('input', schedule);
    input.addEventListener('compositionstart', () => { composing = true; schedule(); });
    input.addEventListener('compositionend', () => { composing = false; schedule(); });
    date.addEventListener('input', () => {
        cancel();
        input.value = '';
        input.setCustomValidity('');
        input.removeAttribute('aria-invalid');
        preview.textContent = '';
    });
});
