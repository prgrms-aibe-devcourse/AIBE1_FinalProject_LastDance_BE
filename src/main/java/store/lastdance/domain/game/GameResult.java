package store.lastdance.domain.game;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import java.util.UUID;

@Getter
@Entity
@Table(name = "game_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameResult extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "game_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private GameType gameType;

    @Column(name = "participants", nullable = false, columnDefinition = "JSON")
    private String participants;

    @Column(name = "result", nullable = false, columnDefinition = "JSON")
    private String result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public GameResult(@NonNull GameType gameType, @NonNull String participants, @NonNull String result, UUID groupId, UUID userId) {
        this.gameType = gameType;
        this.participants = participants;
        this.result = result;
        this.groupId = groupId;
        this.userId = userId;
    }
}
