package com.campus.express.controller;

import com.campus.express.common.OpLog;
import com.campus.express.common.R;
import com.campus.express.entity.PhoneContact;
import com.campus.express.entity.ReceivePreference;
import com.campus.express.entity.UserAddress;
import com.campus.express.security.UserContext;
import com.campus.express.service.ProfileService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 地址簿 / 收货偏好 / 电话簿 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/addresses")
    public R<List<UserAddress>> addresses() {
        return R.ok(profileService.addresses(UserContext.require().getUid()));
    }

    @PostMapping("/addresses")
    @OpLog(module = "profile", action = "新增收货地址")
    public R<UserAddress> addAddress(@RequestBody Map<String, Object> body) {
        return R.ok(profileService.saveAddress(UserContext.require().getUid(), null, body));
    }

    @PutMapping("/addresses/{id}")
    @OpLog(module = "profile", action = "修改收货地址")
    public R<UserAddress> updateAddress(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(profileService.saveAddress(UserContext.require().getUid(), id, body));
    }

    @DeleteMapping("/addresses/{id}")
    @OpLog(module = "profile", action = "删除收货地址")
    public R<Void> deleteAddress(@PathVariable Long id) {
        profileService.deleteAddress(UserContext.require().getUid(), id);
        return R.ok();
    }

    @PostMapping("/addresses/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        profileService.setDefaultAddress(UserContext.require().getUid(), id);
        return R.ok();
    }

    @GetMapping("/preference")
    public R<ReceivePreference> preference() {
        return R.ok(profileService.preference(UserContext.require().getUid()));
    }

    @PutMapping("/preference")
    @OpLog(module = "profile", action = "保存收货偏好")
    public R<ReceivePreference> savePreference(@RequestBody Map<String, Object> body) {
        return R.ok(profileService.savePreference(UserContext.require().getUid(), body));
    }

    @GetMapping("/contacts")
    public R<List<PhoneContact>> contacts() {
        return R.ok(profileService.contacts(UserContext.require().getUid()));
    }

    @PostMapping("/contacts")
    @OpLog(module = "profile", action = "新增常用联系人")
    public R<PhoneContact> addContact(@RequestBody Map<String, Object> body) {
        return R.ok(profileService.addContact(UserContext.require().getUid(), body));
    }

    @DeleteMapping("/contacts/{id}")
    public R<Void> deleteContact(@PathVariable Long id) {
        profileService.deleteContact(UserContext.require().getUid(), id);
        return R.ok();
    }
}