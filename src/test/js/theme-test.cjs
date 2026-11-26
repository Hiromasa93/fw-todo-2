// Run with: node src/test/js/theme-test.cjs (no additional dependencies).
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const resources = path.resolve(__dirname, '../../main/resources');
const fragment = fs.readFileSync(path.join(resources, 'templates/fragments/theme.html'), 'utf8');
const bootstrap = fragment.match(/<script>([\s\S]*?)<\/script>/)[1];
const behavior = fs.readFileSync(path.join(resources, 'static/js/theme.js'), 'utf8');

function page({saved = null, dark = false, blocked = false, noMedia = false} = {}) {
    const attributes = {};
    const buttonAttributes = {};
    const events = {};
    let stored = saved;
    let mediaChange;
    let click;
    const media = {matches: dark, addEventListener: (_, listener) => { mediaChange = listener; }};
    const button = {
        hidden: true,
        setAttribute: (name, value) => { buttonAttributes[name] = value; },
        addEventListener: (_, listener) => { click = listener; }
    };
    const storage = {
        getItem: () => stored,
        setItem: (key, value) => { assert.equal(key, 'theme'); stored = value; }
    };
    const context = vm.createContext({
        document: {
            documentElement: {
                setAttribute: (name, value) => { attributes[name] = value; },
                getAttribute: name => attributes[name]
            },
            getElementById: () => button
        },
        window: {
            matchMedia: noMedia ? undefined : query => {
                assert.equal(query, '(prefers-color-scheme: dark)');
                return media;
            },
            addEventListener: (name, listener) => { events[name] = listener; }
        }
    });
    Object.defineProperty(context, 'localStorage', {get() {
        if (blocked) throw new Error('Storage disabled');
        return storage;
    }});
    vm.runInContext(bootstrap, context);
    const initialTheme = attributes['data-theme'];
    vm.runInContext(behavior, context);
    return {
        initialTheme, button, buttonAttributes,
        theme: () => attributes['data-theme'],
        saved: () => stored,
        click: () => click(),
        os: dark => { media.matches = dark; mediaChange(); },
        storage: (value, key = 'theme', storageArea = storage) => {
            stored = value;
            events.storage({key, newValue: value, storageArea});
        }
    };
}

for (const options of [
    {dark: false, expected: 'light'}, {dark: true, expected: 'dark'},
    {saved: 'light', dark: true, expected: 'light'},
    {saved: 'dark', dark: false, expected: 'dark'},
    {saved: 'invalid', dark: true, expected: 'dark'},
    {blocked: true, dark: true, expected: 'dark'},
    {noMedia: true, expected: 'light'}
]) {
    const p = page(options);
    assert.equal(p.initialTheme, options.expected, 'head script sets theme before body');
    assert.equal(p.theme(), options.expected, 'deferred script preserves initial theme');
    assert.equal(p.button.hidden, false);
    assert.equal(p.buttonAttributes['aria-pressed'], String(options.expected === 'dark'));
}

const p = page();
p.os(true);
assert.equal(p.theme(), 'dark', 'OS changes apply before manual selection');
p.click();
assert.equal(p.theme(), 'light');
assert.equal(p.saved(), 'light');
assert.equal(p.buttonAttributes['aria-label'], 'ダークモードに切り替え');
p.os(true);
assert.equal(p.theme(), 'light', 'manual selection overrides OS');
p.click();
assert.equal(p.saved(), 'dark');
assert.equal(p.buttonAttributes['aria-label'], 'ライトモードに切り替え');
assert.equal(page({saved: p.saved()}).initialTheme, 'dark', 'selection survives reload');
p.storage('light');
assert.equal(p.theme(), 'light', 'other tabs synchronize');
p.storage(null);
assert.equal(p.theme(), 'dark', 'removing preference restores OS theme');
p.storage('light', 'unrelated');
assert.equal(p.theme(), 'dark', 'unrelated keys are ignored');
p.storage('light', 'theme', {});
assert.equal(p.theme(), 'dark', 'sessionStorage events are ignored');
p.storage(null, null);
assert.equal(p.theme(), 'dark', 'clearing storage restores OS theme');
const blocked = page({blocked: true});
blocked.click();
blocked.os(false);
assert.equal(blocked.theme(), 'dark', 'blocked storage still allows manual selection');
assert.doesNotThrow(() => blocked.storage('light'));

// Every full page must initialize the theme before loading the application CSS.
for (const name of fs.readdirSync(path.join(resources, 'templates')).filter(name => name.endsWith('.html'))) {
    const html = fs.readFileSync(path.join(resources, 'templates', name), 'utf8');
    const head = html.indexOf('fragments/theme :: head');
    assert.ok(head >= 0 && head < html.indexOf('/css/style.css'), name);
    assert.ok(html.includes('fragments/theme :: controls'), name);
}
console.log('PASS: theme initialization, persistence, OS changes, storage failures, tab sync, and all page templates');
