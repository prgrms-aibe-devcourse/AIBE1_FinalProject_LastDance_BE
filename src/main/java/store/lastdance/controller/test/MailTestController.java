package store.lastdance.controller.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import store.lastdance.service.notification.MailService;

@RestController
@RequestMapping("/api/test/mail")
@RequiredArgsConstructor
@Slf4j
public class MailTestController {

    private final MailService mailService;

    /**
     * Gmail 테스트
     */
    @PostMapping("/gmail")
    public String testGmail(@RequestParam String to) {
        try {
            mailService.sendSimpleMail(to, "Gmail 테스트", "Gmail SMTP 연결 테스트입니다.", "gmail");
            return "Gmail 발송 성공: " + to;
        } catch (Exception e) {
            log.error("Gmail 테스트 실패", e);
            return "Gmail 발송 실패: " + e.getMessage();
        }
    }

    /**
     * 네이버 테스트
     */
    @PostMapping("/naver")
    public String testNaver(@RequestParam String to) {
        try {
            mailService.sendSimpleMail(to, "네이버 테스트", "네이버 SMTP 연결 테스트입니다.", "naver");
            return "네이버 발송 성공: " + to;
        } catch (Exception e) {
            log.error("네이버 테스트 실패", e);
            return "네이버 발송 실패: " + e.getMessage();
        }
    }

    /**
     * 서비스 가용성 체크
     */
    @GetMapping("/status")
    public String checkStatus() {
        boolean gmailAvailable = mailService.isProviderAvailable("gmail");
        boolean naverAvailable = mailService.isProviderAvailable("naver");
        
        return String.format("Gmail 사용 가능: %s, 네이버 사용 가능: %s", 
                gmailAvailable, naverAvailable);
    }
}
