package com.wgcloud.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wgcloud.entity.*;
import com.wgcloud.service.LogInfoService;
import com.wgcloud.service.SystemInfoService;
import com.wgcloud.util.TokenUtils;
import com.wgcloud.util.msg.WarnMailUtil;
import com.wgcloud.util.staticvar.BatchData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/agent")
public class AgentController {


    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 40, 2, TimeUnit.MINUTES, new LinkedBlockingDeque<>());


    @Resource
    private LogInfoService logInfoService;
    @Resource
    private SystemInfoService systemInfoService;
    @Autowired
    private TokenUtils tokenUtils;

    /**
     * 用于处理Agent端上报的监控数据的接口。
     * 该接口通过HTTP请求接收Agent端上报的监控数据，并将其处理后存储到相应的数据结构中。
     * @param paramBean 接收到的JSON格式的监控数据
     * @return 返回一个JSON对象，表示处理结果（成功或失败）
     */
    @ResponseBody
    @RequestMapping("/minTask")
    public JSONObject minTask(@RequestBody String paramBean) {
        // 将接收到的JSON字符串解析为JSONObject对象
        JSONObject agentJsonObject = (JSONObject) JSONUtil.parse(paramBean);
        // 创建一个用于存储处理结果的JSONObject对象
        JSONObject resultJson = new JSONObject();
        // 校验Agent端的令牌是否有效
        if (!tokenUtils.checkAgentToken(agentJsonObject)) {
            logger.error("token is invalidate");
            resultJson.put("result", "error：token is invalidate");
            return resultJson;
        }
        // 分别获取监控数据中的各项信息
        JSONObject cpuState = agentJsonObject.getJSONObject("cpuState");
        JSONObject memState = agentJsonObject.getJSONObject("memState");
        JSONObject sysLoadState = agentJsonObject.getJSONObject("sysLoadState");
        JSONArray appInfoList = agentJsonObject.getJSONArray("appInfoList");
        JSONArray appStateList = agentJsonObject.getJSONArray("appStateList");
        JSONObject logInfo = agentJsonObject.getJSONObject("logInfo");
        JSONObject systemInfo = agentJsonObject.getJSONObject("systemInfo");
        JSONObject netIoState = agentJsonObject.getJSONObject("netIoState");
        JSONArray deskStateList = agentJsonObject.getJSONArray("deskStateList");

        try {
            // 将日志信息存储到日志信息列表中
            if (logInfo != null) {
                LogInfo bean = new LogInfo();
                BeanUtil.copyProperties(logInfo, bean);
                BatchData.LOG_INFO_LIST.add(bean);
            }
            // 将CPU状态信息存储到CPU状态列表中，并发送CPU警报邮件
            if (cpuState != null) {
                CpuState bean = new CpuState();
                BeanUtil.copyProperties(cpuState, bean);
                BatchData.CPU_STATE_LIST.add(bean);
                Runnable runnable = () -> {
                    WarnMailUtil.sendCpuWarnInfo(bean);
                };
                executor.execute(runnable);

            }
            // 将内存状态信息存储到内存状态列表中，并发送警报邮件
            if (memState != null) {
                MemState bean = new MemState();
                BeanUtil.copyProperties(memState, bean);
                BatchData.MEM_STATE_LIST.add(bean);
                Runnable runnable = () -> {
                    WarnMailUtil.sendWarnInfo(bean);
                };
                executor.execute(runnable);
            }
            // 将系统负载状态信息存储到系统负载状态列表中
            if (sysLoadState != null) {
                SysLoadState bean = new SysLoadState();
                BeanUtil.copyProperties(sysLoadState, bean);
                BatchData.SYSLOAD_STATE_LIST.add(bean);
            }
            // 将网络IO状态信息存储到网络IO状态列表中
            if (netIoState != null) {
                NetIoState bean = new NetIoState();
                BeanUtil.copyProperties(netIoState, bean);
                BatchData.NETIO_STATE_LIST.add(bean);
            }
            // 将应用信息和应用状态信息分别存储到相应列表中
            if (appInfoList != null && appStateList != null) {
                List<AppInfo> appInfoResList = JSONUtil.toList(appInfoList, AppInfo.class);
                for (AppInfo appInfo : appInfoResList) {
                    BatchData.APP_INFO_LIST.add(appInfo);
                }
                List<AppState> appStateResList = JSONUtil.toList(appStateList, AppState.class);
                for (AppState appState : appStateResList) {
                    BatchData.APP_STATE_LIST.add(appState);
                }
            }
            // 将系统信息存储到系统信息列表中
            if (systemInfo != null) {
                SystemInfo bean = new SystemInfo();
                BeanUtil.copyProperties(systemInfo, bean);
                BatchData.SYSTEM_INFO_LIST.add(bean);
            }
            // 将桌面状态信息存储到桌面状态列表中
            if (deskStateList != null) {
                for (Object jsonObjects : deskStateList) {
                    DeskState bean = new DeskState();
                    BeanUtil.copyProperties(jsonObjects, bean);
                    BatchData.DESK_STATE_LIST.add(bean);
                }
            }
            // 设置处理结果为成功
            resultJson.put("result", "success");
        } catch (Exception e) {
            // 处理过程中出现异常，设置处理结果为失败，并将异常信息存储到结果JSON中
            e.printStackTrace();
            resultJson.put("result", "error：" + e.toString());
        } finally {
            // 最终返回处理结果JSON对象
            return resultJson;
        }
    }

}
