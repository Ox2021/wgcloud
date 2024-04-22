package com.wgcloud.task;


import cn.hutool.core.collection.CollectionUtil;
import com.wgcloud.config.CommonConfig;
import com.wgcloud.entity.*;
import com.wgcloud.mapper.*;
import com.wgcloud.service.*;
import com.wgcloud.util.DateUtil;
import com.wgcloud.util.RestUtil;
import com.wgcloud.util.jdbc.ConnectionUtil;
import com.wgcloud.util.jdbc.RDSConnection;
import com.wgcloud.util.msg.WarnMailUtil;
import com.wgcloud.util.msg.WarnPools;
import com.wgcloud.util.staticvar.BatchData;
import com.wgcloud.util.staticvar.StaticKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName:ScheduledTask.java
 * @Description: ScheduledTask.java
 */
@Component
public class ScheduledTask {

    private Logger logger = LoggerFactory.getLogger(ScheduledTask.class);

    /**
     * 线程池
     */
    static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 40, 2, TimeUnit.MINUTES, new LinkedBlockingDeque<>());

    @Autowired
    SystemInfoService systemInfoService;
    @Autowired
    DeskStateService deskStateService;
    @Autowired
    LogInfoService logInfoService;
    @Autowired
    AppInfoService appInfoService;
    @Autowired
    CpuStateService cpuStateService;
    @Autowired
    MemStateService memStateService;
    @Autowired
    NetIoStateService netIoStateService;
    @Autowired
    SysLoadStateService sysLoadStateService;
    @Autowired
    TcpStateService tcpStateService;
    @Autowired
    AppStateService appStateService;
    @Autowired
    MailSetService mailSetService;
    @Autowired
    IntrusionInfoService intrusionInfoService;
    @Autowired
    HostInfoService hostInfoService;
    @Autowired
    DbInfoService dbInfoService;
    @Autowired
    DbTableService dbTableService;
    @Autowired
    DbTableCountService dbTableCountService;
    @Autowired
    HeathMonitorService heathMonitorService;
    @Autowired
    private RestUtil restUtil;
    @Autowired
    ConnectionUtil connectionUtil;
    @Autowired
    CommonConfig commonConfig;

    /**
     * 20秒后执行
     * 初始化操作
     */
    @Scheduled(initialDelay = 20000L, fixedRate = 600 * 60 * 1000)
    public void initTask() {
        try {
            // 初始化参数集合
            Map<String, Object> params = new HashMap<>();

            // 通过参数集合查询所有邮件设置
            List<MailSet> list = mailSetService.selectAllByParams(params);

            // 如果查询结果不为空
            if (list.size() > 0) {
                // 将查询到的第一个邮件设置对象存储在静态变量中，以供其他部分使用
                StaticKeys.mailSet = list.get(0);
            }
        } catch (Exception e) {
            // 捕获并记录初始化操作中的错误信息
            logger.error("初始化操作错误", e);
        }

    }


    /**
     * 300秒后执行
     * 检测主机是否已经下线，检测进程是否下线
     */
    @Scheduled(initialDelay = 300000L, fixedRate = 20 * 60 * 1000)
    public void hostDownCheckTask() {
        // 获取当前时间
        Date date = DateUtil.getNowTime();
        // 设置延迟时间，即超过该时间后未上报状态则视为已下线（单位：毫秒）
        long delayTime = 900 * 1000;

        try {
            // 查询所有主机信息
            Map<String, Object> params = new HashMap<>();
            List<SystemInfo> list = systemInfoService.selectAllByParams(params);

            // 如果查询结果不为空
            if (!CollectionUtil.isEmpty(list)) {
                // 初始化更新列表和日志信息列表
                List<SystemInfo> updateList = new ArrayList<>();
                List<LogInfo> logInfoList = new ArrayList<>();

                // 遍历所有主机信息
                for (SystemInfo systemInfo : list) {
                    // 计算当前时间与主机信息创建时间的时间差
                    Date createTime = systemInfo.getCreateTime();
                    long diff = date.getTime() - createTime.getTime();

                    // 如果时间差超过延迟时间
                    if (diff > delayTime) {
                        // 如果该主机已经发送过下线警报，则跳过
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(systemInfo.getId()))) {
                            continue;
                        }

                        // 更新主机状态为已下线
                        systemInfo.setState(StaticKeys.DOWN_STATE);

                        // 添加下线日志信息
                        LogInfo logInfo = new LogInfo();
                        logInfo.setHostname("主机下线：" + systemInfo.getHostname());
                        logInfo.setInfoContent("超过10分钟未上报状态，可能已下线：" + systemInfo.getHostname());
                        logInfo.setState(StaticKeys.LOG_ERROR);
                        logInfoList.add(logInfo);

                        // 添加到更新列表中
                        updateList.add(systemInfo);

                        // 异步发送主机下线警报邮件
                        Runnable runnable = () -> {
                            WarnMailUtil.sendHostDown(systemInfo, true);
                        };
                        executor.execute(runnable);
                    } else {
                        // 如果该主机已经发送过下线警报，则异步发送恢复上线警报邮件
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(systemInfo.getId()))) {
                            Runnable runnable = () -> {
                                WarnMailUtil.sendHostDown(systemInfo, false);
                            };
                            executor.execute(runnable);
                        }
                    }
                }

                // 批量更新主机信息
                if (updateList.size() > 0) {
                    systemInfoService.updateRecord(updateList);
                }

                // 批量保存日志信息
                if (logInfoList.size() > 0) {
                    logInfoService.saveRecord(logInfoList);
                }
            }
        } catch (Exception e) {
            // 捕获并记录检测主机是否下线错误信息
            logger.error("检测主机是否下线错误", e);
        }

        try {
            // 查询所有进程信息
            Map<String, Object> params = new HashMap<>();
            List<AppInfo> list = appInfoService.selectAllByParams(params);

            // 如果查询结果不为空
            if (!CollectionUtil.isEmpty(list)) {
                // 初始化更新列表和日志信息列表
                List<AppInfo> updateList = new ArrayList<>();
                List<LogInfo> logInfoList = new ArrayList<>();

                // 遍历所有进程信息
                for (AppInfo appInfo : list) {
                    // 计算当前时间与进程信息创建时间的时间差
                    Date createTime = appInfo.getCreateTime();
                    long diff = date.getTime() - createTime.getTime();

                    // 如果时间差超过延迟时间
                    if (diff > delayTime) {
                        // 如果该进程已经发送过下线警报，则跳过
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(appInfo.getId()))) {
                            continue;
                        }

                        // 更新进程状态为已下线
                        appInfo.setState(StaticKeys.DOWN_STATE);

                        // 添加下线日志信息
                        LogInfo logInfo = new LogInfo();
                        logInfo.setHostname("进程下线IP：" + appInfo.getHostname() + "，名称：" + appInfo.getAppName());
                        logInfo.setInfoContent("超过10分钟未上报状态，可能已下线IP：" + appInfo.getHostname() + "，名称：" + appInfo.getAppName() + "，进程ID：" + appInfo.getAppPid());
                        logInfo.setState(StaticKeys.LOG_ERROR);
                        logInfoList.add(logInfo);

                        // 添加到更新列表中
                        updateList.add(appInfo);

                        // 异步发送进程下线警报邮件
                        Runnable runnable = () -> {
                            WarnMailUtil.sendAppDown(appInfo, true);
                        };
                        executor.execute(runnable);
                    } else {
                        // 如果该进程已经发送过下线警报，则异步发送恢复上线警报邮件
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(appInfo.getId()))) {
                            Runnable runnable = () -> {
                                WarnMailUtil.sendAppDown(appInfo, false);
                            };
                            executor.execute(runnable);
                        }
                    }
                }
                // 批量更新进程信息
                if (updateList.size() > 0) {
                    appInfoService.updateRecord(updateList);
                }
                // 批量保存日志信息
                if (logInfoList.size() > 0) {
                    logInfoService.saveRecord(logInfoList);
                }
            }
        } catch (Exception e) {
            // 捕获并记录检测进程是否下线错误信息
            logger.error("检测进程是否下线错误", e);
        }


    }


    /**
     * 90秒后执行，之后每隔10分钟执行, 单位：ms。
     * 检测心跳
     */
    @Scheduled(initialDelay = 90000L, fixedRateString = "${base.heathTimes}")
    public void heathMonitorTask() {
        // 记录日志，表示心跳检测任务开始执行
        logger.info("heathMonitorTask------------" + DateUtil.getDateTimeString(new Date()));

        // 初始化参数集合
        Map<String, Object> params = new HashMap<>();
        // 初始化心跳监测信息列表和日志信息列表
        List<HeathMonitor> heathMonitors = new ArrayList<>();
        List<LogInfo> logInfoList = new ArrayList<>();

        // 获取当前时间
        Date date = DateUtil.getNowTime();
        try {
            // 查询所有心跳监测信息
            List<HeathMonitor> heathMonitorAllList = heathMonitorService.selectAllByParams(params);

            // 如果查询结果不为空
            if (heathMonitorAllList.size() > 0) {
                // 遍历所有心跳监测信息
                for (HeathMonitor h : heathMonitorAllList) {
                    // 初始化状态码为500
                    int status = 500;
                    // 发送HTTP请求，获取服务接口的状态码
                    status = restUtil.get(h.getHeathUrl());

                    // 更新心跳监测信息的创建时间和状态码
                    h.setCreateTime(date);
                    h.setHeathStatus(status + "");
                    heathMonitors.add(h);

                    // 如果服务接口状态码不为200（即异常）
                    if (!"200".equals(h.getHeathStatus())) {
                        // 如果该心跳监测信息已经发送过异常警报，则跳过
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(h.getId()))) {
                            continue;
                        }

                        // 添加异常日志信息
                        LogInfo logInfo = new LogInfo();
                        logInfo.setHostname("服务接口检测异常：" + h.getAppName());
                        logInfo.setInfoContent("服务接口检测异常：" + h.getAppName() + "，" + h.getHeathUrl() + "，返回状态" + h.getHeathStatus());
                        logInfo.setState(StaticKeys.LOG_ERROR);
                        logInfoList.add(logInfo);

                        // 异步发送异常警报邮件
                        Runnable runnable = () -> {
                            WarnMailUtil.sendHeathInfo(h, true);
                        };
                        executor.execute(runnable);
                    } else {
                        // 如果该心跳监测信息已经发送过异常警报，则异步发送恢复正常警报邮件
                        if (!StringUtils.isEmpty(WarnPools.MEM_WARN_MAP.get(h.getId()))) {
                            Runnable runnable = () -> {
                                WarnMailUtil.sendHeathInfo(h, false);
                            };
                            executor.execute(runnable);
                        }
                    }
                }

                // 批量更新心跳监测信息
                heathMonitorService.updateRecord(heathMonitors);

                // 批量保存日志信息
                if (logInfoList.size() > 0) {
                    logInfoService.saveRecord(logInfoList);
                }
            }
        } catch (Exception e) {
            // 捕获并记录服务接口检测任务错误信息
            logger.error("服务接口检测任务错误", e);
            // 保存错误日志信息
            logInfoService.save("服务接口检测错误", e.toString(), StaticKeys.LOG_ERROR);
        }
    }


    /**
     * 60秒后执行，之后每隔120分钟执行, 单位：ms。
     * 数据表监控
     */
    @Scheduled(initialDelay = 60000L, fixedRateString = "${base.dbTableTimes}")
    public void tableCountTask() {
        // 初始化参数集合和相关列表
        Map<String, Object> params = new HashMap<>();
        List<DbTable> dbTablesUpdate = new ArrayList<>();
        List<DbTableCount> dbTableCounts = new ArrayList<>();
        // 获取当前时间
        Date date = DateUtil.getNowTime();
        String sql = "";
        Long tableCount = 0l;
        try {
            // 查询所有数据库信息
            List<DbInfo> dbInfos = dbInfoService.selectAllByParams(params);

            // 遍历所有数据库信息
            for (DbInfo dbInfo : dbInfos) {
                // 设置参数中的数据库信息ID
                params.put("dbInfoId", dbInfo.getId());
                // 查询该数据库中所有数据表信息
                List<DbTable> dbTables = dbTableService.selectAllByParams(params);

                // 遍历该数据库中所有数据表信息
                for (DbTable dbTable : dbTables) {
                    String whereAnd = "";
                    if (!StringUtils.isEmpty(dbTable.getWhereVal())) {
                        whereAnd = " and ";
                    }

                    // 根据数据库类型拼接查询表记录数的SQL语句
                    if ("postgresql".equals(dbInfo.getDbType())) {
                        sql = RDSConnection.query_table_count_pg.replace("{tableName}", dbTable.getTableName()) + whereAnd + dbTable.getWhereVal();
                    } else {
                        sql = RDSConnection.query_table_count.replace("{tableName}", dbTable.getTableName()) + whereAnd + dbTable.getWhereVal();
                    }

                    // 查询表记录数
                    tableCount = connectionUtil.queryTableCount(dbInfo, sql);

                    // 初始化数据表记录数对象
                    DbTableCount dbTableCount = new DbTableCount();
                    dbTableCount.setCreateTime(date);
                    dbTableCount.setDbTableId(dbTable.getId());
                    dbTableCount.setTableCount(tableCount);
                    dbTableCounts.add(dbTableCount);

                    // 更新数据表对象中的记录数信息
                    dbTable.setDateStr(DateUtil.getDateTimeString(date));
                    dbTable.setTableCount(tableCount);
                    dbTablesUpdate.add(dbTable);
                }
            }

            // 如果记录数列表不为空，则批量保存记录数信息和更新数据表信息
            if (dbTableCounts.size() > 0) {
                dbTableCountService.saveRecord(dbTableCounts);
                dbTableService.updateRecord(dbTablesUpdate);
            }
        } catch (Exception e) {
            // 捕获并记录数据表监控任务错误信息
            logger.error("数据表监控任务错误", e);
            // 保存错误日志信息
            logInfoService.save("数据表监控任务错误", e.toString(), StaticKeys.LOG_ERROR);
        }
    }

    /**
     * 30秒后执行，之后每隔1分钟执行, 单位：ms。
     * 批量提交数据
     */
    @Scheduled(initialDelay = 30000L, fixedRate = 1 * 60 * 1000)
    public synchronized void commitTask() {
        // 记录日志，表示批量提交监控数据任务开始执行
        logger.info("批量提交监控数据任务开始----------" + DateUtil.getCurrentDateTime());
        try {
            // 批量提交应用状态数据
            if (BatchData.APP_STATE_LIST.size() > 0) {
                List<AppState> APP_STATE_LIST = new ArrayList<AppState>();
                APP_STATE_LIST.addAll(BatchData.APP_STATE_LIST);
                BatchData.APP_STATE_LIST.clear();
                appStateService.saveRecord(APP_STATE_LIST);
            }

            // 批量提交CPU状态数据
            if (BatchData.CPU_STATE_LIST.size() > 0) {
                List<CpuState> CPU_STATE_LIST = new ArrayList<CpuState>();
                CPU_STATE_LIST.addAll(BatchData.CPU_STATE_LIST);
                BatchData.CPU_STATE_LIST.clear();
                cpuStateService.saveRecord(CPU_STATE_LIST);
            }

            // 批量提交内存状态数据
            if (BatchData.MEM_STATE_LIST.size() > 0) {
                List<MemState> MEM_STATE_LIST = new ArrayList<MemState>();
                MEM_STATE_LIST.addAll(BatchData.MEM_STATE_LIST);
                BatchData.MEM_STATE_LIST.clear();
                memStateService.saveRecord(MEM_STATE_LIST);
            }

            // 批量提交网络IO状态数据
            if (BatchData.NETIO_STATE_LIST.size() > 0) {
                List<NetIoState> NETIO_STATE_LIST = new ArrayList<NetIoState>();
                NETIO_STATE_LIST.addAll(BatchData.NETIO_STATE_LIST);
                BatchData.NETIO_STATE_LIST.clear();
                netIoStateService.saveRecord(NETIO_STATE_LIST);
            }

            // 批量提交系统负载状态数据
            if (BatchData.SYSLOAD_STATE_LIST.size() > 0) {
                List<SysLoadState> SYSLOAD_STATE_LIST = new ArrayList<SysLoadState>();
                SYSLOAD_STATE_LIST.addAll(BatchData.SYSLOAD_STATE_LIST);
                BatchData.SYSLOAD_STATE_LIST.clear();
                sysLoadStateService.saveRecord(SYSLOAD_STATE_LIST);
            }

            // 批量提交日志信息数据
            if (BatchData.LOG_INFO_LIST.size() > 0) {
                List<LogInfo> LOG_INFO_LIST = new ArrayList<LogInfo>();
                LOG_INFO_LIST.addAll(BatchData.LOG_INFO_LIST);
                BatchData.LOG_INFO_LIST.clear();
                logInfoService.saveRecord(LOG_INFO_LIST);
            }

            // 批量提交桌面状态数据
            if (BatchData.DESK_STATE_LIST.size() > 0) {
                Map<String, Object> paramsDel = new HashMap<String, Object>();

                List<DeskState> DESK_STATE_LIST = new ArrayList<DeskState>();
                DESK_STATE_LIST.addAll(BatchData.DESK_STATE_LIST);
                BatchData.DESK_STATE_LIST.clear();
                List<String> hostnameList = new ArrayList<String>();
                for (DeskState deskState : DESK_STATE_LIST) {
                    if (!hostnameList.contains(deskState.getHostname())) {
                        hostnameList.add(deskState.getHostname());
                    }
                }
                for (String hostname : hostnameList) {
                    paramsDel.put("hostname", hostname);
                    deskStateService.deleteByAccHname(paramsDel);
                }
                deskStateService.saveRecord(DESK_STATE_LIST);
            }

            // 批量提交系统信息数据
            if (BatchData.SYSTEM_INFO_LIST.size() > 0) {
                Map<String, Object> paramsDel = new HashMap<String, Object>();
                List<SystemInfo> SYSTEM_INFO_LIST = new ArrayList<SystemInfo>();
                SYSTEM_INFO_LIST.addAll(BatchData.SYSTEM_INFO_LIST);
                BatchData.SYSTEM_INFO_LIST.clear();
                List<SystemInfo> updateList = new ArrayList<SystemInfo>();
                List<SystemInfo> insertList = new ArrayList<SystemInfo>();
                List<SystemInfo> savedList = systemInfoService.selectAllByParams(paramsDel);
                for (SystemInfo systemInfo : SYSTEM_INFO_LIST) {
                    boolean issaved = false;
                    for (SystemInfo systemInfoS : savedList) {
                        if (systemInfoS.getHostname().equals(systemInfo.getHostname())) {
                            systemInfo.setId(systemInfoS.getId());
                            updateList.add(systemInfo);
                            issaved = true;
                            break;
                        }
                    }
                    if (!issaved) {
                        insertList.add(systemInfo);
                    }
                }
                systemInfoService.updateRecord(updateList);
                systemInfoService.saveRecord(insertList);
            }

            // 批量提交应用信息数据
            if (BatchData.APP_INFO_LIST.size() > 0) {
                Map<String, Object> paramsDel = new HashMap<String, Object>();
                List<AppInfo> APP_INFO_LIST = new ArrayList<AppInfo>();
                APP_INFO_LIST.addAll(BatchData.APP_INFO_LIST);
                BatchData.APP_INFO_LIST.clear();

                List<AppInfo> updateList = new ArrayList<AppInfo>();
                List<AppInfo> insertList = new ArrayList<AppInfo>();
                List<AppInfo> savedList = appInfoService.selectAllByParams(paramsDel);
                for (AppInfo systemInfo : APP_INFO_LIST) {
                    boolean issaved = false;
                    for (AppInfo systemInfoS : savedList) {
                        if (systemInfoS.getHostname().equals(systemInfo.getHostname()) && systemInfoS.getAppPid().equals(systemInfo.getAppPid())) {
                            systemInfo.setId(systemInfoS.getId());
                            updateList.add(systemInfo);
                            issaved = true;
                            break;
                        }
                    }
                    if (!issaved) {
                        insertList.add(systemInfo);
                    }
                }
                appInfoService.updateRecord(updateList);
                appInfoService.saveRecord(insertList);
            }
        } catch (Exception e) {
            //捕获并记录批量提交监控数据错误信息
            logger.error("批量提交监控数据错误----------", e);
            // 保存错误日志信息
            logInfoService.save("commitTask", "批量提交监控数据错误：" + e.toString(), StaticKeys.LOG_ERROR);
        }
        // 记录日志，表示批量提交监控数据任务结束执行
        logger.info("批量提交监控数据任务结束----------" + DateUtil.getCurrentDateTime());
    }

    @Autowired
    SystemInfoMapper systemInfoMapper;
    @Autowired
    CpuStateMapper cpuStateMapper;
    @Autowired
    DeskStateMapper deskStateMapper;
    @Autowired
    MemStateMapper memStateMapper;
    @Autowired
    NetIoStateMapper netIoStateMapper;
    @Autowired
    SysLoadStateMapper sysLoadStateMapper;
    @Autowired
    TcpStateMapper tcpStateMapper;
    @Autowired
    AppInfoMapper appInfoMapper;
    @Autowired
    AppStateMapper appStateMapper;
    @Autowired
    MailSetMapper mailSetMapper;
    @Autowired
    IntrusionInfoMapper intrusionInfoMapper;
    @Autowired
    LogInfoMapper logInfoMapper;

    /**
     * 每天凌晨1:10执行
     * 删除历史数据，15天
     */
    @Scheduled(cron = "0 10 1 * * ?")
    public void clearHisdataTask() {
        // 记录日志，表示定时清空历史数据任务开始执行
        logger.info("定时清空历史数据任务开始----------" + DateUtil.getCurrentDateTime());

        // 清空发告警邮件的记录
        WarnPools.clearOldData();//清空发告警邮件的记录

        // 获取当前时间
        String nowTime = DateUtil.getCurrentDateTime();
        //15天前时间
        String thrityDayBefore = DateUtil.getDateBefore(nowTime, 15);


        // 初始化删除条件参数集合
        Map<String, Object> paramsDel = new HashMap<>();
        try {
            // 设置删除条件：结束时间为15天前
            paramsDel.put(StaticKeys.SEARCH_END_TIME, thrityDayBefore);
            //执行删除操作begin
            if (paramsDel.get(StaticKeys.SEARCH_END_TIME) != null && !"".equals(paramsDel.get(StaticKeys.SEARCH_END_TIME))) {
                cpuStateMapper.deleteByAccountAndDate(paramsDel);//删除cpu监控信息
                deskStateMapper.deleteByAccountAndDate(paramsDel);//删除磁盘监控信息
                memStateMapper.deleteByAccountAndDate(paramsDel);//删除内存监控信息
                netIoStateMapper.deleteByAccountAndDate(paramsDel);//删除吞吐率监控信息
                sysLoadStateMapper.deleteByAccountAndDate(paramsDel);//删除负载状态监控信息
                tcpStateMapper.deleteByAccountAndDate(paramsDel);//删除tcp监控信息
                appStateMapper.deleteByDate(paramsDel);
                //删除15天前的日志信息
                logInfoMapper.deleteByDate(paramsDel);
                //删除15天前数据库表统计信息
                dbTableCountService.deleteByDate(paramsDel);

                // 记录日志，表示定时清空历史数据任务完成
                logInfoService.save("定时清空历史数据完成", "定时清空历史数据完成：", StaticKeys.LOG_ERROR);
            }
            //执行删除操作end

        } catch (Exception e) {
            // 捕获并记录定时清空历史数据任务错误信息
            logger.error("定时清空历史数据任务出错：", e);
            // 保存错误日志信息
            logInfoService.save("定时清空历史数据错误", "定时清空历史数据错误：" + e.toString(), StaticKeys.LOG_ERROR);
        }
        // 记录日志，表示定时清空历史数据任务结束执行
        logger.info("定时清空历史数据任务结束----------" + DateUtil.getCurrentDateTime());
    }


}
