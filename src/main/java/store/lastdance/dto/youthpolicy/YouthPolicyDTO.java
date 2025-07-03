package store.lastdance.dto.youthpolicy;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YouthPolicyDTO {
    private String plcyNo;
    private String plcyNm;
    private String plcyKywdNm;
    private String plcyExplnCn;
    private String bizPrdBgngYmd;
    private String bizPrdEndYmd;
    private String aplyYmd;
    private String plcySprtCn;
    private String lclsfNm;
    private String mclsfNm;

}
