
package com.downloader.sdk.dlplugin.util.common;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * @Title ReflectUtils
 * @Description 反射的一些工具
 * @author
 * @date 2013-1-20
 * @version V1.0
 */
public class ReflectUtils {
    /**
     * 检测实体属性是否已经被标注为 不被识别
     * 
     * @param field 字段
     * @return
     */
    public static boolean isTransient(Field field) {
        return false;
        // return field.getAnnotation(Transparent.class) != null;
    }

    /**
     * 是否为基本的数据类型
     * 
     * @param field
     * @return
     */
    public static boolean isBaseDateType(Field field) {
        Class<?> clazz = field.getType();
        return clazz.equals(String.class) || clazz.equals(Integer.class)
                || clazz.equals(Byte.class) || clazz.equals(Long.class)
                || clazz.equals(Double.class) || clazz.equals(Float.class)
                || clazz.equals(Character.class) || clazz.equals(Short.class)
                || clazz.equals(Boolean.class) || clazz.equals(Date.class)
                || clazz.equals(Date.class)
                || clazz.equals(java.sql.Date.class) || clazz.isPrimitive();
    }

}
