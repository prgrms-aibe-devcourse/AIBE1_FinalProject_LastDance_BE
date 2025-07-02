package store.lastdance.service.youthpolicy;

import java.util.List;
import store.lastdance.dto.youthpolicy.YouthPolicyDTO;

public interface YouthPolicyService {
    List<YouthPolicyDTO> getAllPolicies();  // DB에서 전체 조회용
    void syncPoliciesWithOpenApi();         // 새벽 12시마다 실행할 동기화 메서드
}
