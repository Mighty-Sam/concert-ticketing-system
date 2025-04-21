package com.ticketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.experimental.Accessors;
import lombok.Data;

@Data
@Accessors(chain = true)
public class UserCreateDto {

    @NotBlank(message = "使用者名稱不能為空")
    private String name;

    @Email(message = "請輸入正確的 Email 格式")
    @NotBlank(message = "Email 不能為空")
    private String email;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, message = "密碼長度至少為 6 字元")
    private String password;

}
