package store.lastdance.service.youthpolicy;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import store.lastdance.util.youthpolicy.YouthPolicyClient;

@Service
public class YouthPolicyServiceImpl implements YouthPolicyService {

    private final YouthPolicyClient youthPolicyClient;

    public YouthPolicyServiceImpl(YouthPolicyClient youthPolicyClient) {
        this.youthPolicyClient = youthPolicyClient;
    }

    @Override
    public JsonNode fetchPolicyList(int page, int size, String keyword) {
        return youthPolicyClient.getYouthPolicies(page, size, keyword);
    }
}
