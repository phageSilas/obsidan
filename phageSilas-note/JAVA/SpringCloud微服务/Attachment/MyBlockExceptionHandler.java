package com.ggg456.exception;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggg456.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;

@Component
public class MyBlockExceptionHandler implements BlockExceptionHandler {

    private ObjectMapper objectMapper = new ObjectMapper(); // Jackson, 用于将对象转换为JSON字符串
    /**
     * 自定义限流异常处理逻辑
     * @param request 请求
     * @param response 响应
     * @param resourceName 资源名称
     * @param e 限流异常
     * @throws Exception 抛出异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       String resourceName,BlockException e) throws Exception {

        response.setContentType("application/json;charset=utf-8"); // 设置响应的Content-Type为JSON, 并设置编码为UTF-8

        PrintWriter printer = response.getWriter(); // 获取响应的输出流

        Result result = Result.error(500, resourceName+" 被Sentinal限制,原因: "+e.getClass());

        String json = objectMapper.writeValueAsString(result); // 将Result对象转换为JSON字符串
        printer.write(json); // 将JSON字符串写入响应输出流


        printer.flush(); // 刷新输出流
        printer.close(); // 关闭输出流
    }


}
