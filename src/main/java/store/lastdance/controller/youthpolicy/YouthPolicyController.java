package store.lastdance.controller.youthpolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.lastdance.domain.youthpolicy.YouthPolicy;
import store.lastdance.dto.youthpolicy.YouthPolicyDTO;
import store.lastdance.service.youthpolicy.YouthPolicyService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/youth-policy")
public class YouthPolicyController {

    private final YouthPolicyService youthPolicyService;

    @GetMapping
    public List<YouthPolicyDTO> getAllPolicies() {
        return youthPolicyService.getAllPolicies();  // DTO 반환 메서드로 변경
    }

    @PostMapping("/test")
    public ResponseEntity<String> syncNow() {
        youthPolicyService.syncPoliciesWithOpenApi();
        return ResponseEntity.ok("정책 동기화 완료");
    }


}
