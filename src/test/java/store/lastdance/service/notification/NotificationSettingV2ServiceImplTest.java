package store.lastdance.service.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import store.lastdance.converter.notification.NotificationSettingConverter;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.notification.NotificationSettingRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingV2ServiceImpl 테스트")
class NotificationSettingV2ServiceImplTest {

    @Mock
    private NotificationSettingRepository settingRepository;

    @Mock
    private NotificationSettingConverter notificationSettingConverter;

    @InjectMocks
    private NotificationSettingV2ServiceImpl service;

    // ──────────────────────────────────────────────
    // getUserSetting
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("getUserSetting")
    class GetUserSetting {

        @Test
        @DisplayName("기존 설정이 있으면 DB에서 조회한 설정을 반환한다")
        void existingSetting_returnsIt() {
            UUID userId = UUID.randomUUID();
            NotificationSetting setting = buildSetting(userId);
            NotificationSettingResponseDTO expected = buildResponseDTO(userId, setting);

            given(settingRepository.findByUserId(userId)).willReturn(Optional.of(setting));
            given(notificationSettingConverter.toDto(setting)).willReturn(expected);

            NotificationSettingResponseDTO result = service.getUserSetting(userId);

            assertThat(result.getUserId()).isEqualTo(userId);
            then(settingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("기존 설정이 없으면 기본 설정을 생성해서 반환한다")
        void noSetting_createsDefault() {
            UUID userId = UUID.randomUUID();
            NotificationSetting newSetting = buildSetting(userId);
            NotificationSettingResponseDTO expected = buildResponseDTO(userId, newSetting);

            given(settingRepository.findByUserId(userId)).willReturn(Optional.empty());
            given(notificationSettingConverter.toEntity(userId)).willReturn(newSetting);
            given(settingRepository.save(newSetting)).willReturn(newSetting);
            given(notificationSettingConverter.toDto(newSetting)).willReturn(expected);

            NotificationSettingResponseDTO result = service.getUserSetting(userId);

            assertThat(result.getUserId()).isEqualTo(userId);
            then(settingRepository).should().save(newSetting);
        }
    }

    // ──────────────────────────────────────────────
    // updateSetting
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("updateSetting")
    class UpdateSetting {

        @Test
        @DisplayName("요청 필드가 모두 null이면 setting 변경 없이 save도 호출하지 않는다")
        void allNull_noSaveCall() {
            UUID userId = UUID.randomUUID();
            NotificationSetting setting = buildSetting(userId);
            NotificationSettingRequestDTO request = new NotificationSettingRequestDTO(
                    null, null, null, null, null);

            given(settingRepository.findByUserId(userId)).willReturn(Optional.of(setting));
            given(notificationSettingConverter.toDto(setting)).willReturn(buildResponseDTO(userId, setting));

            service.updateSetting(userId, request);

            then(settingRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("요청 필드가 있으면 해당 필드만 업데이트하고 save를 호출한다")
        void partialFields_saveCalled() {
            UUID userId = UUID.randomUUID();
            NotificationSetting setting = buildSetting(userId);
            // emailEnabled=true, 나머지 null
            NotificationSettingRequestDTO request = new NotificationSettingRequestDTO(
                    true, null, null, null, null);

            given(settingRepository.findByUserId(userId)).willReturn(Optional.of(setting));
            given(settingRepository.save(setting)).willReturn(setting);
            given(notificationSettingConverter.toDto(setting)).willReturn(buildResponseDTO(userId, setting));

            service.updateSetting(userId, request);

            // emailEnabled만 변경됐으므로 save 1회 호출
            then(settingRepository).should().save(setting);
            // 실제 도메인 객체의 상태 변경 확인
            assertThat(setting.isEmailEnabled()).isTrue();
        }

        @Test
        @DisplayName("모든 필드를 업데이트하면 setting에 모두 반영된다")
        void allFields_allApplied() {
            UUID userId = UUID.randomUUID();
            NotificationSetting setting = buildSetting(userId);
            NotificationSettingRequestDTO request = new NotificationSettingRequestDTO(
                    true, true, true, true, true);

            given(settingRepository.findByUserId(userId)).willReturn(Optional.of(setting));
            given(settingRepository.save(setting)).willReturn(setting);
            given(notificationSettingConverter.toDto(setting)).willReturn(buildResponseDTO(userId, setting));

            service.updateSetting(userId, request);

            assertThat(setting.isEmailEnabled()).isTrue();
            assertThat(setting.isScheduleReminder()).isTrue();
            assertThat(setting.isPaymentReminder()).isTrue();
            assertThat(setting.isChecklistReminder()).isTrue();
            assertThat(setting.isSseEnabled()).isTrue();
        }

        @Test
        @DisplayName("설정이 존재하지 않으면 NOTIFICATION_SETTING_NOT_FOUND 예외를 던진다")
        void settingNotFound_throwsException() {
            UUID userId = UUID.randomUUID();
            given(settingRepository.findByUserId(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateSetting(userId,
                    new NotificationSettingRequestDTO(true, null, null, null, null)))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));
        }
    }

    // ──────────────────────────────────────────────
    // createDefaultSetting
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("createDefaultSetting")
    class CreateDefaultSetting {

        @Test
        @DisplayName("정상적으로 기본 설정을 생성한다")
        void success() {
            UUID userId = UUID.randomUUID();
            NotificationSetting setting = buildSetting(userId);

            given(notificationSettingConverter.toEntity(userId)).willReturn(setting);
            given(settingRepository.save(setting)).willReturn(setting);

            service.createDefaultSetting(userId);

            then(settingRepository).should().save(setting);
        }

        @Test
        @DisplayName("이미 설정이 존재하면 NOTIFICATION_SETTING_ALREADY_EXISTS 예외를 던진다")
        void alreadyExists_throwsException() {
            UUID userId = UUID.randomUUID();
            NotificationSetting setting = buildSetting(userId);

            given(notificationSettingConverter.toEntity(userId)).willReturn(setting);
            given(settingRepository.save(setting)).willThrow(new DataIntegrityViolationException("unique"));

            assertThatThrownBy(() -> service.createDefaultSetting(userId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_SETTING_ALREADY_EXISTS));
        }
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    /** 실제 NotificationSetting 객체를 생성 (mock 대신 실제 도메인 객체 사용) */
    private NotificationSetting buildSetting(UUID userId) {
        return NotificationSetting.builder().userId(userId).build();
    }

    private NotificationSettingResponseDTO buildResponseDTO(UUID userId, NotificationSetting setting) {
        return NotificationSettingResponseDTO.builder()
                .userId(userId)
                .emailEnabled(setting.isEmailEnabled())
                .scheduleReminder(setting.isScheduleReminder())
                .paymentReminder(setting.isPaymentReminder())
                .checklistReminder(setting.isChecklistReminder())
                .sseEnabled(setting.isSseEnabled())
                .build();
    }
}
