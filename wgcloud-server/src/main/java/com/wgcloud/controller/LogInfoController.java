package com.wgcloud.controller;

import com.github.pagehelper.PageInfo;
import com.wgcloud.entity.LogInfo;
import com.wgcloud.service.LogInfoService;
import com.wgcloud.util.CodeUtil;
import com.wgcloud.util.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/log")
public class LogInfoController {


    private static final Logger logger = LoggerFactory.getLogger(LogInfoController.class);

    @Resource
    private LogInfoService logInfoService;

    /**
     * 根据条件查询日志信息列表
     *
     * @param logInfo 日志信息查询条件
     * @param model   Spring MVC中的Model对象，用于存储处理结果和向视图传递数据
     * @return 日志信息列表视图名称
     */
    @RequestMapping(value = "list")
    public String LogInfoList(LogInfo logInfo, Model model) {

        // 创建查询参数Map对象，用于存储查询条件
        Map<String, Object> params = new HashMap<>();
        try {
            // 创建StringBuffer对象，用于构建URL参数
            StringBuffer url = new StringBuffer();
            String hostname = null;
            // 检查日志信息中的主机名是否为空，若不为空则进行处理
            if (!StringUtils.isEmpty(logInfo.getHostname())) {
                // 对主机名进行解码处理
                hostname = CodeUtil.unescape(logInfo.getHostname());
                // 将解码后的主机名加入查询参数中，并去除首尾空格
                params.put("hostname", hostname.trim());
                // 构建URL参数
                url.append("&hostname=").append(CodeUtil.escape(hostname));
            }
            // 调用日志信息服务的查询方法，根据查询条件和分页信息进行查询
            PageInfo pageInfo = logInfoService.selectByParams(params, logInfo.getPage(), logInfo.getPageSize());
            // 初始化分页信息到Model中，以便前端页面使用
            PageUtil.initPageNumber(pageInfo, model);

            // 设置页面URL，用于生成分页链接
            model.addAttribute("pageUrl", "/log/list?1=1" + url.toString());
            // 将查询结果和查询条件存入Model中，供前端页面渲染
            model.addAttribute("page", pageInfo);
            model.addAttribute("logInfo", logInfo);
        } catch (Exception e) {
            // 发生异常时记录错误日志和异常信息
            logger.error("查询日志错误", e);
        }

        // 返回日志信息列表视图名称
        return "log/list";
    }

    /**
     * 查看日志信息
     *
     * @param model    Spring MVC中的Model对象，用于存储处理结果和向视图传递数据
     * @param request  HTTP请求对象，用于获取请求参数和属性
     * @return 查看日志信息页面的视图名称
     */
    @RequestMapping(value = "view")
    public String viewLogInfo(Model model, HttpServletRequest request) {
        // 获取请求参数中的日志ID
        String id = request.getParameter("id");
        // 创建日志信息对象
        LogInfo logInfo;
        try {
            // 根据日志ID查询日志信息
            logInfo = logInfoService.selectById(id);
            // 将查询到的日志信息存入Model中，供前端页面渲染
            model.addAttribute("logInfo", logInfo);
        } catch (Exception e) {
            // 发生异常时记录错误日志和异常信息
            logger.error("查看日志信息：", e);
        }

        // 返回查看日志信息页面的视图名称
        return "log/view";
    }

}
