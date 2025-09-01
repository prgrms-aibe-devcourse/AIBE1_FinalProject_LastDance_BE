package store.lastdance.dto.youthpolicy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouthPolicyDTO {
    private String plcyNo;
    private String plcyNm;
    private String plcyKywdNm;
    private String plcyExplnCn;
    private String bizPrdBgngYmd;
    private String plcyStDt;
    private String plcyEndDt;
    private String bizPrdEndYmd;
    private String aplyYmd;
    private String plcySprtCn;
    private String lclsfNm;
    private String mclsfNm;
}