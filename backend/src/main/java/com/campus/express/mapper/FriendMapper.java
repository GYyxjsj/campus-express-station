package com.campus.express.mapper;

import com.campus.express.entity.Friendship;
import com.campus.express.entity.InviteRebate;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/** 好友关系 + 邀请返利 */
public interface FriendMapper {

    int insert(Friendship friendship);

    Friendship selectById(@Param("id") Long id);

    /** 任意方向的一对关系 */
    Friendship selectPair(@Param("a") Long a, @Param("b") Long b);

    int accept(@Param("id") Long id);

    int delete(@Param("id") Long id);

    /** 我的好友列表（ACCEPTED，双向），联查对方姓名/学号/手机 */
    List<Map<String, Object>> selectFriends(@Param("userId") Long userId);

    /** 待我处理的申请（friend_id=我，PENDING） */
    List<Map<String, Object>> selectIncoming(@Param("userId") Long userId);

    /** 我发出的申请（user_id=我，PENDING） */
    List<Map<String, Object>> selectOutgoing(@Param("userId") Long userId);

    // ---------- 邀请返利 ----------

    int insertRebate(InviteRebate rebate);

    InviteRebate selectRebateByOrder(@Param("orderId") Long orderId);

    /** 我的返利记录（联查被邀请人姓名与订单号） */
    List<Map<String, Object>> selectRebatePage(@Param("inviterId") Long inviterId,
                                               @Param("offset") int offset, @Param("limit") int limit);

    long countRebatePage(@Param("inviterId") Long inviterId);

    java.math.BigDecimal sumRebate(@Param("inviterId") Long inviterId);
}