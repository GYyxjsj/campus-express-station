package com.campus.express.mapper;

import com.campus.express.entity.ServiceTicket;
import com.campus.express.entity.TicketReply;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 客服工单 + 回复 */
public interface TicketMapper {

    int insert(ServiceTicket ticket);

    ServiceTicket selectById(@Param("id") Long id);

    List<ServiceTicket> selectPage(@Param("userId") Long userId, @Param("status") String status,
                                   @Param("unread") Boolean unread, @Param("keyword") String keyword,
                                   @Param("offset") int offset, @Param("limit") int limit);

    long countPage(@Param("userId") Long userId, @Param("status") String status,
                   @Param("unread") Boolean unread, @Param("keyword") String keyword);

    int markRead(@Param("id") Long id);

    /** 员工开始处理：PENDING → PROCESSING，记录处理人 */
    int markProcessing(@Param("id") Long id, @Param("handlerId") Long handlerId);

    /** 处理完成：PENDING/PROCESSING → DONE */
    int markDone(@Param("id") Long id, @Param("handlerId") Long handlerId);

    /** 用户主动关闭 */
    int closeByUser(@Param("id") Long id, @Param("userId") Long userId);

    long countByStatus(@Param("status") String status);

    long countUnread();

    // ---------- 回复 ----------

    int insertReply(TicketReply reply);

    List<TicketReply> selectReplies(@Param("ticketId") Long ticketId);
}