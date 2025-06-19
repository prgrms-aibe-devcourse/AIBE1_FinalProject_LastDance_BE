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

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "priority", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by", insertable = false, updatable = false)
    private User completedByUser;

    @Builder
    public Checklist(@NonNull String title, @NonNull ChecklistType type, @NonNull UUID userId) {
        this.title = title;
        this.type = type;
        this.userId = userId;
        this.isCompleted = false;
        this.priority = Priority.MEDIUM;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void complete(UUID completedBy) {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
        this.completedBy = completedBy;
    }

    public void uncomplete() {
        this.isCompleted = false;
        this.completedAt = null;
        this.completedBy = null;
    }

    public void updateDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public void updatePriority(Priority priority) {
        this.priority = priority;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }
}
