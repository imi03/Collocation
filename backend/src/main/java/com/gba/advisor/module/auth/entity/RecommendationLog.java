package com.gba.advisor.module.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recommendation_log")
public class RecommendationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 触发推荐的用户 ID */
    private Long userId;

    /** 基于哪个面板快照 */
    private Long panelId;

    /** 推荐类型：equipment | trait */
    private String recommendType;

    /** 推荐的装备或特质 ID */
    private Long targetId;

    /** 推荐的装备或特质名称（冗余存储，避免联表） */
    private String targetName;

    /** 替换前伤害数值 */
    private BigDecimal baseDamage;

    /** 替换后伤害数值 */
    private BigDecimal newDamage;

    /** 伤害变化率（%），正数为提升，负数为下降 */
    private BigDecimal damageDeltaPct;

    /** 替换前坦度数值 */
    private BigDecimal baseTank;

    /** 替换后坦度数值 */
    private BigDecimal newTank;

    /** 坦度变化率（%） */
    private BigDecimal tankDeltaPct;

    /** 综合得分：伤害每+1%得1分，坦度每+2%得1分 */
    private BigDecimal totalScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
