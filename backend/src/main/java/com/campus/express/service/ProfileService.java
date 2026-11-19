package com.campus.express.service;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCodes;
import com.campus.express.entity.PhoneContact;
import com.campus.express.entity.ReceivePreference;
import com.campus.express.entity.UserAddress;
import com.campus.express.mapper.ProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 个人资料扩展：地址簿（含经纬度定位）、收货偏好、电话簿 */
@Service
public class ProfileService {

    private static final int ADDRESS_LIMIT = 20;
    private static final int CONTACT_LIMIT = 50;
    private static final Set<String> TIME_SLOTS = set("ANY", "MORNING", "AFTERNOON", "EVENING");
    private static final Set<String> METHODS = set("SELF", "PROXY", "DOOR");
    private static final Set<String> CHANNELS = set("IN_APP", "SMS", "EMAIL", "ALL");

    private final ProfileMapper profileMapper;

    public ProfileService(ProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    // ---------- 地址簿 ----------

    public List<UserAddress> addresses(Long uid) {
        return profileMapper.selectAddresses(uid);
    }

    @Transactional
    public UserAddress saveAddress(Long uid, Long id, Map<String, Object> body) {
        UserAddress address = new UserAddress();
        address.setUserId(uid);
        address.setLabel(limit(str(body.get("label"), "收货地址"), 32));
        address.setContact(required(body.get("contact"), "联系人", 32));
        address.setPhone(phone(body.get("phone")));
        address.setRegion(limit(str(body.get("region"), ""), 120));
        address.setDetail(required(body.get("detail"), "详细地址", 200));
        address.setLongitude(coord(body.get("longitude"), 180));
        address.setLatitude(coord(body.get("latitude"), 90));
        address.setIsDefault(truthy(body.get("isDefault")) ? 1 : 0);

        if (id == null) {
            if (profileMapper.countAddresses(uid) >= ADDRESS_LIMIT) {
                throw new BizException(ErrorCodes.PARAM_INVALID, "地址簿最多保存 " + ADDRESS_LIMIT + " 条");
            }
            if (address.getIsDefault() == 1) {
                profileMapper.clearDefaultAddress(uid);
            }
            profileMapper.insertAddress(address);
        } else {
            requireOwned(uid, id);
            address.setId(id);
            if (address.getIsDefault() == 1) {
                profileMapper.clearDefaultAddress(uid);
            }
            profileMapper.updateAddress(address);
        }
        return profileMapper.selectAddress(id == null ? address.getId() : id);
    }

    @Transactional
    public void deleteAddress(Long uid, Long id) {
        requireOwned(uid, id);
        profileMapper.deleteAddress(id, uid);
    }

    @Transactional
    public void setDefaultAddress(Long uid, Long id) {
        requireOwned(uid, id);
        profileMapper.clearDefaultAddress(uid);
        UserAddress address = profileMapper.selectAddress(id);
        address.setIsDefault(1);
        profileMapper.updateAddress(address);
    }

    private void requireOwned(Long uid, Long id) {
        UserAddress address = profileMapper.selectAddress(id);
        if (address == null || !uid.equals(address.getUserId())) {
            throw new BizException(ErrorCodes.ADDRESS_NOT_FOUND, "地址不存在或不属于您");
        }
    }

    // ---------- 收货偏好 ----------

    public ReceivePreference preference(Long uid) {
        ReceivePreference p = profileMapper.selectPreference(uid);
        if (p == null) {
            p = new ReceivePreference();
            p.setUserId(uid);
            p.setTimeSlot("ANY");
            p.setDeliverMethod("SELF");
            p.setNotifyChannel("IN_APP");
            p.setQuietHours("");
            p.setNoCall(0);
            p.setFragileCare(0);
            p.setAutoProxy(0);
            p.setNote("");
        }
        return p;
    }

    @Transactional
    public ReceivePreference savePreference(Long uid, Map<String, Object> body) {
        ReceivePreference p = new ReceivePreference();
        p.setUserId(uid);
        p.setTimeSlot(enumVal(body.get("timeSlot"), TIME_SLOTS, "ANY"));
        p.setDeliverMethod(enumVal(body.get("deliverMethod"), METHODS, "SELF"));
        p.setNotifyChannel(enumVal(body.get("notifyChannel"), CHANNELS, "IN_APP"));
        p.setQuietHours(limit(str(body.get("quietHours"), ""), 32));
        p.setNoCall(truthy(body.get("noCall")) ? 1 : 0);
        p.setFragileCare(truthy(body.get("fragileCare")) ? 1 : 0);
        p.setAutoProxy(truthy(body.get("autoProxy")) ? 1 : 0);
        p.setNote(limit(str(body.get("note"), ""), 200));
        profileMapper.upsertPreference(p);
        return profileMapper.selectPreference(uid);
    }

    // ---------- 电话簿 ----------

    public List<PhoneContact> contacts(Long uid) {
        return profileMapper.selectContacts(uid);
    }

    @Transactional
    public PhoneContact addContact(Long uid, Map<String, Object> body) {
        if (profileMapper.countContacts(uid) >= CONTACT_LIMIT) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "电话簿最多保存 " + CONTACT_LIMIT + " 条");
        }
        PhoneContact contact = new PhoneContact();
        contact.setUserId(uid);
        contact.setName(required(body.get("name"), "姓名", 32));
        contact.setPhone(phone(body.get("phone")));
        contact.setRelation(limit(str(body.get("relation"), ""), 16));
        contact.setIsStarred(truthy(body.get("isStarred")) ? 1 : 0);
        contact.setRemark(limit(str(body.get("remark"), ""), 120));
        profileMapper.insertContact(contact);
        return profileMapper.selectContacts(uid).stream()
                .filter(c -> c.getId().equals(contact.getId())).findFirst().orElse(contact);
    }

    @Transactional
    public void deleteContact(Long uid, Long id) {
        profileMapper.deleteContact(id, uid);
    }

    // ---------- 工具 ----------

    private static Set<String> set(String... vals) {
        return new HashSet<>(Arrays.asList(vals));
    }

    private static String str(Object o, String def) {
        String s = o == null ? "" : String.valueOf(o).trim();
        return s.isEmpty() ? def : s;
    }

    private static String limit(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String required(Object o, String field, int max) {
        String s = str(o, "");
        if (s.isEmpty()) {
            throw new BizException(ErrorCodes.PARAM_INVALID, field + "必填");
        }
        if (s.length() > max) {
            throw new BizException(ErrorCodes.PARAM_INVALID, field + "不能超过 " + max + " 字");
        }
        return s;
    }

    private static String phone(Object o) {
        String s = str(o, "");
        if (!s.matches("^1[3-9]\\d{9}$") && !s.matches("^0\\d{2,3}-?\\d{7,8}$")) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "请填写有效的手机号或座机号");
        }
        return s;
    }

    private static BigDecimal coord(Object o, int abs) {
        if (o == null || String.valueOf(o).trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(String.valueOf(o).trim());
            if (v.abs().compareTo(BigDecimal.valueOf(abs)) > 0) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "定位坐标无效");
        }
    }

    private static boolean truthy(Object o) {
        if (o == null) { return false; }
        String s = String.valueOf(o).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    private static String enumVal(Object o, Set<String> allowed, String def) {
        String s = str(o, def).toUpperCase();
        if (!allowed.contains(s)) {
            throw new BizException(ErrorCodes.PARAM_INVALID, "选项值无效：" + s);
        }
        return s;
    }
}