package store.lastdance.service.notification;

import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingV2ServiceImpl 테스트")
class NotificationSettingV2ServiceImplTest {

    @Mock
    private NotificationSettingRepository settingRepository;

    @Mock
    private NotificationSettingConverter notificationSettingConverter;

    @InjectMocks
    private NotificationSettingV2ServiceImpl notificationSettingV2Service;

    @Test
    @DisplayName("사용자 알림 설정 조회 성공 - 기존 설정이 존재하는 경우")
    void getUserSetting_Success_ExistingSetting() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSetting mockSetting = createMockNotificationSetting(userId);
        NotificationSettingResponseDTO expectedResponse = createMockResponseDTO(userId);

        given(settingRepository.findByUserId(userId)).willReturn(Optional.of(mockSetting));
        given(notificationSettingConverter.toDto(mockSetting)).willReturn(expectedResponse);

        // When
        NotificationSettingResponseDTO result = notificationSettingV2Service.getUserSetting(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmailEnabled()).isTrue();
        then(settingRepository).should().findByUserId(userId);
        then(notificationSettingConverter).should().toDto(mockSetting);
        then(notificationSettingConverter).should(never()).toEntity(any());
    }

    @Test
    @DisplayName("사용자 알림 설정 조회 성공 - 기존 설정이 없어서 새로 생성하는 경우")
    void getUserSetting_Success_CreateNewSetting() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSetting newSetting = createMockNotificationSetting(userId);
        NotificationSettingResponseDTO expectedResponse = createMockResponseDTO(userId);

        given(settingRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(notificationSettingConverter.toEntity(userId)).willReturn(newSetting);
        given(settingRepository.save(newSetting)).willReturn(newSetting);
        given(notificationSettingConverter.toDto(newSetting)).willReturn(expectedResponse);

        // When
        NotificationSettingResponseDTO result = notificationSettingV2Service.getUserSetting(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        then(settingRepository).should().findByUserId(userId);
        then(notificationSettingConverter).should().toEntity(userId);
        then(settingRepository).should().save(newSetting);
        then(notificationSettingConverter).should().toDto(newSetting);
    }

    @Test
    @DisplayName("사용자 알림 설정 조회 실패 - CustomException 재던지기")
    void getUserSetting_Failure_CustomExceptionRethrow() {
        // Given
        UUID userId = UUID.randomUUID();
        CustomException customException = new CustomException(ErrorCode.USER_NOT_FOUND);

        given(settingRepository.findByUserId(userId)).willThrow(customException);

        // When & Then
        assertThatThrownBy(() -> notificationSettingV2Service.getUserSetting(userId))
                .isInstanceOf(CustomException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        then(settingRepository).should().findByUserId(userId);
    }

    @Test
    @DisplayName("사용자 알림 설정 조회 실패 - 일반 예외를 CustomException으로 변환")
    void getUserSetting_Failure_GeneralExceptionToCustomException() {
        // Given
        UUID userId = UUID.randomUUID();
        RuntimeException runtimeException = new RuntimeException("Database error");

        given(settingRepository.findByUserId(userId)).willThrow(runtimeException);

        // When & Then
        assertThatThrownBy(() -> notificationSettingV2Service.getUserSetting(userId))
                .isInstanceOf(CustomException.class)
                .hasMessage("알림 설정 수정에 실패했습니다");

        then(settingRepository).should().findByUserId(userId);
    }

    @Test
    @DisplayName("알림 설정 업데이트 성공 - 모든 필드 업데이트")
    void updateSetting_Success_AllFields() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSetting existingSetting = createMockNotificationSetting(userId);
        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO(
                false, false, false, false, false
        );
        NotificationSettingResponseDTO expectedResponse = createMockResponseDTO(userId);

        given(settingRepository.findByUserId(userId)).willReturn(Optional.of(existingSetting));
        given(settingRepository.save(existingSetting)).willReturn(existingSetting);
        given(notificationSettingConverter.toDto(existingSetting)).willReturn(expectedResponse);

        // When
        NotificationSettingResponseDTO result = notificationSettingV2Service.updateSetting(userId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        then(settingRepository).should().findByUserId(userId);
        then(settingRepository).should().save(existingSetting);
        then(notificationSettingConverter).should().toDto(existingSetting);

        // 모든 업데이트 메서드가 호출되었는지 확인
        verify(existingSetting).updateEmailEnabled(false);
        verify(existingSetting).updateScheduleReminder(false);
        verify(existingSetting).updatePaymentReminder(false);
        verify(existingSetting).updateChecklistReminder(false);
        verify(existingSetting).updateSSEEnabled(false);
    }

    @Test
    @DisplayName("알림 설정 업데이트 성공 - 일부 필드만 업데이트")
    void updateSetting_Success_PartialFields() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSetting existingSetting = createMockNotificationSetting(userId);
        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO();
        // 나머지 필드는 null

        NotificationSettingResponseDTO expectedResponse = createMockResponseDTO(userId);

        given(settingRepository.findByUserId(userId)).willReturn(Optional.of(existingSetting));
        given(settingRepository.save(existingSetting)).willReturn(existingSetting);
        given(notificationSettingConverter.toDto(existingSetting)).willReturn(expectedResponse);

        // When
        NotificationSettingResponseDTO result = notificationSettingV2Service.updateSetting(userId, request);

        // Then
        assertThat(result).isNotNull();
        then(settingRepository).should().findByUserId(userId);
        then(settingRepository).should().save(existingSetting);

        // 설정된 필드만 업데이트되었는지 확인
        verify(existingSetting).updateEmailEnabled(false);
        verify(existingSetting).updateScheduleReminder(true);
        verify(existingSetting, never()).updatePaymentReminder(anyBoolean());
        verify(existingSetting, never()).updateChecklistReminder(anyBoolean());
        verify(existingSetting, never()).updateSSEEnabled(anyBoolean());
    }

    @Test
    @DisplayName("알림 설정 업데이트 성공 - 변경 사항이 없는 경우 save 호출하지 않음")
    void updateSetting_Success_NoChanges() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSetting existingSetting = createMockNotificationSetting(userId);
        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO();
        // 모든 필드가 null

        NotificationSettingResponseDTO expectedResponse = createMockResponseDTO(userId);

        given(settingRepository.findByUserId(userId)).willReturn(Optional.of(existingSetting));
        given(notificationSettingConverter.toDto(existingSetting)).willReturn(expectedResponse);

        // When
        NotificationSettingResponseDTO result = notificationSettingV2Service.updateSetting(userId, request);

        // Then
        assertThat(result).isNotNull();
        then(settingRepository).should().findByUserId(userId);
        then(settingRepository).should(never()).save(any()); // save 호출되지 않아야 함
        then(notificationSettingConverter).should().toDto(existingSetting);
    }

    @Test
    @DisplayName("알림 설정 업데이트 실패 - 설정을 찾을 수 없는 경우")
    void updateSetting_Failure_SettingNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO(
                true, true, true, true, true
        );

        given(settingRepository.findByUserId(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationSettingV2Service.updateSetting(userId, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("알림 설정을 찾을 수 없습니다.");

        then(settingRepository).should().findByUserId(userId);
        then(settingRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("알림 설정 업데이트 실패 - CustomException 재던지기")
    void updateSetting_Failure_CustomExceptionRethrow() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO(true, true, true, true, true);
        CustomException customException = new CustomException(ErrorCode.USER_NOT_FOUND);

        given(settingRepository.findByUserId(userId)).willThrow(customException);

        // When & Then
        assertThatThrownBy(() -> notificationSettingV2Service.updateSetting(userId, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("알림 설정 업데이트 실패 - 일반 예외를 CustomException으로 변환")
    void updateSetting_Failure_GeneralExceptionToCustomException() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSettingRequestDTO request = new NotificationSettingRequestDTO(true, true, true, true, true);
        RuntimeException runtimeException = new RuntimeException("Database error");

        given(settingRepository.findByUserId(userId)).willThrow(runtimeException);

        // When & Then
        assertThatThrownBy(() -> notificationSettingV2Service.updateSetting(userId, request))
                .isInstanceOf(CustomException.class)
                .hasMessage("알림 설정 수정에 실패했습니다");
    }

    @Test
    @DisplayName("기본 알림 설정 생성 성공")
    void createDefaultSetting_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSetting newSetting = createMockNotificationSetting(userId);

        given(notificationSettingConverter.toEntity(userId)).willReturn(newSetting);
        given(settingRepository.save(newSetting)).willReturn(newSetting);

        // When
        notificationSettingV2Service.createDefaultSetting(userId);

        // Then
        then(notificationSettingConverter).should().toEntity(userId);
        then(settingRepository).should().save(newSetting);
    }

    @Test
    @DisplayName("기본 알림 설정 생성 실패 - 이미 존재하는 설정")
    void createDefaultSetting_Failure_AlreadyExists() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSetting newSetting = createMockNotificationSetting(userId);
        DataIntegrityViolationException dataIntegrityException = new DataIntegrityViolationException("Unique constraint violation");

        given(notificationSettingConverter.toEntity(userId)).willReturn(newSetting);
        given(settingRepository.save(newSetting)).willThrow(dataIntegrityException);

        // When & Then
        assertThatThrownBy(() -> notificationSettingV2Service.createDefaultSetting(userId))
                .isInstanceOf(CustomException.class)
                .hasMessage("사용자의 알림 설정이 이미 존재합니다");

        then(notificationSettingConverter).should().toEntity(userId);
        then(settingRepository).should().save(newSetting);
    }

    @Test
    @DisplayName("기본 알림 설정 생성 실패 - 일반 예외")
    void createDefaultSetting_Failure_GeneralException() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationSetting newSetting = createMockNotificationSetting(userId);
        RuntimeException runtimeException = new RuntimeException("Database error");

        given(notificationSettingConverter.toEntity(userId)).willReturn(newSetting);
        given(settingRepository.save(newSetting)).willThrow(runtimeException);

        // When & Then
        assertThatThrownBy(() -> notificationSettingV2Service.createDefaultSetting(userId))
                .isInstanceOf(CustomException.class)
                .hasMessage("알림 설정 생성에 실패했습니다");

        then(notificationSettingConverter).should().toEntity(userId);
        then(settingRepository).should().save(newSetting);
    }

    private NotificationSetting createMockNotificationSetting(UUID userId) {
        NotificationSetting mockSetting = mock(NotificationSetting.class);
        lenient().when(mockSetting.getUserId()).thenReturn(userId);
        return mockSetting;
    }

    private NotificationSettingResponseDTO createMockResponseDTO(UUID userId) {
        return NotificationSettingResponseDTO.builder()
                .settingId(1L)
                .userId(userId)
                .emailEnabled(true)
                .scheduleReminder(true)
                .paymentReminder(true)
                .checklistReminder(true)
                .sseEnabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}