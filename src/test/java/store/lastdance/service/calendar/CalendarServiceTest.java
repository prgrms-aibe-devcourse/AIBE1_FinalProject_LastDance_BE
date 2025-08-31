package store.lastdance.service.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import store.lastdance.converter.calendar.CalendarConverter;
import store.lastdance.domain.calendar.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.calender.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calender.request.UpdateCalendarRequestDTO;
import store.lastdance.dto.calender.response.CalendarResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalendarV2ServiceImpl 테스트")
class CalendarServiceTest {

    @InjectMocks
    private CalendarV2ServiceImpl sut;

    @Mock
    private CalendarRepository calendarRepository;
    @Mock
    private CalendarConverter calendarConverter;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRepository userRepository;

    private User user;
    private Group group;
    private Calendar calendar;
    private UUID userId;
    private UUID groupId;
    private Long calendarId;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        calendarId = 1L;

        user = User.builder()
                .email("test@test.com")
                .username("테스트유저")
                .nickname("테스트닉네임")
                .provider(OAuthProvider.KAKAO)
                .providerId("12345")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        Field userIdField = User.class.getDeclaredField("userId");
        userIdField.setAccessible(true);
        ReflectionUtils.setField(userIdField, user, userId);

        group = Group.builder()
                .groupName("테스트그룹")
                .inviteCode("TEST01")
                .owner(user)
                .build();

        Field groupIdField = Group.class.getDeclaredField("groupId");
        groupIdField.setAccessible(true);
        ReflectionUtils.setField(groupIdField, group, groupId);

