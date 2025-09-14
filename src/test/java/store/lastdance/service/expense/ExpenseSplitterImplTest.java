package store.lastdance.service.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import store.lastdance.domain.expense.SplitType;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseSplitterImplTest {

    private ExpenseSplitterImpl expenseSplitter;

    @BeforeEach
    void setUp() {
        expenseSplitter = new ExpenseSplitterImpl();
    }

    private User createUserWithId(String uuidString) {
        String uniqueName = "user-" + uuidString.substring(0, 8);
        User user = User.builder()
                .email(uniqueName + "@test.com")
                .username(uniqueName)
                .nickname(uniqueName)
                .provider(OAuthProvider.KAKAO)
                .providerId(uuidString)
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "userId", UUID.fromString(uuidString));
        return user;
    }

    @Test
    @DisplayName("N분의1 - 나머지가 없는 경우")
    void calculateEqualSplit_NoRemainder() {
        // given
        User user1 = createUserWithId("00000000-0000-0000-0000-000000000001");
        User user2 = createUserWithId("00000000-0000-0000-0000-000000000002");
        User user3 = createUserWithId("00000000-0000-0000-0000-000000000003");
        List<User> members = List.of(user1, user2, user3);
        BigDecimal amount = new BigDecimal("9999");

        // when
        Map<UUID, BigDecimal> result = expenseSplitter.split(SplitType.EQUAL, amount, members, null);

        // then
        assertThat(result.get(user1.getUserId())).isEqualByComparingTo("3333");
        assertThat(result.get(user2.getUserId())).isEqualByComparingTo("3333");
        assertThat(result.get(user3.getUserId())).isEqualByComparingTo("3333");
    }

    @Test
    @DisplayName("N분의1 - 나머지가 있는 경우, userId가 가장 낮은 사용자에게 잔여액이 배분된다")
    void calculateEqualSplit_WithRemainder_DistributesToLowestUserId() {
        // given
        User userA = createUserWithId("00000000-0000-0000-0000-00000000000A");
        User userB = createUserWithId("00000000-0000-0000-0000-00000000000B");
        User userLowest = createUserWithId("00000000-0000-0000-0000-000000000001");

        List<User> members = List.of(userA, userB, userLowest);
        BigDecimal amount = new BigDecimal("10000");

        // when
        Map<UUID, BigDecimal> result = expenseSplitter.split(SplitType.EQUAL, amount, members, null);

        // then
        assertThat(result.get(userLowest.getUserId())).isEqualByComparingTo("3334"); // 1원을 더 받음
        assertThat(result.get(userA.getUserId())).isEqualByComparingTo("3333");
        assertThat(result.get(userB.getUserId())).isEqualByComparingTo("3333");
    }

    @Test
    @DisplayName("N분의1 - 입력 멤버의 순서가 달라도 분배 결과는 항상 동일해야 한다 (결정론적 동작 검증)")
    void calculateEqualSplit_isDeterministic_regardlessOfInputOrder() {
        // given
        User userA = createUserWithId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        User userB = createUserWithId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        User userC = createUserWithId("cccccccc-cccc-cccc-cccc-cccccccccccc");

        List<User> order1 = List.of(userA, userB, userC);
        List<User> order2 = List.of(userC, userA, userB);

        BigDecimal amount = new BigDecimal("10000");

        // when
        Map<UUID, BigDecimal> result1 = expenseSplitter.split(SplitType.EQUAL, amount, order1, null);
        Map<UUID, BigDecimal> result2 = expenseSplitter.split(SplitType.EQUAL, amount, order2, null);

        // then
        // 1. 두 결과가 완전히 동일한지 검증
        assertThat(result1).isEqualTo(result2);

        // 2. userId가 가장 낮은 userA가 항상 1원을 더 받는지 구체적으로 검증
        assertThat(result1.get(userA.getUserId())).isEqualByComparingTo("3334");
        assertThat(result1.get(userB.getUserId())).isEqualByComparingTo("3333");
        assertThat(result1.get(userC.getUserId())).isEqualByComparingTo("3333");
    }

    @Test
    @DisplayName("N분의1 - 멤버가 없는 경우 빈 맵을 반환한다")
    void calculateEqualSplit_NoMembers_ReturnsEmptyMap() {
        // given
        List<User> members = List.of();
        BigDecimal amount = new BigDecimal("10000");

        // when
        Map<UUID, BigDecimal> result = expenseSplitter.split(SplitType.EQUAL, amount, members, null);

        // then
        assertThat(result).isNotNull().isEmpty();
    }
}