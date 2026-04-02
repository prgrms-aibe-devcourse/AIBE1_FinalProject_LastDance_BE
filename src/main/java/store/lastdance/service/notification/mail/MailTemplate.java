package store.lastdance.service.notification.mail;

import store.lastdance.domain.notification.NotificationType;

public class MailTemplate {

    private MailTemplate() {}

    public static String buildSubject(NotificationType type, String title) {
        return type.getIcon() + " " + type.getLabel() + " 알림 - " + title;
    }

    public static String buildBody(NotificationType type, String title, String content) {
        return String.format("""
                안녕하세요! LastDance에서 보내는 %s 알림입니다.

                %s %s: %s
                ⏰ 알림: %s

                %s

                LastDance 팀 드림
                """, type.getLabel(), type.getBodyIcon(), type.getLabel(), title, content, type.getClosingMessage());
    }
}
