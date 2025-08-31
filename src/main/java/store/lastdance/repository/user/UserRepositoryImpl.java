package store.lastdance.repository.user;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import store.lastdance.domain.user.User;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

import static store.lastdance.domain.common.QImageFile.imageFile;
import static store.lastdance.domain.user.QUser.user;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<User> findByIdWithProfileImage(UUID userId) {
        User foundUser = queryFactory
                .selectFrom(user)
                .leftJoin(user.profileImageFile, imageFile).fetchJoin()
                .where(user.userId.eq(userId))
                .fetchOne();

        return Optional.ofNullable(foundUser);
    }

    @Override
    public Optional<User> findByNicknameOrEmail(String nickname, String email) {
        Predicate predicate = orConditions(
                nicknameEq(nickname),
                emailEq(email)
        );
        if (predicate == null) {
            return Optional.empty();
        }

        User foundUser = queryFactory
                .selectFrom(user)
                .where(predicate)
                .fetchFirst();
        return Optional.ofNullable(foundUser);
    }

    private Predicate orConditions(BooleanExpression... expressions) {
        BooleanBuilder builder = new BooleanBuilder();
        for (BooleanExpression expression : expressions) {
            if (expression != null) {
                builder.or(expression);
            }
        }
        return builder.hasValue() ? builder.getValue() : null;
    }

    private BooleanExpression nicknameEq(String nickname) {
        return StringUtils.hasText(nickname) ? user.nickname.eq(nickname) : null;
    }

    private BooleanExpression emailEq(String email) {
        return StringUtils.hasText(email) ? user.email.eq(email) : null;
    }
}
