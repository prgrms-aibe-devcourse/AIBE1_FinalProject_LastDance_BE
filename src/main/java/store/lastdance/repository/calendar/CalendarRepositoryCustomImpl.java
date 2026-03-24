package store.lastdance.repository.calendar;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarCategory;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static store.lastdance.domain.calendar.QCalendar.calendar;
import static store.lastdance.domain.group.QGroup.group;
import static store.lastdance.domain.group.QGroupMember.groupMember;

@RequiredArgsConstructor
public class CalendarRepositoryCustomImpl implements CalendarRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Calendar> findCalendarsWithDynamicFilters(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate,
            String type, String category, UUID groupId) {

        return queryFactory
                .selectFrom(calendar)
                .leftJoin(calendar.user).fetchJoin()
                .leftJoin(calendar.group, group).fetchJoin()
                .where(
                        // 1. 접근 권한 (내 일정 OR 내가 속한 그룹 일정)
                        userAccessCondition(userId),
                        // 2. 날짜 범위 (반복 일정 로직 포함)
                        dateRangeCondition(startDate, endDate),
                        // 3. 동적 필터
                        typeEq(type),
                        categoryEq(category),
                        groupIdEq(groupId)
                )
                .orderBy(calendar.startDate.asc())
                .fetch();
    }

    // 접근 권한: 본인 소유이거나, 본인이 그룹장/멤버인 그룹 일정
    private BooleanExpression userAccessCondition(UUID userId) {
        return calendar.user.userId.eq(userId)
                .or(calendar.type.eq(CalendarType.GROUP).and(
                        group.owner.userId.eq(userId)
                                .or(group.groupId.in(
                                        queryFactory.select(groupMember.group.groupId)
                                                .from(groupMember)
                                                .where(groupMember.user.userId.eq(userId))
                                ))
                ));
    }

    // 날짜 조건: 일반 일정은 기간 중첩, 반복 일정은 종료일 확인
    private BooleanExpression dateRangeCondition(LocalDateTime start, LocalDateTime end) {
        BooleanExpression nonRepeating = calendar.repeatType.isNull()
                .or(calendar.repeatType.eq(store.lastdance.domain.calendar.RepeatType.NONE))
                .and(calendar.startDate.loe(end).and(calendar.endDate.goe(start)));

        BooleanExpression repeating = calendar.repeatType.isNotNull()
                .and(calendar.repeatType.ne(store.lastdance.domain.calendar.RepeatType.NONE))
                .and(calendar.startDate.loe(end))
                .and(calendar.repeatEndDate.isNull().or(calendar.repeatEndDate.goe(start)));

        return nonRepeating.or(repeating);
    }

    private BooleanExpression typeEq(String type) {
        if (type == null) return null;
        try {
            return calendar.type.eq(CalendarType.valueOf(type.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.CALENDAR_INVALID_CATEGORY);
        }
    }

    private BooleanExpression categoryEq(String category) {
        if (category == null) return null;
        try {
            return calendar.category.eq(CalendarCategory.valueOf(category.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.CALENDAR_INVALID_CATEGORY);
        }
    }

    private BooleanExpression groupIdEq(UUID groupId) {
        return groupId != null ? calendar.group.groupId.eq(groupId) : null;
    }
}