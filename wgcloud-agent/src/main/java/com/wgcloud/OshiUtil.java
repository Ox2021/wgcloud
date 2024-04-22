package com.wgcloud;

import com.wgcloud.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: Oshi工具类
 */
public class OshiUtil {

    private static Logger logger = LoggerFactory.getLogger(OshiUtil.class);

    private static CommonConfig commonConfig = (CommonConfig) ApplicationContextHelper.getBean(CommonConfig.class);

    private static Runtime r = Runtime.getRuntime();


    /**
     * 获取内存使用信息
     *
     * @param memory 全局内存对象，包含内存相关信息的实例
     * @return 返回内存状态对象，包含内存使用百分比和主机名
     * @throws Exception 当获取内存信息失败时抛出异常
     */
    public static MemState memory(GlobalMemory memory) throws Exception {
        // 创建内存状态对象
        MemState memState = new MemState();
        // 获取总内存大小，并转换为MB单位
        long total = memory.getTotal() / 1024L / 1024L;
        // 获取可用内存大小，并转换为MB单位
        long free = memory.getAvailable() / 1024L / 1024L;
        // 计算内存使用百分比
        double usePer = (double) (total - free) / (double) total;
        // 将内存使用百分比格式化为带有一位小数的字符串，并设置到内存状态对象中
        memState.setUsePer(FormatUtil.formatDouble(usePer * 100, 1));
        // 获取主机名并设置到内存状态对象中
        memState.setHostname(commonConfig.getBindIp());
        // 返回内存状态对象
        return memState;
    }


    /**
     * 获取CPU使用率、等待率和空闲率
     *
     * @param processor CentralProcessor对象，包含了CPU相关信息的实例
     * @return 返回CPU状态对象，包含CPU的系统使用率和主机名
     * @throws Exception 当获取CPU信息失败时抛出异常
     */
    public static CpuState cpu(CentralProcessor processor) throws Exception {
        // 获取系统CPU负载的前一个时间片段
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // 等待一秒钟
        Util.sleep(1000);
        // 创建CPU状态对象
        CpuState cpuState = new CpuState();
        // 计算系统CPU使用率，将结果格式化为带有一位小数的字符串，并设置到CPU状态对象中
        cpuState.setSys(FormatUtil.formatDouble(processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100, 1));
        // 获取主机名并设置到CPU状态对象中
        cpuState.setHostname(commonConfig.getBindIp());
        // 返回CPU状态对象
        return cpuState;
    }

    /**
     * 获取操作系统信息
     *
     * @param processor CentralProcessor对象，包含了CPU相关信息的实例
     * @param os OperatingSystem对象，包含了操作系统相关信息的实例
     * @return 返回操作系统信息对象，包含主机名、CPU核心数、CPU型号、操作系统版本和详细版本信息以及状态
     * @throws Exception 当获取操作系统信息失败时抛出异常
     */
    public static com.wgcloud.entity.SystemInfo os(CentralProcessor processor, OperatingSystem os) throws Exception {

        // 创建操作系统信息对象
        com.wgcloud.entity.SystemInfo systemInfo = new com.wgcloud.entity.SystemInfo();

        // 获取主机名并设置到操作系统信息对象中
        systemInfo.setHostname(commonConfig.getBindIp());

        // 获取CPU逻辑处理器数量，并将其转换为字符串后设置到操作系统信息对象中
        systemInfo.setCpuCoreNum(processor.getLogicalProcessorCount() + "");
        String cpuInfo = processor.toString();

        // 截取CPU信息字符串中第一行，并设置到操作系统信息对象中
        if (cpuInfo.indexOf("\n") > 0) {
            cpuInfo = cpuInfo.substring(0, cpuInfo.indexOf("\n"));
        }
        systemInfo.setCpuXh(cpuInfo);

        // 设置操作系统版本信息到操作系统信息对象中
        systemInfo.setVersion(os.toString());

        // 设置操作系统详细版本信息到操作系统信息对象中
        systemInfo.setVersionDetail(os.toString());

        // 设置状态为正常（状态码为"1"）
        systemInfo.setState("1");

        // 返回操作系统信息对象
        return systemInfo;
    }

