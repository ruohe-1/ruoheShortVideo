package com.shortvideo.pojo.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CursorPageVO<T> {
    //数据列表
    private List<T> records;
    //下一页的游标(最后一条数据的ID)
    private Long lastId;
    //是否还有下一页
    private Boolean hasMore;
    //当前每页大小
    private Integer size;


    public CursorPageVO(List<T> records, Long lastId, Boolean hasMore, Integer pageSize) {
        this.records = records;
        this.lastId = lastId;
        this.hasMore = hasMore;
        this.size = pageSize;
    }
}
