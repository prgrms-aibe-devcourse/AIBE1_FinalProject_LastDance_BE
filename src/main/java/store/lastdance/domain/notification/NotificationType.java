package store.lastdance.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum NotificationType {

    SCHEDULE("일정", "📅", "일정",    "📌", "잊지 마시고 준비해 주세요!"),
    PAYMENT ("납부일", "💳", "지출 항목", "📊", "그룹 지출에 대한 정산이 요청되었습니다.\n앱에서 확인해 주세요!"),
    CHECKLIST("할일", "✅", "할일",   "📝", "완료하는 것을 잊지 마세요!");

    private final String description;   // 한글 설명 (기존 유지)
    private final String icon;          // 제목용 이모지
    private final String label;         // 항목 유형명
    private final String bodyIcon;      // 본문 항목 이모지
    private final String closingMessage; // 본문 마무리 문구


}
