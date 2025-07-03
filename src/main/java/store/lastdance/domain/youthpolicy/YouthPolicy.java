package store.lastdance.domain.youthpolicy;

import jakarta.persistence.*;
import lombok.*;
import store.lastdance.domain.common.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "youth_policies")
public class YouthPolicy extends BaseTimeEntity {

    @Id
    @Column(name = "plcy_no")
    private String plcyNo;

    @Column(name = "plcy_nm", columnDefinition = "TEXT", nullable = false)
    private String plcyNm;

    @Column(name = "plcy_kywd_nm", columnDefinition = "TEXT")
    private String plcyKywdNm;

    @Column(name = "plcy_expln_cn", columnDefinition = "TEXT")
    private String plcyExplnCn;

    @Column(name = "biz_prd_bgng_ymd")
    private String bizPrdBgngYmd;

    @Column(name = "biz_prd_end_ymd")
    private String bizPrdEndYmd;

    @Column(name = "aply_ymd")
    private String aplyYmd;

    @Column(name = "plcy_sprt_cn", columnDefinition = "TEXT")
    private String plcySprtCn;

    @Column(name = "zip_cd", length = 255)
    private String zipCd;

    @Column(name = "lclsf_nm", columnDefinition = "TEXT")
    private String lclsfNm;

    @Column(name = "mclsf_nm", columnDefinition = "TEXT")
    private String mclsfNm;

    @Builder
    public YouthPolicy(String plcyNo, String plcyNm, String plcyKywdNm, String plcyExplnCn,
                       String bizPrdBgngYmd, String bizPrdEndYmd, String aplyYmd, String plcySprtCn,
                       String lclsfNm, String mclsfNm) {
        this.plcyNo = plcyNo;
        this.plcyNm = plcyNm;
        this.plcyKywdNm = plcyKywdNm;
        this.plcyExplnCn = plcyExplnCn;
        this.bizPrdBgngYmd = bizPrdBgngYmd;
        this.bizPrdEndYmd = bizPrdEndYmd;
        this.aplyYmd = aplyYmd;
        this.plcySprtCn = plcySprtCn;
        this.lclsfNm = lclsfNm;
        this.mclsfNm = mclsfNm;
    }
}

