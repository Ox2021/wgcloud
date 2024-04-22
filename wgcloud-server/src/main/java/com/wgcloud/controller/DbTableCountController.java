package com.wgcloud.controller;

import com.github.pagehelper.PageInfo;
import com.wgcloud.entity.DbTableCount;
import com.wgcloud.service.DbInfoService;
import com.wgcloud.service.DbTableCountService;
import com.wgcloud.service.DbTableService;
import com.wgcloud.service.LogInfoService;
import com.wgcloud.util.PageUtil;
import com.wgcloud.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/dbTableCount")
public class DbTableCountController {


    private static final Logger logger = LoggerFactory.getLogger(DbTableCountController.class);

    @Resource
    private DbInfoService dbInfoService;
    @Resource
    private DbTableService dbTableService;
    @Resource
    private DbTableCountService dbTableCountService;
    @Resource
    private LogInfoService logInfoService;


    /**
     * 根据条件查询数据源表统计信息列表
     *
     * @param dbTableCount 数据源表统计信息查询条件
     * @param model Spring MVC中的Model对象，用于存储处理结果和向视图传递数据
     * @return 数据源表统计信息列表视图名称
     */
    @RequestMapping(value = "list")
    public String dbTableCountList(DbTableCount dbTableCount, Model model) {
        // 创建查询参数Map对象，用于存储查询条件
        Map<String, Object> params = new HashMap<>();
        try {
            // 调用数据表统计服务的查询方法，根据查询条件和分页信息进行查询
            PageInfo pageInfo = dbTableCountService.selectByParams(params, dbTableCount.getPage(), dbTableCount.getPageSize());

            // 初始化分页信息到Model中，以便前端页面使用
            PageUtil.initPageNumber(pageInfo, model);

            // 设置页面URL，用于生成分页链接
            model.addAttribute("pageUrl", "/dbTableCount/list?1=1");

            // 将查询到的分页信息存入Model中，供前端页面渲染
            model.addAttribute("page", pageInfo);
        } catch (Exception e) {
            // 发生异常时记录错误日志和异常信息，并保存到操作日志中
            logger.error("查询数据源表统计信息错误", e);
            logInfoService.save("查询数据源表统计信息错误", e.toString(), StaticKeys.LOG_ERROR);

        }

        // 返回数据源表统计信息列表视图名称
        return "dbTableCount/list";
    }


    /**
     * 保存数据源表统计信息
     *
     * @param DbTableCount
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "save")
    public String saveDbTableCount(DbTableCount DbTableCount, Model model, HttpServletRequest request) {
        try {
            dbTableCountService.save(DbTableCount);
        } catch (Exception e) {
            logger.error("保存数据源表统计错误：", e);
            logInfoService.save("保存数据源表统计错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return "redirect:/dbTableCount/list";
    }


    /**
     * 删除数据源表统计
     *
     * @param model Spring MVC中的Model对象，用于存储处理结果和向视图传递数据
     * @param request HTTP请求对象，用于获取请求参数
     * @param redirectAttributes Spring MVC中的RedirectAttributes对象，用于重定向时传递消息
     * @return
     */
    @RequestMapping(value = "del")
    public String delete(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // 错误信息提示字符串
        String errorMsg = "删除数据源表统计信息错误：";
        // 创建数据源表统计对象
        DbTableCount DbTableCount = new DbTableCount();
        try {
            // 检查请求参数中是否包含"id"
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                // 根据请求参数中的"id"获取对应的数据源表统计信息对象
                DbTableCount = dbTableCountService.selectById(request.getParameter("id"));
                // 调用数据源表统计服务的删除方法，删除指定ID的数据源表统计信息
                dbTableCountService.deleteById(request.getParameter("id").split(","));
            }
        } catch (Exception e) {
            // 发生异常时记录错误日志和异常信息，并保存到操作日志中
            logger.error(errorMsg, e);
            logInfoService.save(errorMsg, e.toString(), StaticKeys.LOG_ERROR);
        }

        // 重定向到数据源表统计信息列表页面
        return "redirect:/dbTableCount/list";
    }


}
