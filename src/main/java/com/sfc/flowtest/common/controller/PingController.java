package com.sfc.flowtest.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基础连通性探针接口，用于快速判断应用是否可访问。
 */
@RestController
public class PingController {
    /**
     * 返回固定字符串 "ok"。
     *
     * @return 连通性结果
     */
    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }
}
