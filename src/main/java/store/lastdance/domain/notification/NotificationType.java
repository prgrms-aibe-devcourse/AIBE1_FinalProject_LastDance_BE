package store.lastdance.domain.notification;

import lombok.Getter;

@Getter
public enum NotificationType {

    SCHEDULE("일정", "📅", "일정") {
        @Override
        public String buildSubject(String title) {
            return getIcon() + " 일정 알림 - " + title;
        }

        @Override
        public String buildBody(String title, String content) {
            return String.format("""
                    안녕하세요! LastDance에서 보내는 일정 알림입니다.

                    📌 일정: %s
                    ⏰ 알림: %s

                    잊지 마시고 준비해 주세요!

                    LastDance 팀 드림
                    """, title, content);
        }
    },

    PAYMENT("납부일", "💳", "정산 요청") {
        @Override
        public String buildSubject(String title) {
            return getIcon() + " 정산 요청 알림 - " + title;
        }

        @Override
        public String buildBody(String title, String content) {
            return String.format("""
                    안녕하세요! LastDance에서 보내는 정산 요청 알림입니다.

                    📊 지출 항목: %s
                    📅 알림: %s

                    그룹 지출에 대한 정산이 요청되었습니다.
                    앱에서 확인해 주세요!

                    LastDance 팀 드림
                    """, title, content);
        }
    },

    CHECKLIST("할일", "✅", "할일") {
        @Override
        public String buildSubject(String title) {
            return getIcon() + " 할일 알림 - " + title;
        }

        @Override
        public String buildBody(String title, String content) {
            return String.format("""
                    안녕하세요! LastDance에서 보내는 할일 알림입니다.

                    📝 할일: %s
                    ⏰ 알림: %s

                    완료하는 것을 잊지 마세요!

                    LastDance 팀 드림
                    """, title, content);
        }
    };

    private final String description;
    private final String icon;
    private final String label;

    NotificationType(String description, String icon, String label) {
        this.description = description;
        this.icon = icon;
        this.label = label;
    }

    public abstract String buildSubject(String title);

    public abstract String buildBody(String title, String content);
}
