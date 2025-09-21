package store.lastdance.converter.youthpolicy;

import org.springframework.stereotype.Component;
import store.lastdance.domain.youthpolicy.YouthPolicy;
import store.lastdance.dto.youthpolicy.YouthPolicyDTO;

@Component
public class YouthPolicyConverter {

    public YouthPolicyDTO convertToDto(YouthPolicy policy) {
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
