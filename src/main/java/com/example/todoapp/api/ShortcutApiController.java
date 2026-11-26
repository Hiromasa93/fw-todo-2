package com.example.todoapp.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortcutApiController {
    public record Shortcut(String keys, String description, String scope) {}

    @GetMapping("/api/shortcuts")
    public List<Shortcut> shortcuts() {
        return List.of(
                new Shortcut("Alt+N", "新規タスク作成", "全画面"),
                new Shortcut("Ctrl+S", "確認画面へ進む／登録・保存する", "作成・編集・確認画面"),
                new Shortcut("Ctrl+F", "検索欄にフォーカス（他の画面では一覧へ移動）", "全画面"),
                new Shortcut("↑ / ↓ / ← / →", "前後のタスクにフォーカス", "タスク一覧"),
                new Shortcut("Enter", "フォーカス中のタスクの詳細を表示", "タスク行"),
                new Shortcut("?", "ショートカットヘルプを表示", "全画面"),
                new Shortcut("Escape", "ヘルプを閉じる／入力欄のフォーカスを外す", "入力中も有効"));
    }
}
