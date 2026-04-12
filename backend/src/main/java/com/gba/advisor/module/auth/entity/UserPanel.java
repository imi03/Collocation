package com.gba.advisor.module.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_panel")
public class UserPanel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 面板数值 JSON，如 {"atk":1200,"def":800,"hp":5000} */
    private String panelData;

    /** 数据来源：ocr | manual */
    private String source;

    /** 面板名称，用户自定义，如"毕业面板" */
    private String panelName;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
