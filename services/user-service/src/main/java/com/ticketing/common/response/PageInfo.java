package com.ticketing.common.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PageInfo {

    /***
     * 頁數
     */
    @Min(value = 0)
    @Max(value = Integer.MAX_VALUE)
    private Integer page;

    /***
     * 每頁筆數
     */
    @Min(value = 0)
    @Max(value = Integer.MAX_VALUE)
    private Integer pageSize;

    /***
     * 資料總筆數
     */
    private Long totalCount;

}
