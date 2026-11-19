package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.common.PageResult;
import com.campus.express.dto.PolicyDTO;
import com.campus.express.entity.Policy;
import com.campus.express.mapper.PolicyMapper;
import com.campus.express.security.LoginUser;
import com.campus.express.vo.PolicyVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 政策协议：发布/版本管理、用户签署（按版本留痕）、必签门禁。
 */
@Service
public class PolicyService {

    private final PolicyMapper policyMapper;
    private final NotificationService notificationService;

    public PolicyService(PolicyMapper policyMapper, NotificationService notificationService) {
        this.policyMapper = policyMapper;
        this.notificationService = notificationService;
    }

    public List<PolicyVO> list(Long userId, String category, String keyword) {
        return policyMapper.selectList(userId, true, category, keyword);
    }

    /** 登录后门禁：尚未签署的必签协议 */
    public List<PolicyVO> pending(Long userId) {
        return policyMapper.selectPending(userId);
    }

    public PolicyVO detail(Long userId, String code) {
        PolicyVO vo = policyMapper.selectVoByCode(userId, code);
        if (vo == null) {
            throw new BizException(ErrorCodes.POLICY_NOT_FOUND, "协议不存在");
        }
        return vo;
    }

    /** 批量签署（按当前发布版本留痕，重复签署幂等） */
    @Transactional
    public int accept(Long userId, List<Long> policyIds, String ip) {
        int count = 0;
        for (Long id : policyIds) {
            Policy policy = policyMapper.selectById(id);
            if (policy == null) {
                throw new BizException(ErrorCodes.POLICY_NOT_FOUND, "协议不存在");
            }
            if (policy.getStatus() == null || policy.getStatus() != 1) {
                throw new BizException(ErrorCodes.POLICY_NOT_PUBLISHED, "《" + policy.getTitle() + "》尚未发布");
            }
            count += policyMapper.insertAcceptance(userId, id, policy.getVersion(), ip);
        }
        return count;
    }

    public Map<String, Object> myAcceptances(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("policies", policyMapper.selectList(userId, true, null, null));
        result.put("pending", policyMapper.selectPending(userId));
        return result;
    }

    // ---------- 管理端 ----------

    public PageResult<PolicyVO> adminPage(String category, Integer status, String keyword,
                                          String sortKey, String order, int page, int size) {
        long total = policyMapper.countAdminPage(category, status, keyword);
        return new PageResult<>(total,
                policyMapper.selectAdminPage(category, status, keyword,
                        com.campus.express.common.Sorts.policy(sortKey),
                        com.campus.express.common.Sorts.dir(order), (page - 1) * size, size));
    }

    @Transactional
    public Policy create(LoginUser admin, PolicyDTO dto) {
        Policy policy = new Policy();
        apply(policy, dto);
        policy.setCode(dto.getCode().trim().toUpperCase());
        policy.setPublisherId(admin.getUid());
        policy.setEffectiveAt(OffsetDateTime.now());
        try {
            policyMapper.insert(policy);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCodes.POLICY_CODE_EXISTS, "协议编码已存在");
        }
        return policy;
    }

    @Transactional
    public void update(LoginUser admin, Long id, PolicyDTO dto) {
        Policy policy = policyMapper.selectById(id);
        if (policy == null) {
            throw new BizException(ErrorCodes.POLICY_NOT_FOUND, "协议不存在");
        }
        String oldVersion = policy.getVersion();
        apply(policy, dto);
        policy.setPublisherId(admin.getUid());
        policyMapper.update(policy);
        // 版本升级且为必签协议 → 通知全体已签署用户重新签署
        if (policy.getRequired() != null && policy.getRequired() == 1
                && !oldVersion.equals(policy.getVersion())
                && policy.getStatus() != null && policy.getStatus() == 1) {
            notifyRevision(policy);
        }
    }

    private void notifyRevision(Policy policy) {
        List<Map<String, Object>> signers = policyMapper.selectAcceptances(policy.getId(), null,
                "a.accepted_at", "DESC", 0, 500);
        for (Map<String, Object> signer : signers) {
            Object uid = signer.get("userId");
            if (uid instanceof Number) {
                notificationService.sendBiz(((Number) uid).longValue(), null, "POLICY_UPDATE",
                        "协议更新待签署",
                        String.format("《%s》已更新至 v%s，属于必签协议，请前往「政策协议」重新阅读并签署后继续使用相关服务。",
                                policy.getTitle(), policy.getVersion()));
            }
        }
    }

    @Transactional
    public void setStatus(Long id, Integer status) {
        if (policyMapper.selectById(id) == null) {
            throw new BizException(ErrorCodes.POLICY_NOT_FOUND, "协议不存在");
        }
        policyMapper.updateStatus(id, status != null && status == 1 ? 1 : 0);
    }

    public PageResult<Map<String, Object>> acceptances(Long policyId, String keyword,
                                                       String sortKey, String order, int page, int size) {
        long total = policyMapper.countAcceptances(policyId, keyword);
        return new PageResult<>(total,
                policyMapper.selectAcceptances(policyId, keyword,
                        com.campus.express.common.Sorts.acceptance(sortKey),
                        com.campus.express.common.Sorts.dir(order), (page - 1) * size, size));
    }

    public Map<String, Object> stats() {
        return policyMapper.acceptanceStats();
    }

    private void apply(Policy policy, PolicyDTO dto) {
        policy.setTitle(dto.getTitle().trim());
        policy.setVersion(dto.getVersion().trim());
        policy.setCategory(dto.getCategory() == null ? "SERVICE" : dto.getCategory().trim().toUpperCase());
        policy.setSummary(dto.getSummary());
        policy.setContent(dto.getContent());
        policy.setRequired(dto.getRequired() == null ? 0 : dto.getRequired());
        policy.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }
}