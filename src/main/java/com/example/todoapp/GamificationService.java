package com.example.todoapp;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamificationService {
    private final GamificationMapper mapper;

    public GamificationService(GamificationMapper mapper) { this.mapper = mapper; }

    @Transactional
    public void awardCompletion(Todo todo) {
        int points = 10;
        if (todo.getPriority() != null && todo.getPriority() == 1) points += 10;
        else if (todo.getPriority() != null && todo.getPriority() == 2) points += 5;
        if (todo.getDueDate() != null && todo.getCompletedAt() != null
                && !todo.getCompletedAt().toLocalDate().isAfter(todo.getDueDate())) points += 5;
        mapper.addPoints(points);
        mapper.insertHistory(points, "タスク完了: " + todo.getTitle(), todo.getId());
    }

    public GamificationProfile profile() {
        int total = mapper.totalPoints();
        int level = 1;
        int threshold = 0;
        int nextThreshold = 100;
        while (total >= nextThreshold) {
            level++;
            threshold = nextThreshold;
            nextThreshold += level * 100;
        }
        int span = nextThreshold - threshold;
        return new GamificationProfile(total, level, total - threshold, nextThreshold - total,
                (int) ((long) (total - threshold) * 100 / span));
    }

    public List<PointHistory> history() { return mapper.findHistory(); }
}
