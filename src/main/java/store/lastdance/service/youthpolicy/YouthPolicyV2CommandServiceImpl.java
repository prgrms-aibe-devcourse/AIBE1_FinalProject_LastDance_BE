package store.lastdance.service.youthpolicy;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.youthpolicy.YouthPolicy;
import store.lastdance.repository.youthpolicy.YouthPolicyRepository;
import store.lastdance.util.youthpolicy.YouthPolicyClient;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class YouthPolicyV2CommandServiceImpl implements YouthPolicyV2CommandService {

    private final YouthPolicyClient policyClient;
    private final YouthPolicyRepository policyRepository;

    @Override
    @Transactional
    public void syncPoliciesWithOpenApi() {
        policyRepository.deleteAll();

        int page = 1;
        int pageSize = 3;
        boolean hasMore = true;

        LocalDate today = LocalDate.now();

        while (hasMore) {
            JsonNode result = policyClient.getYouthPolicies(page, pageSize, "");
            JsonNode list = result.path("result").path("youthPolicyList");

            if (list.isEmpty()) break;

            for (JsonNode node : list) {
                String endDate = node.path("bizPrdEndYmd").asText();
                if (endDate.isEmpty() || endDate.compareTo(today.toString().replaceAll("-", "")) < 0) continue;

                // aplyYmd가 null이거나 비어있으면 저장하지 않도록 추가된 로직
                String aplyYmd = node.path("aplyYmd").asText();
                if (aplyYmd == null || aplyYmd.isEmpty()) {
                    continue;
                }

                YouthPolicy policy = YouthPolicy.builder()
                        .plcyNo(node.path("plcyNo").asText())
                        .plcyNm(node.path("plcyNm").asText())
                        .plcyKywdNm(node.path("plcyKywdNm").asText())
                        .plcyExplnCn(node.path("plcyExplnCn").asText())
                        .bizPrdBgngYmd(node.path("bizPrdBgngYmd").asText())
                        .bizPrdEndYmd(endDate)
                        .aplyYmd(aplyYmd)
                        .plcySprtCn(node.path("plcySprtCn").asText())
                        .lclsfNm(node.path("lclsfNm").asText())
                        .mclsfNm(node.path("mclsfNm").asText())
                        .build();

                policyRepository.save(policy);
            }

            page++;
            hasMore = list.size() == pageSize;
        }
    }
}
