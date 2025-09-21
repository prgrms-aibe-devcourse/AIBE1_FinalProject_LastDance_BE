package store.lastdance.service.youthpolicy;

import java.util.List;
import store.lastdance.dto.youthpolicy.YouthPolicyDTO;

public interface YouthPolicyV2QueryService {
    List<YouthPolicyDTO> getAllPolicies();

    YouthPolicyDTO getPolicyByPlcyNo(String plcyNo);
}