        calendar = Calendar.builder()
                .title("테스트 일정")
                .description("테스트 일정 설명")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(1))
                .isAllDay(false)
                .type(CalendarType.PERSONAL)
                .category(CalendarCategory.GENERAL)
                .user(user)
                .build();

        Field calendarIdField = Calendar.class.getDeclaredField("calendarId");
        calendarIdField.setAccessible(true);
        ReflectionUtils.setField(calendarIdField, calendar, calendarId);
    }

    @Nested
    @DisplayName("createCalendar 메서드 테스트")
    class CreateCalendarTest {

        @Test
        @DisplayName("성공 - 개인 일정을 정상적으로 생성한다")
        void createCalendar_success_personal() {
            // given
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("개인 일정")
                    .description("개인 일정 설명")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusHours(1))
                    .isAllDay(false)
                    .category(CalendarCategory.PAYMENT)
                    .build();

            CalendarResponseDTO expectedResponse = CalendarResponseDTO.builder()
                    .calendarId(calendarId)
                    .title("개인 일정")
                    .description("개인 일정 설명")
                    .type(CalendarType.PERSONAL)
                    .category(CalendarCategory.PAYMENT)
                    .userId(userId)
                    .build();

            given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
            given(calendarConverter.toEntity(request, user, null, CalendarType.PERSONAL)).willReturn(calendar);
            given(calendarRepository.save(calendar)).willReturn(calendar);
            given(calendarConverter.toDto(calendar, user, null, null)).willReturn(expectedResponse);

            // when
            CalendarResponseDTO result = sut.createCalendar(request, userId, null);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            verify(userRepository).findByUserId(userId);
            verify(calendarConverter).toEntity(request, user, null, CalendarType.PERSONAL);
            verify(calendarRepository).save(calendar);
            verify(calendarConverter).toDto(calendar, user, null, null);
        }

        @Test
        @DisplayName("성공 - 그룹 일정을 정상적으로 생성한다")
        void createCalendar_success_group() {
            // given
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("그룹 일정")
                    .description("그룹 일정 설명")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusHours(1))
                    .isAllDay(false)
                    .category(CalendarCategory.PAYMENT)
                    .build();

            CalendarResponseDTO expectedResponse = CalendarResponseDTO.builder()
                    .calendarId(calendarId)
                    .title("그룹 일정")
                    .description("그룹 일정 설명")
                    .type(CalendarType.GROUP)
                    .category(CalendarCategory.PAYMENT)
                    .groupId(groupId)
                    .groupName("테스트그룹")
                    .userId(userId)
                    .build();

            given(groupRepository.existsByGroupIdAndOwnerId(groupId, userId)).willReturn(true);
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
            given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
            given(calendarConverter.toEntity(request, user, group, CalendarType.GROUP)).willReturn(calendar);
            given(calendarRepository.save(calendar)).willReturn(calendar);
            given(calendarConverter.toDto(calendar, user, group, "테스트그룹")).willReturn(expectedResponse);

            // when
            CalendarResponseDTO result = sut.createCalendar(request, userId, groupId);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            verify(groupRepository).existsByGroupIdAndOwnerId(groupId, userId);
            verify(groupRepository).findById(groupId);
            verify(userRepository).findByUserId(userId);
            verify(calendarConverter).toEntity(request, user, group, CalendarType.GROUP);
            verify(calendarRepository).save(calendar);
            verify(calendarConverter).toDto(calendar, user, group, "테스트그룹");
        }

        @Test
        @DisplayName("실패 - 사용자가 존재하지 않으면 USER_NOT_FOUND 예외를 던진다")
        void createCalendar_fail_userNotFound() {
            // given
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("일정")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusHours(1))
                    .isAllDay(false)
                    .category(CalendarCategory.GENERAL)
                    .build();

            given(userRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    sut.createCalendar(request, userId, null));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 그룹이 존재하지 않으면 GROUP_NOT_FOUND 예외를 던진다")
        void createCalendar_fail_groupNotFound() {
            // given
            CreateCalendarRequestDTO request = CreateCalendarRequestDTO.builder()
                    .title("일정")
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusHours(1))
                    .isAllDay(false)
                    .category(CalendarCategory.GENERAL)
                    .build();

            given(groupRepository.existsByGroupIdAndOwnerId(groupId, userId)).willReturn(true);
            given(groupRepository.findById(groupId)).willReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    sut.createCalendar(request, userId, groupId));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GROUP_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getCalendarById 메서드 테스트")
    class GetCalendarByIdTest {

        @Test
        @DisplayName("성공 - 일정을 정상적으로 조회한다")
        void getCalendarById_success() {
            // given
            CalendarResponseDTO expectedResponse = CalendarResponseDTO.builder()
                    .calendarId(calendarId)
                    .title("테스트 일정")
                    .description("테스트 일정 설명")
                    .type(CalendarType.PERSONAL)
                    .userId(userId)
                    .build();

            given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
            given(calendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));
            given(calendarConverter.toDto(calendar, user, null, null)).willReturn(expectedResponse);

            // when
            CalendarResponseDTO result = sut.getCalendarById(calendarId, userId);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            verify(userRepository).findByUserId(userId);
            verify(calendarRepository).findById(calendarId);
            verify(calendarConverter).toDto(calendar, user, null, null);
        }

        @Test
        @DisplayName("실패 - 일정이 존재하지 않으면 CALENDAR_NOT_FOUND 예외를 던진다")
        void getCalendarById_fail_calendarNotFound() {
            // given
            given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
            given(calendarRepository.findById(calendarId)).willReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    sut.getCalendarById(calendarId, userId));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CALENDAR_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateCalendar 메서드 테스트")
    class UpdateCalendarTest {

        @Test
        @DisplayName("성공 - 일정을 정상적으로 수정한다")
        void updateCalendar_success() {
            // given
            UpdateCalendarRequestDTO request = UpdateCalendarRequestDTO.builder()
                    .title("수정된 제목")
                    .description("수정된 설명")
                    .build();

            CalendarResponseDTO expectedResponse = CalendarResponseDTO.builder()
                    .calendarId(calendarId)
                    .title("수정된 제목")
                    .description("수정된 설명")
                    .type(CalendarType.PERSONAL)
                    .userId(userId)
                    .build();

            given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
            given(calendarRepository.findByIdWithLock(calendarId)).willReturn(Optional.of(calendar));
            given(calendarRepository.save(calendar)).willReturn(calendar);
            given(calendarConverter.toDto(calendar, user, null, null)).willReturn(expectedResponse);

            // when
            CalendarResponseDTO result = sut.updateCalendar(calendarId, request, userId);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            verify(userRepository).findByUserId(userId);
            verify(calendarRepository).findByIdWithLock(calendarId);
            verify(calendarRepository).save(calendar);
            verify(calendarConverter).toDto(calendar, user, null, null);
        }

        @Test
        @DisplayName("실패 - 권한이 없으면 CALENDAR_ACCESS_DENIED 예외를 던진다")
        void updateCalendar_fail_accessDenied() throws Exception {
            // given
            UUID otherUserId = UUID.randomUUID();
            User otherUser = User.builder()
                    .email("other@test.com")
                    .username("다른유저")
                    .nickname("다른닉네임")
                    .provider(OAuthProvider.KAKAO)
                    .providerId("67890")
                    .role(UserRole.USER)
                    .isActive(true)
                    .build();

            Field userIdField = User.class.getDeclaredField("userId");
            userIdField.setAccessible(true);
            ReflectionUtils.setField(userIdField, otherUser, otherUserId);

            Calendar otherCalendar = Calendar.builder()
                    .title("다른 사용자의 일정")
                    .user(otherUser)
                    .type(CalendarType.PERSONAL)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusHours(1))
                    .isAllDay(false)
                    .category(CalendarCategory.PAYMENT)
                    .build();

            UpdateCalendarRequestDTO request = UpdateCalendarRequestDTO.builder()
                    .title("수정 시도")
                    .build();

            given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
            given(calendarRepository.findByIdWithLock(calendarId)).willReturn(Optional.of(otherCalendar));

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    sut.updateCalendar(calendarId, request, userId));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CALENDAR_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("deleteCalendar 메서드 테스트")
    class DeleteCalendarTest {

        @Test
        @DisplayName("성공 - 일정을 정상적으로 삭제한다")
        void deleteCalendar_success() {
            // given
            given(calendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));
            willDoNothing().given(calendarRepository).delete(calendar);

            // when
            sut.deleteCalendar(calendarId, userId);

            // then
            verify(calendarRepository).findById(calendarId);
            verify(calendarRepository).delete(calendar);
        }

        @Test
        @DisplayName("실패 - 일정이 존재하지 않으면 CALENDAR_NOT_FOUND 예외를 던진다")
        void deleteCalendar_fail_calendarNotFound() {
            // given
            given(calendarRepository.findById(calendarId)).willReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class, () ->
                    sut.deleteCalendar(calendarId, userId));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CALENDAR_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("isGroupMember 메서드 테스트")
    class IsGroupMemberTest {

        @Test
        @DisplayName("성공 - 그룹 소유자인 경우 true를 반환한다")
        void isGroupMember_success_owner() {
            // given
            given(groupRepository.existsByGroupIdAndOwnerId(groupId, userId)).willReturn(true);
            given(groupRepository.existsByGroupIdAndMemberId(groupId, userId)).willReturn(false);

            // when
            boolean result = sut.isGroupMember(groupId, userId);

            // then
            assertThat(result).isTrue();
            verify(groupRepository).existsByGroupIdAndOwnerId(groupId, userId);
        }

        @Test
        @DisplayName("성공 - 그룹 멤버인 경우 true를 반환한다")
        void isGroupMember_success_member() {
            // given
            given(groupRepository.existsByGroupIdAndOwnerId(groupId, userId)).willReturn(false);
            given(groupRepository.existsByGroupIdAndMemberId(groupId, userId)).willReturn(true);

            // when
            boolean result = sut.isGroupMember(groupId, userId);

            // then
            assertThat(result).isTrue();
            verify(groupRepository).existsByGroupIdAndMemberId(groupId, userId);
        }

        @Test
        @DisplayName("성공 - 그룹 멤버가 아닌 경우 false를 반환한다")
        void isGroupMember_success_notMember() {
            // given
            given(groupRepository.existsByGroupIdAndOwnerId(groupId, userId)).willReturn(false);
            given(groupRepository.existsByGroupIdAndMemberId(groupId, userId)).willReturn(false);

            // when
            boolean result = sut.isGroupMember(groupId, userId);

            // then
            assertThat(result).isFalse();
        }
    }
}