package com.testplatform.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.Data;

import java.util.List;

/**
 * @author greada
 * @version 1.0.0
 */
@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(IPage<T> p) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setPage(p.getCurrent());
        pageResult.setRecords(p.getRecords());
        pageResult.setSize(p.getSize());
        pageResult.setTotal(p.getTotal());
        return pageResult;
    }
}
