package store.lastdance.repository.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import store.lastdance.domain.user.User;

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
        User foundUser = queryFactory
                .selectFrom(user)
                .where(user.nickname.eq(nickname).or(user.email.eq(email)))
                .fetchOne();
        return Optional.ofNullable(foundUser);
    }
}
