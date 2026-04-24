package com.sfc.flowtest.manuscript.vo;

/**
 * 稿件列表符合条件的总条数。
 */
public class ManuscriptListTotalVO {
    private long total;

    public ManuscriptListTotalVO() {
    }

    public ManuscriptListTotalVO(long total) {
        this.total = total;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
