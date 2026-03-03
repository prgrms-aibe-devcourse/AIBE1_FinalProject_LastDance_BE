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
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YouthPolicyV2ServiceImpl implements YouthPolicyV2Service { // Implement V2 Service

    private final YouthPolicyClient policyClient;
    private final YouthPolicyRepository policyRepository;

    @Override
    public List<YouthPolicyDTO> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public YouthPolicyDTO getPolicyByPlcyNo(String plcyNo) {
        return policyRepository.findByPlcyNo(plcyNo)
                .map(this::convertToDto)
                .orElseThrow(() -> new NoSuchElementException("정책을 찾을 수 없습니다: " + plcyNo));
    }

    @Override
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

    private YouthPolicyDTO convertToDto(YouthPolicy policy) {
        return YouthPolicyDTO.builder()
                .plcyNo(policy.getPlcyNo())
                .plcyNm(policy.getPlcyNm())
                .plcyKywdNm(policy.getPlcyKywdNm())
                .plcyExplnCn(policy.getPlcyExplnCn())
                .bizPrdBgngYmd(policy.getBizPrdBgngYmd())
                .plcyStDt(policy.getBizPrdBgngYmd())
                .plcyEndDt(policy.getBizPrdEndYmd())
                .bizPrdEndYmd(policy.getBizPrdEndYmd())
                .aplyYmd(policy.getAplyYmd())
                .plcySprtCn(policy.getPlcySprtCn())
                .lclsfNm(policy.getLclsfNm())
                .mclsfNm(policy.getMclsfNm())
                .build();
    }
}
