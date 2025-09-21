package store.lastdance.service.youthpolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.youthpolicy.YouthPolicy;
import store.lastdance.dto.youthpolicy.YouthPolicyDTO;
import store.lastdance.repository.youthpolicy.YouthPolicyRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YouthPolicyV2QueryServiceImpl implements YouthPolicyV2QueryService {

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
