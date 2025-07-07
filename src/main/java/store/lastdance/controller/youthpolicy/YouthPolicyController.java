package store.lastdance.controller.youthpolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.youthpolicy.YouthPolicyDTO;
import store.lastdance.service.youthpolicy.YouthPolicyService;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/youth-policy")
public class YouthPolicyController {

    private final YouthPolicyService youthPolicyService;

    /**
     * 모든 청년 정책 목록을 가져옵니다.
     * GET /api/v1/youth-policy
     * @return 청년 정책 목록 DTO 배열
     */
    @GetMapping
    public List<YouthPolicyDTO> getAllPolicies() {
        return youthPolicyService.getAllPolicies();
    }

    /**
     * 특정 청년 정책을 ID(plcyNo)로 가져옵니다.
     * GET /api/v1/youth-policy/{plcyNo}
     * @param plcyNo 조회할 정책의 고유 번호
     * @return 단일 청년 정책 DTO 객체
     */
    @GetMapping("/{plcyNo}")
    public ResponseEntity<YouthPolicyDTO> getPolicyByPlcyNo(@PathVariable String plcyNo) {
        try {
            YouthPolicyDTO policy = youthPolicyService.getPolicyByPlcyNo(plcyNo);
            return ResponseEntity.ok(policy);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Open API와 정책 데이터를 동기화합니다. (테스트용)
     * POST /api/v1/youth-policy/test
     * @return 동기화 결과 메시지
     */
    @PostMapping("/test")
    public ResponseEntity<String> syncNow() {
        youthPolicyService.syncPoliciesWithOpenApi();
        return ResponseEntity.ok("정책 동기화 완료");
    }
}