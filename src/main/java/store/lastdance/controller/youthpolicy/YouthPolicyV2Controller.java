package store.lastdance.controller.youthpolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.youthpolicy.YouthPolicyDTO;
import store.lastdance.service.youthpolicy.YouthPolicyV2CommandService;
import store.lastdance.service.youthpolicy.YouthPolicyV2QueryService;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/youth-policy")
public class YouthPolicyV2Controller {

    private final YouthPolicyV2CommandService youthPolicyV2CommandService;
    private final YouthPolicyV2QueryService youthPolicyV2QueryService;

    @GetMapping
    public List<YouthPolicyDTO> getAllPolicies() {
        return youthPolicyV2QueryService.getAllPolicies();
    }

    @GetMapping("/{plcyNo}")
    public ResponseEntity<YouthPolicyDTO> getPolicyByPlcyNo(@PathVariable String plcyNo) {
        try {
            YouthPolicyDTO policy = youthPolicyV2QueryService.getPolicyByPlcyNo(plcyNo);
            return ResponseEntity.ok(policy);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/test")
    public ResponseEntity<String> syncNow() {
        youthPolicyV2CommandService.syncPoliciesWithOpenApi();
        return ResponseEntity.ok("정책 동기화 완료");
    }
}