package com.campus.express.mapper;

import com.campus.express.entity.WalletAccount;
import com.campus.express.entity.WalletTransaction;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 钱包账户与资金流水 */
public interface WalletMapper {

    WalletAccount selectByUser(@Param("userId") Long userId);

    int insertAccount(@Param("userId") Long userId);

    /** 入账（RECHARGE/EARN/REFUND/ADJUST），返回入账后余额；账户冻结返回 null */
    BigDecimal addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount,
                          @Param("kind") String kind);

    /** 出账（余额充足才扣），返回扣款后余额；不足返回 null */
    BigDecimal subBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    int insertTx(WalletTransaction tx);

    List<WalletTransaction> selectTxPage(@Param("userId") Long userId, @Param("type") String type,
                                         @Param("keyword") String keyword,
                                         @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                                         @Param("offset") int offset, @Param("limit") int limit);

    long countTxPage(@Param("userId") Long userId, @Param("type") String type,
                     @Param("keyword") String keyword);

    List<Map<String, Object>> selectAccounts(@Param("keyword") String keyword,
                                             @Param("sortCol") String sortCol, @Param("sortDir") String sortDir,
                                             @Param("offset") int offset, @Param("limit") int limit);

    long countAccounts(@Param("keyword") String keyword);

    Map<String, Object> platformSummary();

    List<Map<String, Object>> dailyFlow(@Param("days") int days);

    List<Map<String, Object>> typeDistribution();
}