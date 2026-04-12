package com.gba.advisor.module.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("trait")
public class Trait {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 特质名称 */
    private String name;

    /** 所属职业中文名，NULL 表示全职业通用 */
    private String profession;

    /** 特质效果说明 */
    private String description;

    /** 计算方式文字说明（实际逻辑在 FormulaEngine 中实现） */
    private String calcFormula;

    /** 特质图标，阿里云 OSS 完整 URL */
    private String imageUrl;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}