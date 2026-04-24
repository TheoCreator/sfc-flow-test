package com.sfc.flowtest.manuscript.vo;

/**
 * 通用ID响应对象。
 */
public class IdVO {
    private Long id;

    public IdVO() {
    }

    public IdVO(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