    /**
     * 获取磁盘使用信息
     *
     * @param t 时间戳，表示获取磁盘信息的时间点
     * @param fileSystem FileSystem对象，包含了文件系统相关信息的实例
     * @return 返回包含磁盘状态的列表，每个磁盘状态对象包含文件系统名称、主机名、已使用空间、可用空间、总空间、使用百分比和创建时间
     * @throws Exception 当获取磁盘信息失败时抛出异常
     */
    public static List<DeskState> file(Timestamp t, FileSystem fileSystem) throws Exception {
        // 创建存储磁盘状态的列表
        List<DeskState> list = new ArrayList<>();

        // 获取文件系统列表
        List<OSFileStore> fsArray = fileSystem.getFileStores();

        // 遍历文件系统列表
        for (OSFileStore fs : fsArray) {
            // 获取可用空间和总空间
            long usable = fs.getUsableSpace();
            long total = fs.getTotalSpace();

            // 创建磁盘状态对象
            DeskState deskState = new DeskState();

            // 设置文件系统名称到磁盘状态对象中
            deskState.setFileSystem(fs.getName());

            // 获取主机名并设置到磁盘状态对象中
            deskState.setHostname(commonConfig.getBindIp());

            // 计算已使用空间，并将结果转换为GB单位后设置到磁盘状态对象中
            deskState.setUsed(((total - usable) / 1024 / 1024 / 1024) + "G");

            // 计算可用空间，并将结果转换为GB单位后设置到磁盘状态对象中
            deskState.setAvail((usable / 1024 / 1024 / 1024) + "G");

            // 将总空间转换为GB单位后设置到磁盘状态对象中
            deskState.setSize((total / 1024 / 1024 / 1024) + "G");

            // 计算使用百分比，并设置到磁盘状态对象中
            double usedSize = (total - usable);
            double usePercent = 0;
            if (total > 0) {
                usePercent = FormatUtil.formatDouble(usedSize / total * 100D, 2);
            }

            // 设置创建时间到磁盘状态对象中
            deskState.setUsePer(usePercent + "%");
            deskState.setCreateTime(t);

            // 将磁盘状态对象添加到列表中
            list.add(deskState);
        }

        // 返回包含磁盘状态的列表
        return list;
    }

    /**
     * 获取系统负载
     *
     * @param systemInfo 包含操作系统信息的SystemInfo对象
     * @param processor CentralProcessor对象，包含了CPU相关信息的实例
     * @return 返回系统负载状态对象，包含一分钟、五分钟和十五分钟的系统负载值，以及主机名
     * @throws Exception 当获取系统负载信息失败时抛出异常
     */
    public static SysLoadState getLoadState(com.wgcloud.entity.SystemInfo systemInfo, CentralProcessor processor) throws Exception {
        // 创建系统负载状态对象
        SysLoadState sysLoadState = new SysLoadState();

        // 如果系统信息为空，则返回空值
        if (systemInfo == null) {
            return null;
        }

        // 如果操作系统是Windows，则返回空值，因为Windows系统不支持负载指标
        if (systemInfo.getVersionDetail().indexOf("Microsoft") > -1) {
            //windows系统不支持负载指标
            return null;
        }

        // 获取系统负载的一分钟、五分钟和十五分钟的平均值
        double[] loadAverage = processor.getSystemLoadAverage(3);

        // 设置一分钟的系统负载值到系统负载状态对象中
        sysLoadState.setOneLoad(loadAverage[0]);

        // 获取主机名并设置到系统负载状态对象中
        sysLoadState.setHostname(commonConfig.getBindIp());

        // 设置五分钟的系统负载值到系统负载状态对象中
        sysLoadState.setFiveLoad(loadAverage[1]);

        // 设置十五分钟的系统负载值到系统负载状态对象中
        sysLoadState.setFifteenLoad(loadAverage[2]);

        // 返回系统负载状态对象
        return sysLoadState;
    }


