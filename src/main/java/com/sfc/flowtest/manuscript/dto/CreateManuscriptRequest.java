package com.sfc.flowtest.manuscript.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建稿件请求参数。
 */
public class CreateManuscriptRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 500, message = "标题长度不能超过500")
    private String title;

    @NotBlank(message = "正文不能为空")
    private String body;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
