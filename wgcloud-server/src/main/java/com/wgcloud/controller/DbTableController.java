package com.wgcloud.controller;

import cn.hutool.json.JSONUtil;
import com.github.pagehelper.PageInfo;
import com.wgcloud.entity.DbInfo;
import com.wgcloud.entity.DbTable;
import com.wgcloud.entity.DbTableCount;
import com.wgcloud.service.DbInfoService;
import com.wgcloud.service.DbTableCountService;
import com.wgcloud.service.DbTableService;
import com.wgcloud.service.LogInfoService;
import com.wgcloud.util.PageUtil;
import com.wgcloud.util.jdbc.RDSConnection;
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
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dbTable")
public class DbTableController {


    private static final Logger logger = LoggerFactory.getLogger(DbTableController.class);

    @Resource
    private DbInfoService dbInfoService;
    @Resource
    private DbTableService dbTableService;
    @Resource
    private DbTableCountService dbTableCountService;
    @Resource
    private LogInfoService logInfoService;


    /**
     * 查询数据库表列表
     *
     * @param DbTable  数据库表对象，包含分页信息和其他查询条件
     * @param model    模型对象，用于向视图传递数据
     * @return         返回数据库表列表视图的名称
     */
    @RequestMapping(value = "list")
    public String DbTableList(DbTable DbTable, Model model) {
        // 创建参数Map以存储查询条件
        Map<String, Object> params = new HashMap<>();
        try {
            // 根据参数查询数据库表信息，并进行分页处理
            PageInfo<DbTable> pageInfo = dbTableService.selectByParams(params, DbTable.getPage(), DbTable.getPageSize());
            // 初始化分页信息并添加到模型中
            PageUtil.initPageNumber(pageInfo, model);

            // 查询所有数据库信息
            List<DbInfo> dbInfoList = dbInfoService.selectAllByParams(params);

            // 遍历数据库表列表，将对应的数据库别名设置为表名
            for (DbTable dbTable : pageInfo.getList()) {
                for (DbInfo dbInfo : dbInfoList) {
                    if (dbInfo.getId().equals(dbTable.getDbInfoId())) {
                        dbTable.setTableName(dbInfo.getAliasName());
                    }
                }
            }

            // 添加页面URL和分页信息到模型中
            model.addAttribute("pageUrl", "/dbTable/list?1=1");
            model.addAttribute("page", pageInfo);
        } catch (Exception e) {
            // 捕获异常并记录错误日志
            logger.error("查询数据表信息错误", e);
            // 将错误信息保存到日志表中
            logInfoService.save("查询数据表信息错误", e.toString(), StaticKeys.LOG_ERROR);

        }
        // 返回数据库表列表视图的名称
        return "mysql/list";
    }


    /**
     * 保存数据源表信息
     *
     * @param DbTable
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "save")
    public String saveDbTable(DbTable DbTable, Model model, HttpServletRequest request) {
        try {
            String whereVal = DbTable.getWhereVal().toLowerCase();
            if (!StringUtils.isEmpty(whereVal)) {
                String[] sqlinkeys = RDSConnection.SQL_INKEYS.split(",");
                for (String sqlinkey : sqlinkeys) {
                    if (whereVal.indexOf(sqlinkey) > -1) {
                        model.addAttribute("dbTable", DbTable);
                        List<DbInfo> dbInfoList = dbInfoService.selectAllByParams(new HashMap<>());
                        model.addAttribute("dbInfoList", dbInfoList);
                        model.addAttribute("msg", "where语句含有sql敏感字符" + sqlinkey + "，请检查");
                        return "mysql/add";
                    }
                }
            }
            if (StringUtils.isEmpty(DbTable.getId())) {
                dbTableService.save(DbTable);
            } else {
                dbTableService.updateById(DbTable);
            }
        } catch (Exception e) {
            logger.error("保存数据表错误：", e);
            logInfoService.save("保存数据表错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return "redirect:/dbTable/list";
    }


    /**
     * 查看数据源表信息
     *
     * @param DbTable
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "edit")
    public String editDbTable(DbTable DbTable, Model model, HttpServletRequest request) {
        try {
            String id = request.getParameter("id");
            DbTable dbTableInfo = new DbTable();
            if (!StringUtils.isEmpty(id)) {
                dbTableInfo = dbTableService.selectById(id);
            }
            List<DbInfo> dbInfoList = dbInfoService.selectAllByParams(new HashMap<>());
            model.addAttribute("dbInfoList", dbInfoList);
            model.addAttribute("dbTable", dbTableInfo);
        } catch (Exception e) {
            logger.error("查看数据表错误：", e);
            logInfoService.save("查看数据表错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return "mysql/add";
    }

    /**
     * 查看数据源表图表统计信息
     *
     * @param DbTable
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "viewChart")
    public String viewChartDbTable(DbTable DbTable, Model model, HttpServletRequest request) {
        try {
            String id = request.getParameter("id");
            if (!StringUtils.isEmpty(id)) {
                DbTable dbTableInfo = dbTableService.selectById(id);
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("dbTableId", id);
                List<DbTableCount> dbTableCounts = dbTableCountService.selectAllByParams(params);
                model.addAttribute("dbTableCounts", JSONUtil.parseArray(dbTableCounts));
                model.addAttribute("dbTable", dbTableInfo);
                String sql = RDSConnection.query_table_count.replace("{tableName}", dbTableInfo.getTableName()) + dbTableInfo.getWhereVal();
                model.addAttribute("sqlCount", sql);
            }
        } catch (Exception e) {
            logger.error("查看数据表图表统计错误：", e);
            logInfoService.save("查看数据表图表统计错误", e.toString(), StaticKeys.LOG_ERROR);
        }
        return "mysql/view";
    }


    /**
     * 删除数据源表
     *
     * @param model
     * @param request
     * @param redirectAttributes
     * @return
     */
    @RequestMapping(value = "del")
    public String delete(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // 错误信息提示字符串
        String errorMsg = "删除数据源表信息错误：";
        try {
            // 检查请求参数中是否包含"id"
            if (!StringUtils.isEmpty(request.getParameter("id"))) {
                // 根据请求参数中的"id"获取对应的数据表对象
                DbTable dbTable = dbTableService.selectById(request.getParameter("id"));

                // 记录操作日志：保存删除数据表的操作信息
                logInfoService.save("删除数据表：" + dbTable.getTableName(), "删除数据表：" + dbTable.getTableName(), StaticKeys.LOG_ERROR);

                // 调用数据表服务的删除方法，删除指定ID的数据表
                dbTableService.deleteById(request.getParameter("id").split(","));
            }
        } catch (Exception e) {
            // 发生异常时记录异常信息及堆栈信息，并保存到操作日志中
            logger.error(errorMsg, e);
            logInfoService.save(errorMsg, e.toString(), StaticKeys.LOG_ERROR);
        }

        // 重定向到数据表列表页面
        return "redirect:/dbTable/list";
    }


}
