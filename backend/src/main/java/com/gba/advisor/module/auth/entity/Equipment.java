package com.gba.advisor.module.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("equipment")
public class Equipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 装备名称 */
    private String name;

    /** 装备槽位：武器 / 头盔 / 胸甲 / 腿甲 / 饰品 等 */
    private String slot;

    /** 属性加成 JSON，如 {"atk":120,"def":80} */
    private String statBonuses;

    /** 装备描述 */
    private String description;

    /** 装备图标，阿里云 OSS 完整 URL */
    private String imageUrl;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

