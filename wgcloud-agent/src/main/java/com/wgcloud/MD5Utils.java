package com.wgcloud;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 
 * @ClassName:MD5Utils.java

 * @Description: Md5加密处理
 * @Copyright: 2017-2024 www.wgstart.com. All rights reserved.
 */
@SuppressWarnings("unused")
public class MD5Utils {
    private static final Logger logger = LoggerFactory.getLogger(MD5Utils.class);

    // 全局数组
    private final static String[] strDigits = {"0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    /**
     * 16进制字符
     */
    private final static char hexdigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    /**
     * 对文件全文生成MD5摘要
     *
     * @param filePath 要加密的文件路径
     * @return 返回生成的MD5摘要码，如果文件不存在或者生成MD5摘要失败，则返回空字符串
     */
    public static String getMD5ForFile(String filePath) {
        // 文件输入流
        FileInputStream fis = null;

        // MD5摘要算法实例
        MessageDigest md = null;
        try {

            // 获取MD5算法实例
            md = MessageDigest.getInstance("MD5");

            // 根据文件路径创建File对象
            File file = new File(filePath);
            if (!file.exists()) { // 判断文件是否存在
                return ""; // 如果文件不存在，则返回空字符串
            }

            // 创建文件输入流
            fis = new FileInputStream(file);

            // 创建缓冲区
            byte[] buffer = new byte[4096];

            // 用于记录每次读取的字节数
            int length = -1;

            // 循环读取文件内容，并更新MD5摘要
            while ((length = fis.read(buffer)) != -1) {

                // 更新MD5摘要
                md.update(buffer, 0, length);
            }

            // 获取最终的MD5摘要结果
            byte[] b = md.digest();

            // 将字节数组转换为十六进制字符串形式的MD5摘要码并返回
            return byteToHexString(b);
        } catch (Exception ex) {

            // 捕获异常并记录错误日志
            logger.error("获取MD5信息发生异常！" + ex.toString());
            // 返回空字符串表示生成MD5摘要失败
            return null;
        } finally {
            try {

                // 关闭文件输入流
                if (null != fis) {
                    fis.close();
                }
            } catch (IOException e) {

                // 捕获异常并记录错误日志
                logger.error("获取MD5信息发生异常！" + e.toString());
            }
        }
    }

    /**
     * 把byte[]数组转换成十六进制字符串表示形式
     *
     * @param tmp 要转换的byte[]数组
     * @return 返回十六进制字符串表示形式
     */
    private static String byteToHexString(byte[] tmp) {

        // 存储转换后的十六进制字符串
        String s;

        // 创建字符数组，用于存储十六进制字符
        char str[] = new char[16 * 2];

        // 记录当前处理的字符索引
        int k = 0;

        // 循环处理字节数组中的每个字节
        for (int i = 0; i < 16; i++) {
            // 获取当前字节的值
            byte byte0 = tmp[i];

            // 将当前字节转换为两个十六进制字符，并存储到字符数组中
            str[k++] = hexdigits[byte0 >>> 4 & 0xf];
            str[k++] = hexdigits[byte0 & 0xf];
        }

        // 使用字符数组创建字符串
        s = new String(str);

        // 返回转换后的十六进制字符串
        return s;
    }

    /**
     * 将单个字节转换为数字跟字符串形式表示
     *
     * @param bByte 要转换的单个字节
     * @return 返回数字跟字符串形式表示的结果
     */
    private static String byteToArrayString(byte bByte) {
        // 将字节转换为整数值
        int iRet = bByte;
        // 如果整数值为负数，则加上256，使其变为正数
        if (iRet < 0) {
            iRet += 256;
        }

        // 将整数值分别除以16得到商和余数
        int iD1 = iRet / 16;
        int iD2 = iRet % 16;

        // 返回数字跟字符串形式表示的结果，即对应的十六进制表示形式
        return strDigits[iD1] + strDigits[iD2];
    }

    /**
     * 将单个字节转换为数字形式表示
     *
     * @param bByte 要转换的单个字节
     * @return 返回数字形式表示的结果
     */
    private static String byteToNum(byte bByte) {
        // 将字节转换为整数值
        int iRet = bByte;

        // 打印整数值（用于调试）
        System.out.println("iRet1=" + iRet);

        // 如果整数值为负数，则加上256，使其变为正数
        if (iRet < 0) {
            iRet += 256;
        }

        // 将整数值转换为字符串并返回
        return String.valueOf(iRet);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bByte 要转换的字节数组
     * @return 返回十六进制字符串表示形式
     */
    private static String byteToString(byte[] bByte) {
        // 创建StringBuffer对象，用于存储转换后的十六进制字符串
        StringBuffer sBuffer = new StringBuffer();

        // 遍历字节数组中的每个字节
        for (int i = 0; i < bByte.length; i++) {

            // 将每个字节转换为十六进制字符串并追加到StringBuffer中
            sBuffer.append(byteToArrayString(bByte[i]));
        }

        // 返回转换后的十六进制字符串
        return sBuffer.toString();
    }

    /**
     * 获取字符串的MD5哈希码
     *
     * @param strObj 要计算哈希码的字符串
     * @return 返回计算得到的MD5哈希码，如果输入字符串为空或计算失败，则返回空字符串
     */
    public static String GetMD5Code(String strObj) {

        // 如果输入字符串为空，则直接返回空字符串
        if (StringUtils.isEmpty(strObj)) {
            return "";
        }

        // 存储计算结果的字符串
        String resultString = null;
        try {

            // 将输入字符串转换为新的字符串对象
            resultString = new String(strObj);

            // 获取MD5消息摘要实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 调用md.digest()方法计算字符串的哈希值，并将结果转换为十六进制字符串
            resultString = byteToString(md.digest(strObj.getBytes()));
        } catch (NoSuchAlgorithmException ex) { // 捕获算法不存在异常

            // 输出异常信息
            ex.printStackTrace();
        }

        // 返回计算得到的MD5哈希码，如果计算失败则返回空字符串
        return resultString;
    }


}
