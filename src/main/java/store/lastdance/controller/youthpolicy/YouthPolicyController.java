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

    @GetMapping
    public List<YouthPolicyDTO> getAllPolicies() {
        return youthPolicyService.getAllPolicies();
    }

    @GetMapping("/{plcyNo}")
    public ResponseEntity<YouthPolicyDTO> getPolicyByPlcyNo(@PathVariable String plcyNo) {
        try {
            YouthPolicyDTO policy = youthPolicyService.getPolicyByPlcyNo(plcyNo);
            return ResponseEntity.ok(policy);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/test")
    public ResponseEntity<String> syncNow() {
        youthPolicyService.syncPoliciesWithOpenApi();
        return ResponseEntity.ok("정책 동기화 완료");
    }
}