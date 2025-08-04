package store.lastdance.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByUserId(UUID userId);

    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.emailEnabled = true AND ns.emailEnabled IS NOT NULL")
    List<UUID> findUserIdsByEmailEnabledTrue();

    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.sseEnabled = true AND ns.sseEnabled IS NOT NULL")
    List<UUID> findUserIdsBySSEEnabledTrue();

    @Query(value = "SELECT CASE WHEN COUNT(ns.user_id) > 0 THEN true ELSE false END " +
                   "FROM notification_settings ns " +
                   "WHERE ns.user_id = :userId AND ns.sse_enabled = true AND ns.schedule_reminder = true",
            nativeQuery = true)
    boolean isSSEEnabledAndScheduleReminderTrue(@Param("userId") UUID userId);
    
    @Query(value = "SELECT CASE WHEN COUNT(ns.user_id) > 0 THEN true ELSE false END " +
                   "FROM notification_settings ns " +
                   "WHERE ns.user_id = :userId AND ns.sse_enabled = true AND ns.payment_reminder = true",
            nativeQuery = true)
    boolean isSSEEnabledAndPaymentReminderTrue(@Param("userId") UUID userId);
    
    @Query(value = "SELECT CASE WHEN COUNT(ns.user_id) > 0 THEN true ELSE false END " +
                   "FROM notification_settings ns " +
                   "WHERE ns.user_id = :userId AND ns.sse_enabled = true AND ns.checklist_reminder = true",
            nativeQuery = true)
    boolean isSSEEnabledAndChecklistReminderTrue(@Param("userId") UUID userId);
}