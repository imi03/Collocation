package com.gba.advisor.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SelectProfessionRequest {

    @NotBlank(message = "职业不能为空")
    private String profession;
}
