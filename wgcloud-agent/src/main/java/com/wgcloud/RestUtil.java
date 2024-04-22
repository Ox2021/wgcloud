package com.wgcloud;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @ClassName:RestUtil.java
 * @Description: RestUtil.java
 * REST请求工具类，用于发送HTTP请求并处理响应。
 */
@Component
public class RestUtil {

    // 自动注入RestTemplate实例和CommonConfig实例
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CommonConfig commonConfig;

    /**
     * 发送HTTP POST请求，携带JSON格式的数据体，并接收服务器响应。
     * @param url 请求的目标URL
     * @param jsonObject 待发送的JSON数据体
     * @return 服务器响应的字符串形式
     */
    public String post(String url, JSONObject jsonObject) {
        // 如果JSON数据体不为null，则添加wgToken字段，用于身份验证
        if (null != jsonObject) {
            jsonObject.put("wgToken", MD5Utils.GetMD5Code(commonConfig.getWgToken()));
        }
        // 设置请求头信息，指定Content-Type为JSON格式
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString());
        // 构建HTTP请求实体，包括请求体和请求头
        HttpEntity<String> httpEntity = new HttpEntity<>(JSONUtil.parse(jsonObject).toString(), headers);
        // 发送HTTP POST请求，并接收服务器响应
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
        // 返回服务器响应的字符串形式
        return responseEntity.getBody();
    }

    /**
     * 发送HTTP POST请求，不携带任何数据体，仅接收服务器响应。
     * @param url 请求的目标URL
     * @return 服务器响应的JSON对象形式
     */
    public JSONObject post(String url) {
        // 设置请求头信息，指定Content-Type为JSON格式
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString());
        // 构建HTTP请求实体，不携带请求体
        HttpEntity<String> httpEntity = new HttpEntity<>("", headers);
        // 发送HTTP POST请求，并接收服务器响应
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
        // 将服务器响应的字符串形式转换为JSON对象
        return JSONUtil.parseObj(responseEntity.getBody());
    }

    /**
     * 发送HTTP GET请求，仅接收服务器响应。
     * @param url 请求的目标URL
     * @return 服务器响应的JSON对象形式
     */
    public JSONObject get(String url) {
        // 发送HTTP GET请求，并接收服务器响应
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        // 将服务器响应的字符串形式转换为JSON对象
        return JSONUtil.parseObj(responseEntity.getBody());
    }

}
