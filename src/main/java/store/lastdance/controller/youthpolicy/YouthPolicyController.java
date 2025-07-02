package store.lastdance.controller.youthpolicy;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import store.lastdance.service.community.CommunityService;
import store.lastdance.service.youthpolicy.YouthPolicyService;
import store.lastdance.service.youthpolicy.YouthPolicyServiceImpl;

@RestController
@RequestMapping("/api/v1/youth-policy")
public class YouthPolicyController {

    private final YouthPolicyService policyService;

    public YouthPolicyController(YouthPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public JsonNode getPolicies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return policyService.fetchPolicyList(page, size, keyword);
    }
}
