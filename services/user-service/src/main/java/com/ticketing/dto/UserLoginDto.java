package com.ticketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.experimental.Accessors;
import lombok.Data;

@Data
@Accessors(chain = true)
public class UserLoginDto {

    @Email(message = "請輸入正確的 Email 格式")
    @NotBlank(message = "Email 不能為空")
    private String email;

    @NotBlank(message = "密碼不能為空")
    private String password;

}