    /**
     * 获取进程信息
     *
     * @param pid 进程ID
     * @param os OperatingSystem对象，包含了操作系统相关信息的实例
     * @param memory GlobalMemory对象，包含了全局内存相关信息的实例
     * @return 返回应用状态对象，包含进程的CPU使用百分比和内存使用百分比
     * @throws Exception 当获取进程信息失败时抛出异常
     */
    public static AppState getLoadPid(String pid, OperatingSystem os, GlobalMemory memory) throws Exception {

        try {
            // 创建进程ID列表，用于获取指定进程的信息
            List<Integer> pidList = new ArrayList<>();
            pidList.add(Integer.valueOf(pid));

            // 获取指定进程的信息列表
            List<OSProcess> procs = os.getProcesses(pidList);

            // 遍历进程信息列表，最多处理前5个进程
            for (int i = 0; i < procs.size() && i < 5; i++) {
                OSProcess p = procs.get(i);

                // 创建应用状态对象
                AppState appState = new AppState();

                // 计算进程的CPU使用百分比，并设置到应用状态对象中
                appState.setCpuPer(FormatUtil.formatDouble(100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime(), 2));

                // 计算进程的内存使用百分比，并设置到应用状态对象中
                appState.setMemPer(FormatUtil.formatDouble(100d * p.getResidentSetSize() / memory.getTotal(), 2));

                // 返回应用状态对象
                return appState;
            }

        } catch (Exception e) {
            // 捕获异常并记录错误日志
            logger.error("获取进程信息错误", e);
        }

        // 如果出现异常或者未找到指定进程，则返回空值
        return null;
    }


    /**
     * 获取网络带宽
     *
     * @param hal HardwareAbstractionLayer对象，包含了硬件相关信息的实例
     * @return 返回网络I/O状态对象，包含平均每秒接收字节数、平均每秒发送字节数、平均每秒接收数据包数、平均每秒发送数据包数以及主机名
     * @throws Exception 当获取网络信息失败时抛出异常
     */
    public static NetIoState net(HardwareAbstractionLayer hal) throws Exception {
        // 初始化变量，用于记录开始和结束时的网络信息
        long rxBytesBegin = 0;
        long txBytesBegin = 0;
        long rxPacketsBegin = 0;
        long txPacketsBegin = 0;
        long rxBytesEnd = 0;
        long txBytesEnd = 0;
        long rxPacketsEnd = 0;
        long txPacketsEnd = 0;

        // 获取开始时的网络接口列表及其相关信息
        List<NetworkIF> listBegin = hal.getNetworkIFs();
        for (NetworkIF net : listBegin) {
            rxBytesBegin += net.getBytesRecv();
            txBytesBegin += net.getBytesSent();
            rxPacketsBegin += net.getPacketsRecv();
            txPacketsBegin += net.getPacketsSent();
        }

        // 暂停5秒，等待一段时间以获取结束时的网络接口列表及其相关信息
        Thread.sleep(5000);

        // 获取结束时的网络接口列表及其相关信息
        List<NetworkIF> listEnd = hal.getNetworkIFs();
        for (NetworkIF net : listEnd) {
            rxBytesEnd += net.getBytesRecv();
            txBytesEnd += net.getBytesSent();
            rxPacketsEnd += net.getPacketsRecv();
            txPacketsEnd += net.getPacketsSent();
        }

        // 计算平均每秒接收字节数、平均每秒发送字节数、平均每秒接收数据包数、平均每秒发送数据包数
        long rxBytesAvg = (rxBytesEnd - rxBytesBegin) / 3 / 1024;
        long txBytesAvg = (txBytesEnd - txBytesBegin) / 3 / 1024;
        long rxPacketsAvg = (rxPacketsEnd - rxPacketsBegin) / 3 / 1024;
        long txPacketsAvg = (txPacketsEnd - txPacketsBegin) / 3 / 1024;

        // 创建网络I/O状态对象
        NetIoState netIoState = new NetIoState();

        // 设置平均每秒接收字节数到网络I/O状态对象中
        netIoState.setRxbyt(rxBytesAvg + "");

        // 设置平均每秒发送字节数到网络I/O状态对象中
        netIoState.setTxbyt(txBytesAvg + "");

        // 设置平均每秒接收数据包数到网络I/O状态对象中
        netIoState.setRxpck(rxPacketsAvg + "");

        // 设置平均每秒发送数据包数到网络I/O状态对象中
        netIoState.setTxpck(txPacketsAvg + "");

        // 获取主机名并设置到网络I/O状态对象中
        netIoState.setHostname(commonConfig.getBindIp());

        // 返回网络I/O状态对象
        return netIoState;
    }
}
