package store.lastdance.domain.checklist;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "checklists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checklist extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checklist_id")
    private Long checklistId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ChecklistType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User assignee;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "priority", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Builder
    public Checklist(@NonNull String title, String description, @NonNull ChecklistType type, @NonNull User assignee, Group group, @NonNull LocalDateTime dueDate, @NonNull Priority priority) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.assignee = assignee;
        this.group = group;
        this.dueDate = dueDate;
        this.priority = priority;
        this.isCompleted = false;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }

    public void uncomplete() {
        this.isCompleted = false;
        this.completedAt = null;
    }

    public void updateDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public void updatePriority(Priority priority) {
        this.priority = priority;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void update(String title, String description, User assigneeById, LocalDateTime localDateTime, Priority priority) {
        this.title = title;
        this.description = description;
        this.assignee = assigneeById;
        this.dueDate = localDateTime;
        this.priority = priority;
    }
}
