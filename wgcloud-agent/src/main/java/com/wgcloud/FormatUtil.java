package com.wgcloud;

import cn.hutool.core.io.FileUtil;
import com.wgcloud.entity.AppInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 
 * @ClassName:FormatUtil.java
 * @author: wgcloud
 * @date: 2019年11月16日
 * @Description: FormatUtil.java
 * @Copyright: 2017-2024 www.wgstart.com. All rights reserved.
 */
public class FormatUtil {

    private static Logger logger = LoggerFactory.getLogger(ScheduledTask.class);

    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 格式化双精度浮点数
     *
     * @param str 要格式化的双精度浮点数
     * @param num 保留小数点位数
     * @return 返回经过格式化的双精度浮点数
     */
    public static double formatDouble(double str, int num) {

        // 使用BigDecimal将双精度浮点数进行精确计算
        java.math.BigDecimal b = new java.math.BigDecimal(str);

        // 将结果保留指定位数的小数并进行四舍五入
        double myNum3 = b.setScale(num, java.math.BigDecimal.ROUND_HALF_UP).doubleValue();

        // 返回格式化后的双精度浮点数
        return myNum3;
    }

    /**
     * 删除字符串中的特定字符
     *
     * @param str 要处理的字符串
     * @return 返回删除特定字符后的字符串
     */
    public static String delChar(String str) {
        // 如果输入字符串为空，则直接返回空字符串
        if (StringUtils.isEmpty(str)) {
            return "";
        }

        // 使用replace方法删除字符串中的百分号字符
        str = str.replace("%", "");

        // 返回删除特定字符后的字符串
        return str;
    }

    /**
     * 获取当前时间
     *
     * @return 返回当前日期的Timestamp对象
     */
    public static Timestamp getNowTime() {

        // 创建日期格式化对象
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_PATTERN);

        // 获取当前日期并转换为Timestamp对象
        Timestamp nowTime = Timestamp.valueOf(dateFormat.format(new Date()));

        // 返回当前时间的Timestamp对象
        return nowTime;
    }

    /**
     * 获取指定日期之前的日期
     *
     * @param datetimes 指定的日期
     * @param day       要减去的天数
     * @return 返回计算得到的日期对象
     */
    public static Date getDateBefore(Date datetimes, int day) {
        // 创建Calendar对象
        Calendar now = Calendar.getInstance();

        // 设置指定的日期
        now.setTime(datetimes);

        // 将日期向前推day天
        now.set(Calendar.DATE, now.get(Calendar.DATE) - day);

        // 返回计算得到的日期对象
        return now.getTime();
    }

    /**
     * 将数据单位从兆（M）、千兆（G）、千千兆（T）转换为千兆（G）
     *
     * @param str 要转换的数据字符串，如"100M"、"1G"、"2.5T"等
     * @return 返回转换后的数据量，单位为千兆（G）
     */
    public static double mToG(String str) {
        // 存储转换后的数据量
        double result = 0;

        // 千兆和兆的换算比例
        double mod = 1024;

        // 如果数据单位为兆（M）
        if (str.contains("M")) { // 获取数据量并去除单位
            double f = Double.valueOf(str.replace("M", ""));

            // 将兆转换为千兆
            result = f / mod;
        } else if (str.contains("K")) { // 如果数据单位为千兆（K）
            double f = Double.valueOf(str.replace("K", "")); // 获取数据量并去除单位
            result = (f / mod) / mod; // 将千兆转换为千千兆再转换为千兆
        } else if (str.contains("T")) { // 如果数据单位为千千兆（T）
            double f = Double.valueOf(str.replace("T", "")); // 获取数据量并去除单位
            result = f * 1024; // 将千千兆转换为千兆
        } else if (str.contains("G")) { // 如果数据单位为千兆（G）
            result = Double.valueOf(str.replace("G", "")); // 获取数据量并去除单位
        }

        // 格式化结果并返回，保留两位小数
        return formatDouble(result, 2);
    }

    /**
     * 根据应用信息获取进程ID（PID）
     *
     * @param appInfo 应用信息对象，包含应用类型和应用PID或PID文件路径
     * @return 返回获取到的进程ID（PID），如果获取失败或应用信息为空，则返回空字符串
     */
    public static String getPidByFile(AppInfo appInfo) {

        // 如果应用类型为1，表示应用PID直接可用
        if ("1".equals(appInfo.getAppType())) {

            // 返回应用PID
            return appInfo.getAppPid();
        } else {
            try {

                // 从PID文件中读取PID
                String pid = FileUtil.readString(appInfo.getAppPid(), "UTF-8");

                // 如果PID非空
                if (!StringUtils.isEmpty(pid)) {

                    // 返回去除首尾空白字符后的PID
                    return pid.trim();
                }
            } catch (Exception e) { // 捕获异常
                // 记录错误日志
                logger.error("获取PID文件错误", e);
            }
        }

        // 返回空字符串（获取失败或应用信息为空）
        return "";
    }
}
