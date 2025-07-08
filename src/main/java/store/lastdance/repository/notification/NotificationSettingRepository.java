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

    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.webpushEnabled = true AND ns.webpushEnabled IS NOT NULL AND ns.webpushEndpoint IS NOT NULL")
    List<UUID> findUserIdsByWebPushEnabledTrue();

    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.emailEnabled = true AND ns.scheduleReminder = true AND ns.emailEnabled IS NOT NULL AND ns.scheduleReminder IS NOT NULL")
    boolean isEmailEnabledAndScheduleReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.emailEnabled = true AND ns.paymentReminder = true AND ns.emailEnabled IS NOT NULL AND ns.paymentReminder IS NOT NULL")
    List<UUID> findUserIdsByEmailEnabledAndPaymentReminderTrue();
    
    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.emailEnabled = true AND ns.checklistReminder = true AND ns.emailEnabled IS NOT NULL AND ns.checklistReminder IS NOT NULL")
    List<UUID> findUserIdsByEmailEnabledAndChecklistReminderTrue();

    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.sseEnabled = true AND ns.scheduleReminder = true AND ns.sseEnabled IS NOT NULL AND ns.scheduleReminder IS NOT NULL")
    boolean isSSEEnabledAndScheduleReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.sseEnabled = true AND ns.paymentReminder = true AND ns.sseEnabled IS NOT NULL AND ns.paymentReminder IS NOT NULL")
    boolean isSSEEnabledAndPaymentReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.sseEnabled = true AND ns.checklistReminder = true AND ns.sseEnabled IS NOT NULL AND ns.checklistReminder IS NOT NULL")
    boolean isSSEEnabledAndChecklistReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.sseEnabled = true AND ns.paymentReminder = true AND ns.sseEnabled IS NOT NULL AND ns.paymentReminder IS NOT NULL")
    List<UUID> findUserIdsBySSEEnabledAndPaymentReminderTrue();
    
    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.sseEnabled = true AND ns.checklistReminder = true AND ns.sseEnabled IS NOT NULL AND ns.checklistReminder IS NOT NULL")
    List<UUID> findUserIdsBySSEEnabledAndChecklistReminderTrue();

    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.webpushEnabled = true AND ns.webpushEndpoint IS NOT NULL AND ns.scheduleReminder = true AND ns.webpushEnabled IS NOT NULL AND ns.scheduleReminder IS NOT NULL")
    boolean isWebPushEnabledAndScheduleReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.webpushEnabled = true AND ns.webpushEndpoint IS NOT NULL AND ns.paymentReminder = true AND ns.webpushEnabled IS NOT NULL AND ns.paymentReminder IS NOT NULL")
    boolean isWebPushEnabledAndPaymentReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(ns) > 0 THEN true ELSE false END FROM NotificationSetting ns WHERE ns.userId = :userId AND ns.webpushEnabled = true AND ns.webpushEndpoint IS NOT NULL AND ns.checklistReminder = true AND ns.webpushEnabled IS NOT NULL AND ns.checklistReminder IS NOT NULL")
    boolean isWebPushEnabledAndChecklistReminderTrue(@Param("userId") UUID userId);
    
    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.webpushEnabled = true AND ns.webpushEndpoint IS NOT NULL AND ns.paymentReminder = true AND ns.webpushEnabled IS NOT NULL AND ns.paymentReminder IS NOT NULL")
    List<UUID> findUserIdsByWebPushEnabledAndPaymentReminderTrue();
    
    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.webpushEnabled = true AND ns.webpushEndpoint IS NOT NULL AND ns.checklistReminder = true AND ns.webpushEnabled IS NOT NULL AND ns.checklistReminder IS NOT NULL")
    List<UUID> findUserIdsByWebPushEnabledAndChecklistReminderTrue();
}