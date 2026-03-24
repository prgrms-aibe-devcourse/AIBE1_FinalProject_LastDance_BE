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

    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.emailEnabled = true")
    List<UUID> findUserIdsByEmailEnabledTrue();

    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.sseEnabled = true")
    List<UUID> findUserIdsBySSEEnabledTrue();

    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.sseEnabled = true AND ns.scheduleReminder = true")
    boolean isSSEEnabledAndScheduleReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.sseEnabled = true AND ns.paymentReminder = true")
    boolean isSSEEnabledAndPaymentReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.sseEnabled = true AND ns.checklistReminder = true")
    boolean isSSEEnabledAndChecklistReminderTrue(@Param("userId") UUID userId);
}