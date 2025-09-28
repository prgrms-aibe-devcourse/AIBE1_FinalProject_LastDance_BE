package store.lastdance.converter.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSettingConverter 테스트")
class NotificationSettingConverterTest {

    @InjectMocks
    private NotificationSettingConverter notificationSettingConverter;

    @Test
    @DisplayName("Entity 생성 성공 - toEntity 메서드")
    void toEntity_Success() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        NotificationSetting result = notificationSettingConverter.toEntity(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);

        // 기본값 확인 (빌더 패턴으로 생성된 객체의 기본값)
        assertThat(result.isEmailEnabled()).isFalse(); // 기본값
        assertThat(result.isScheduleReminder()).isFalse(); // 기본값
        assertThat(result.isPaymentReminder()).isFalse(); // 기본값
        assertThat(result.isChecklistReminder()).isFalse(); // 기본값
        assertThat(result.isSseEnabled()).isFalse(); // 기본값
    }

    @Test
    @DisplayName("DTO 변환 성공 - toDto 메서드")
    void toDto_Success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        NotificationSetting setting = createNotificationSettingWithAllValues(
                1L, userId, true, true, false, true, false, createdAt
        );

        // When
        NotificationSettingResponseDTO result = notificationSettingConverter.toDto(setting);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSettingId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmailEnabled()).isTrue();
        assertThat(result.getScheduleReminder()).isTrue();
        assertThat(result.getPaymentReminder()).isFalse();
        assertThat(result.getChecklistReminder()).isTrue();
        assertThat(result.getSseEnabled()).isFalse();
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("DTO 변환 성공 - 모든 값이 false인 경우")
    void toDto_Success_AllFalse() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        NotificationSetting setting = createNotificationSettingWithAllValues(
                2L, userId, false, false, false, false, false, createdAt
        );

        // When
        NotificationSettingResponseDTO result = notificationSettingConverter.toDto(setting);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSettingId()).isEqualTo(2L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmailEnabled()).isFalse();
        assertThat(result.getScheduleReminder()).isFalse();
        assertThat(result.getPaymentReminder()).isFalse();
        assertThat(result.getChecklistReminder()).isFalse();
        assertThat(result.getSseEnabled()).isFalse();
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("DTO 변환 성공 - 모든 값이 true인 경우")
    void toDto_Success_AllTrue() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        NotificationSetting setting = createNotificationSettingWithAllValues(
                3L, userId, true, true, true, true, true, createdAt
        );

        // When
        NotificationSettingResponseDTO result = notificationSettingConverter.toDto(setting);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSettingId()).isEqualTo(3L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmailEnabled()).isTrue();
        assertThat(result.getScheduleReminder()).isTrue();
        assertThat(result.getPaymentReminder()).isTrue();
        assertThat(result.getChecklistReminder()).isTrue();
        assertThat(result.getSseEnabled()).isTrue();
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Entity와 DTO 간 변환 일관성 테스트")
    void conversionConsistency_Test() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();

        // Entity 생성
        NotificationSetting originalEntity = notificationSettingConverter.toEntity(userId);

        // 임의의 값으로 설정 (reflection을 사용하여 private 필드 설정)
        setPrivateField(originalEntity, "settingId", 10L);
        setPrivateField(originalEntity, "createdAt", LocalDateTime.now());

        // When - Entity를 DTO로 변환
        NotificationSettingResponseDTO dto = notificationSettingConverter.toDto(originalEntity);

        // Then - 변환된 DTO의 값들이 원본 Entity와 일치하는지 확인
        assertThat(dto.getSettingId()).isEqualTo(originalEntity.getSettingId());
        assertThat(dto.getUserId()).isEqualTo(originalEntity.getUserId());
        assertThat(dto.getEmailEnabled()).isEqualTo(originalEntity.isEmailEnabled());
        assertThat(dto.getScheduleReminder()).isEqualTo(originalEntity.isScheduleReminder());
        assertThat(dto.getPaymentReminder()).isEqualTo(originalEntity.isPaymentReminder());
        assertThat(dto.getChecklistReminder()).isEqualTo(originalEntity.isChecklistReminder());
        assertThat(dto.getSseEnabled()).isEqualTo(originalEntity.isSseEnabled());
        assertThat(dto.getCreatedAt()).isEqualTo(originalEntity.getCreatedAt());
    }

    @Test
    @DisplayName("Null 값 처리 테스트 - toEntity 메서드에서 NullPointerException 발생")
    void toEntity_WithNullUserId() {
        // Given
        UUID nullUserId = null;

        // When & Then
        assertThatThrownBy(() -> notificationSettingConverter.toEntity(nullUserId))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId is marked non-null but is null");
    }

    @Test
    @DisplayName("다양한 UUID 값에 대한 Entity 생성 테스트")
    void toEntity_WithVariousUserIds() {
        // Given
        UUID[] userIds = {
                UUID.randomUUID(),
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                UUID.randomUUID()
        };

        for (UUID userId : userIds) {
            // When
            NotificationSetting result = notificationSettingConverter.toEntity(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
        }
    }

    /**
     * 리플렉션을 사용하여 NotificationSetting 객체에 모든 값을 설정하는 헬퍼 메서드
     */
    private NotificationSetting createNotificationSettingWithAllValues(
            Long settingId, UUID userId, boolean emailEnabled, boolean scheduleReminder,
            boolean paymentReminder, boolean checklistReminder, boolean sseEnabled, LocalDateTime createdAt) throws Exception {

        NotificationSetting setting = NotificationSetting.builder()
                .userId(userId)
                .build();

        // 리플렉션을 사용하여 private 필드들 설정
        setPrivateField(setting, "settingId", settingId);
        setPrivateField(setting, "emailEnabled", emailEnabled);
        setPrivateField(setting, "scheduleReminder", scheduleReminder);
        setPrivateField(setting, "paymentReminder", paymentReminder);
        setPrivateField(setting, "checklistReminder", checklistReminder);
        setPrivateField(setting, "sseEnabled", sseEnabled);
        setPrivateField(setting, "createdAt", createdAt);

        return setting;
    }

    /**
     * 리플렉션을 사용하여 private 필드에 값을 설정하는 헬퍼 메서드
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}