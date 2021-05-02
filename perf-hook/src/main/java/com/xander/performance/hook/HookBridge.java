package com.xander.performance.hook;

import com.xander.performance.hook.core.IHookBridge;
import com.xander.performance.hook.core.MethodHook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import io.github.xanderwang.asu.aLog;
import io.github.xanderwang.asu.aUtil;

/**
 * 提供对外的统一的 hook 方法
 */
public class HookBridge {

  private static final String TAG = "HookBridge";

  static IHookBridge iHookBridge = null;

  static {
    ServiceLoader<IHookBridge> iHookBridgeLoader = ServiceLoader.load(IHookBridge.class);
    Iterator<IHookBridge> iterator = iHookBridgeLoader.iterator();
    if (!iterator.hasNext()) {
      // multi dex 的时候，发现会找不到配置，目前通过设置 classloader 来处理
      iHookBridgeLoader = ServiceLoader.load(IHookBridge.class, IHookBridge.class.getClassLoader());
      iterator = iHookBridgeLoader.iterator();
    }
    aLog.e(TAG, "HookBridge init has instance: %s", iterator.hasNext());
    while (iterator.hasNext()) {
      iHookBridge = iterator.next();
      aLog.e(TAG, "HookBridge init find instance: %s", iHookBridge.getClass());
    }
  }

  public static boolean isEpic() {
    return IHookBridge.TYPE_EPIC == iHookBridge.getHookType();
  }

  public static boolean isSandHook() {
    return IHookBridge.TYPE_SAND_HOOK == iHookBridge.getHookType();
  }

  public static boolean isFastHook() {
    return IHookBridge.TYPE_FAST_HOOK == iHookBridge.getHookType();
  }

  public static void hookAllConstructors(Class<?> clazz, MethodHook callback) {
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    assertNotNullOrEmpty(constructors);
    if (null == constructors) {
      return;
    }
    for (Constructor<?> constructor : constructors) {
      hookMethod(constructor, callback);
    }
  }

  public static void findAllAndHookMethod(Class<?> clazz, String methodName,
      MethodHook methodHookCallback) {
    List<Method> methodList = findMethodList(clazz, methodName, true, null);
    assertNotNullOrEmpty(methodList);
    for (Method method : methodList) {
      hookMethod(method, methodHookCallback);
    }
  }

  public static void findAndHookMethod(Class<?> clazz, String methodName,
      Object... parameterTypesAndCallback) {
    Class<?>[] parameterTypes = null;
    if (null != parameterTypesAndCallback && parameterTypesAndCallback.length > 1) {
      int len = parameterTypesAndCallback.length - 1;
      parameterTypes = new Class<?>[len];
      for (int i = 0; i < len; i++) {
        parameterTypes[i] = (Class<?>) parameterTypesAndCallback[i];
      }
    }

    Object callback = null;
    if (null != parameterTypesAndCallback && parameterTypesAndCallback.length >= 1) {
      callback = parameterTypesAndCallback[parameterTypesAndCallback.length - 1];
    }
    assertNotNullOrEmpty(callback);
    MethodHook methodCallback = (MethodHook) callback;

    List<Method> methodList = findMethodList(clazz, methodName, parameterTypes);
    assertNotNullOrEmpty(methodList);
    for (Method method : methodList) {
      hookMethod(method, methodCallback);
    }

  }

  public static void hookMethod(Member hookMethod, MethodHook callback) {
    assertNotNullOrEmpty(iHookBridge);
    aLog.d(TAG, "hookMethod: %s", aUtil.memberToString(hookMethod));
    try {
      iHookBridge.hookMethod(hookMethod, callback);
    } catch (Exception e) {
      aLog.ee(TAG, "hookMethod", e);
    }
  }

  private static List<Method> findMethodList(Class<?> clazz, String methodName,
      Class<?>... parameterTypes) {
    return findMethodList(clazz, methodName, false, parameterTypes);
  }

  private static List<Method> findMethodList(Class<?> clazz, String methodName,
      boolean justCheckName, Class<?>... parameterTypes) {
    List<Method> list = new ArrayList<>();
    if (!justCheckName && (null == parameterTypes || parameterTypes.length == 0)) {
      justCheckName = true;
    }
    if (justCheckName) {
      // getMethods  public 的方法，包括继承的
      // getDeclaredMethods 自身定义的和实现的接口的方法，无论是
      Method[] methods = clazz.getDeclaredMethods();
      if (null != methods) {
        for (Method method : methods) {
          if (method.getName().equals(methodName)) {
            list.add(method);
          }
        }
      }
    } else {
      try {
        Method m = clazz.getDeclaredMethod(methodName, parameterTypes);
        list.add(m);
      } catch (Exception e) {
        aLog.ee(TAG, "findMethodList", e);
      }
    }
    return list;
  }

  private static void assertNotNullOrEmpty(Object object) {
    if (null == object) {
      aLog.ee(TAG, "assertNotNullOrEmpty", new IllegalArgumentException("null object!!!"));
      // throw new IllegalArgumentException("null object!!!");
    }
    if (object instanceof List) {
      if (((List) object).isEmpty()) {
        aLog.ee(TAG, "assertNotNullOrEmpty", new IllegalArgumentException("empty list!!!"));
        // throw new IllegalArgumentException("empty list!!!");
      }
    }
    if (object instanceof Member[]) {
      if (((Member[]) object).length == 0) {
        aLog.ee(TAG, "assertNotNullOrEmpty", new IllegalArgumentException("empty array!!!"));
        // throw new IllegalArgumentException("empty array!!!");
      }
    }
  }
}
