package store.lastdance.service.youthpolicy;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.domain.youthpolicy.YouthPolicy;
import store.lastdance.dto.youthpolicy.YouthPolicyDTO;
import store.lastdance.repository.youthpolicy.YouthPolicyRepository;
import store.lastdance.util.youthpolicy.YouthPolicyClient;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YouthPolicyServiceImpl implements YouthPolicyService {

    private final YouthPolicyClient policyClient;
    private final YouthPolicyRepository policyRepository;

    @Override
    public List<YouthPolicyDTO> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(policy -> YouthPolicyDTO.builder()
                        .plcyNo(policy.getPlcyNo())
                        .plcyNm(policy.getPlcyNm())
                        .plcyKywdNm(policy.getPlcyKywdNm())
                        .plcyExplnCn(policy.getPlcyExplnCn())
                        .bizPrdBgngYmd(policy.getBizPrdBgngYmd())
                        .bizPrdEndYmd(policy.getBizPrdEndYmd())
                        .aplyYmd(policy.getAplyYmd())
                        .plcySprtCn(policy.getPlcySprtCn())
                        .lclsfNm(policy.getLclsfNm())
                        .mclsfNm(policy.getMclsfNm())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void syncPoliciesWithOpenApi() {
        // 1. 전체 삭제
        policyRepository.deleteAll();

        // 2. 새 데이터 불러오기
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

                YouthPolicy policy = YouthPolicy.builder()
                        .plcyNo(node.path("plcyNo").asText())
                        .plcyNm(node.path("plcyNm").asText())
                        .plcyKywdNm(node.path("plcyKywdNm").asText())
                        .plcyExplnCn(node.path("plcyExplnCn").asText())
                        .bizPrdBgngYmd(node.path("bizPrdBgngYmd").asText())
                        .bizPrdEndYmd(endDate)
                        .aplyYmd(node.path("aplyYmd").asText())
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
