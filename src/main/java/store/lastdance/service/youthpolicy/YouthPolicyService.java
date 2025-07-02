package store.lastdance.service.youthpolicy;

import com.fasterxml.jackson.databind.JsonNode;

public interface YouthPolicyService {
    JsonNode fetchPolicyList(int page, int size, String keyword);
}
