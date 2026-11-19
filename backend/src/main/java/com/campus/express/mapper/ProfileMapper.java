package com.campus.express.mapper;

import com.campus.express.entity.PhoneContact;
import com.campus.express.entity.ReceivePreference;
import com.campus.express.entity.UserAddress;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 地址簿 / 收货偏好 / 电话簿 */
public interface ProfileMapper {

    // ---------- 地址簿 ----------

    int insertAddress(UserAddress address);

    UserAddress selectAddress(@Param("id") Long id);

    List<UserAddress> selectAddresses(@Param("userId") Long userId);

    int updateAddress(UserAddress address);

    int deleteAddress(@Param("id") Long id, @Param("userId") Long userId);

    int clearDefaultAddress(@Param("userId") Long userId);

    long countAddresses(@Param("userId") Long userId);

    // ---------- 收货偏好 ----------

    ReceivePreference selectPreference(@Param("userId") Long userId);

    int upsertPreference(ReceivePreference preference);

    // ---------- 电话簿 ----------

    int insertContact(PhoneContact contact);

    List<PhoneContact> selectContacts(@Param("userId") Long userId);

    int deleteContact(@Param("id") Long id, @Param("userId") Long userId);

    long countContacts(@Param("userId") Long userId);
}