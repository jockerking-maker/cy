package com.hospital.drugmanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hospital.drugmanagement.entity.DrugInfo;
import com.hospital.drugmanagement.mapper.DrugInfoMapper;
import com.hospital.drugmanagement.service.DrugInfoService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 药品信息业务实现。
 * <p>
 * 负责自动生成药品编码（YP001）和批准文号（国药准字 H/Z），继承 MyBatis-Plus 基础 CRUD。
 */
@Service
public class DrugInfoServiceImpl extends ServiceImpl<DrugInfoMapper, DrugInfo> implements DrugInfoService {

    private static final String DRUG_CODE_PREFIX = "YP";
    private static final String APPROVAL_PREFIX_H = "国药准字H";
    private static final String APPROVAL_PREFIX_Z = "国药准字Z";

    /** 根据药品类型生成下一组药品编码与批准文号。 */
    @Override
    public Map<String, String> generateNextCodes(String drugType) {
        Map<String, String> codes = new HashMap<>();
        codes.put("drugCode", generateNextDrugCode());
        codes.put("approvalNum", generateNextApprovalNum(drugType));
        return codes;
    }

    private String generateNextDrugCode() {
        LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(DrugInfo::getDrugCode, DRUG_CODE_PREFIX)
                .orderByDesc(DrugInfo::getDrugCode)
                .last("LIMIT 1");

        DrugInfo last = getOne(wrapper, false);
        int nextSeq = 1;
        if (last != null && last.getDrugCode() != null && last.getDrugCode().startsWith(DRUG_CODE_PREFIX)) {
            try {
                nextSeq = Integer.parseInt(last.getDrugCode().substring(DRUG_CODE_PREFIX.length())) + 1;
            } catch (NumberFormatException ignored) {
                nextSeq = (int) count() + 1;
            }
        }

        return nextSeq <= 999
                ? String.format("%s%03d", DRUG_CODE_PREFIX, nextSeq)
                : DRUG_CODE_PREFIX + nextSeq;
    }

    private String generateNextApprovalNum(String drugType) {
        boolean useZ = "中药".equals(drugType) || "中成药".equals(drugType);
        String prefix = useZ ? APPROVAL_PREFIX_Z : APPROVAL_PREFIX_H;
        int defaultSeq = useZ ? 53020001 : 20000001;

        LambdaQueryWrapper<DrugInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(DrugInfo::getApprovalNum, prefix)
                .orderByDesc(DrugInfo::getApprovalNum)
                .last("LIMIT 1");

        DrugInfo last = getOne(wrapper, false);
        int nextSeq = defaultSeq;
        if (last != null && last.getApprovalNum() != null && last.getApprovalNum().startsWith(prefix)) {
            try {
                nextSeq = Integer.parseInt(last.getApprovalNum().substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                nextSeq = defaultSeq;
            }
        }

        return prefix + String.format("%08d", nextSeq);
    }
}
