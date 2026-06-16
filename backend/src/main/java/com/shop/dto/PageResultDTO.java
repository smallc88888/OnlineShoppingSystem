package com.shop.dto;

import java.util.List;

/**
 * 通用分页返回对象 (泛型设计，方便后续复用)
 */
public class PageResultDTO<T> {
    private long total;       // 总记录数
    private int totalPages;   // 总页数
    private int page;         // 当前页码
    private int pageSize;     // 每页条数
    private List<T> items;    // 当前页的数据列表

    public PageResultDTO(long total, int totalPages, int page, int pageSize, List<T> items) {
        this.total = total;
        this.totalPages = totalPages;
        this.page = page;
        this.pageSize = pageSize;
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}