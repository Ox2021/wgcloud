package com.wgcloud;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wgcloud.entity.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 
 * @ClassName:ScheduledTask.java
 * @author: wgcloud
 * @date: 2019年11月16日
 * @Description: ScheduledTask.java
 * @Copyright: 2017-2024 www.wgstart.com. All rights reserved.
 */
@Component
public class ScheduledTask {

    private Logger logger = LoggerFactory.getLogger(ScheduledTask.class);
    public static List<AppInfo> appInfoList = Collections.synchronizedList(new ArrayList<AppInfo>());
    @Autowired
    private RestUtil restUtil;
    @Autowired
    private CommonConfig commonConfig;

    private SystemInfo systemInfo = null;


    /**
     * 线程池
     */
    static ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 2, TimeUnit.MINUTES, new LinkedBlockingDeque<>());

    /**
     * 60秒后执行，每隔120秒执行, 单位：ms。
     * 定时任务方法，用于定期获取Agent端系统信息和进程信息，并上报给服务器。
     * initialDelay属性表示首次执行任务的延迟时间，本例中延迟59秒后执行。
     * fixedRate属性表示任务的执行间隔时间，本例中每隔120秒（2分钟）执行一次。
     * 单位均为毫秒（ms）。
     */
    @Scheduled(initialDelay = 59 * 1000L, fixedRate = 120 * 1000)
    public void minTask() {
        // 创建一个用于备份监控进程列表的副本
        List<AppInfo> APP_INFO_LIST_CP = new ArrayList<>();
        APP_INFO_LIST_CP.addAll(appInfoList);
        // 创建一个JSON对象，用于构建监控数据
        JSONObject jsonObject = new JSONObject();
        // 创建一个日志信息实例
        LogInfo logInfo = new LogInfo();
        // 获取当前时间戳
        Timestamp t = FormatUtil.getNowTime();
        // 设置日志信息的主机名和创建时间
        logInfo.setHostname(commonConfig.getBindIp() + "：Agent错误");
        logInfo.setCreateTime(t);
        try {
            // 使用Oshi库获取系统信息
            oshi.SystemInfo si = new oshi.SystemInfo();

            HardwareAbstractionLayer hal = si.getHardware();
            OperatingSystem os = si.getOperatingSystem();

            // 操作系统信息
            // 获取系统信息、CPU信息、内存信息、网络信息、系统负载信息
            systemInfo = OshiUtil.os(hal.getProcessor(), os);
            systemInfo.setCreateTime(t);
            // 文件系统信息
            List<DeskState> deskStateList = OshiUtil.file(t, os.getFileSystem());
            // cpu信息
            CpuState cpuState = OshiUtil.cpu(hal.getProcessor());
            cpuState.setCreateTime(t);
            // 内存信息
            MemState memState = OshiUtil.memory(hal.getMemory());
            memState.setCreateTime(t);
            // 网络流量信息
            NetIoState netIoState = OshiUtil.net(hal);
            netIoState.setCreateTime(t);
            // 系统负载信息
            SysLoadState sysLoadState = OshiUtil.getLoadState(systemInfo, hal.getProcessor());
            if (sysLoadState != null) {
                sysLoadState.setCreateTime(t);
            }

            // 将获取到的各项信息存储到JSON对象中
            if (cpuState != null) {
                jsonObject.put("cpuState", cpuState);
            }
            if (memState != null) {
                jsonObject.put("memState", memState);
            }
            if (netIoState != null) {
                jsonObject.put("netIoState", netIoState);
            }
            if (sysLoadState != null) {
                jsonObject.put("sysLoadState", sysLoadState);
            }
            if (systemInfo != null) {
                // 设置系统信息中的一些额外属性
                if (memState != null) {
                    systemInfo.setVersionDetail(systemInfo.getVersion() + "，总内存：" + oshi.util.FormatUtil.formatBytes(hal.getMemory().getTotal()));
                    systemInfo.setMemPer(memState.getUsePer());
                } else {
                    systemInfo.setMemPer(0d);
                }
                if (cpuState != null) {
                    systemInfo.setCpuPer(cpuState.getSys());
                } else {
                    systemInfo.setCpuPer(0d);
                }
                jsonObject.put("systemInfo", systemInfo);
            }
            if (deskStateList != null) {
                jsonObject.put("deskStateList", deskStateList);
            }
            //进程信息
            // 获取并处理监控进程信息
            if (APP_INFO_LIST_CP.size() > 0) {
                List<AppInfo> appInfoResList = new ArrayList<>();
                List<AppState> appStateResList = new ArrayList<>();
                for (AppInfo appInfo : APP_INFO_LIST_CP) {
                    appInfo.setHostname(commonConfig.getBindIp());
                    appInfo.setCreateTime(t);
                    appInfo.setState("1");
                    String pid = FormatUtil.getPidByFile(appInfo);
                    if (StringUtils.isEmpty(pid)) {
                        continue;
                    }
                    AppState appState = OshiUtil.getLoadPid(pid, os, hal.getMemory());
                    if (appState != null) {
                        appState.setCreateTime(t);
                        appState.setAppInfoId(appInfo.getId());
                        appInfo.setMemPer(appState.getMemPer());
                        appInfo.setCpuPer(appState.getCpuPer());
                        appInfoResList.add(appInfo);
                        appStateResList.add(appState);
                    }
                }
                jsonObject.put("appInfoList", appInfoResList);
                jsonObject.put("appStateList", appStateResList);
            }
            // 输出JSON字符串到日志
            logger.debug("---------------" + jsonObject.toString());
        } catch (Exception e) {
            // 捕获异常并记录到日志信息中
            e.printStackTrace();
            logInfo.setInfoContent(e.toString());
        } finally {
            // 如果日志信息不为空，则将其加入JSON对象中
            if (!StringUtils.isEmpty(logInfo.getInfoContent())) {
                jsonObject.put("logInfo", logInfo);
            }
            // 发送POST请求将JSON对象上报给服务器
            restUtil.post(commonConfig.getServerUrl() + "/wgcloud/agent/minTask", jsonObject);
        }

    }


    /**
     * 30秒后执行，每隔5分钟执行, 单位：ms。
     * 获取监控进程
     * 通过定时任务，每隔5分钟执行一次，获取监控进程信息并上报给服务器。
     * initialDelay属性表示首次执行任务的延迟时间，本例中延迟28秒后执行。
     * fixedRate属性表示任务的执行间隔时间，本例中每隔5分钟（300秒）执行一次。
     * 单位均为毫秒（ms）。
     */
    @Scheduled(initialDelay = 28 * 1000L, fixedRate = 300 * 1000)
    public void appInfoListTask() {
        // 创建一个JSON对象，用于记录日志信息
        JSONObject jsonObject = new JSONObject();
        // 创建一个日志信息实例
        LogInfo logInfo = new LogInfo();
        // 获取当前时间戳
        Timestamp t = FormatUtil.getNowTime();
        // 设置日志信息的主机名和创建时间
        logInfo.setHostname(commonConfig.getBindIp() + "：Agent获取进程列表错误");
        logInfo.setCreateTime(t);
        try {
            // 构建请求参数JSON对象
            JSONObject paramsJson = new JSONObject();
            paramsJson.put("hostname", commonConfig.getBindIp());
            // 发送POST请求获取监控进程列表
            String resultJson = restUtil.post(commonConfig.getServerUrl() + "/wgcloud/appInfo/agentList", paramsJson);
            if (resultJson != null) {
                // 解析服务器返回的JSON数组，并转换为AppInfo对象列表
                JSONArray resultArray = JSONUtil.parseArray(resultJson);
                // 清空当前的监控进程列表
                appInfoList.clear();
                if (resultArray.size() > 0) {
                    // 如果服务器返回的监控进程列表不为空，则将其转换为AppInfo对象列表并存储到appInfoList中
                    appInfoList = JSONUtil.toList(resultArray, AppInfo.class);
                }
            }
        } catch (Exception e) {
            // 捕获异常并记录到日志信息中
            e.printStackTrace();
            logInfo.setInfoContent(e.toString());
        } finally {
            // 在finally块中进行清理工作，确保日志信息不为空时将其上报给服务器
            if (!StringUtils.isEmpty(logInfo.getInfoContent())) {
                jsonObject.put("logInfo", logInfo);
            }
            // 发送POST请求将日志信息上报给服务器
            restUtil.post(commonConfig.getServerUrl() + "/wgcloud/agent/minTask", jsonObject);
        }
    }


}
