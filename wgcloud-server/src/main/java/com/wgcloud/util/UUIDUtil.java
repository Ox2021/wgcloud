package com.wgcloud.util;

import java.util.Random;
import java.util.UUID;


/**
 
 * @ClassName:UUIDUtil.java

 * @Description: UUIDUtil.java
 * @Copyright: 2017-2022 wgcloud. All rights reserved.
 */
public class UUIDUtil {

    public static String getUUID() {
        return String.valueOf(UUID.randomUUID()).replace("-", "");
    }

    /**
     * 随机6位数字
     *
     * @return
     */
    public static String getRandomSix() {
        return "" + new Random().nextInt(999999);
    }

}
