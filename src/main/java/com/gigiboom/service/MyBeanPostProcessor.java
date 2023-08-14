package com.gigiboom.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Component // 将其也设置为一个bean
// Spring发现这个bean实现了BeanPostProcessor接口，会做特殊处理
public class MyBeanPostProcessor implements BeanPostProcessor {
    // 创建所有的bean都会走这个流程
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) { // 初始化前
        System.out.println("初始化前");
        // 如果想对特定的bean做操作
        if (beanName.equals("userService")) {
            ((UserServiceImpl)bean).setName("gigiboom,帅");
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) { // 初始化后
        System.out.println("初始化后");
        // 匹配切面
        if (beanName.equals("userService")) {

            // 生成代理对象
            Object proxyInstance = Proxy.newProxyInstance(MyBeanPostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    // 先执行代理逻辑
                    System.out.println("代理逻辑"); // 找切点
                    // 执行被代理的bean的原始方法
                    return method.invoke(bean, args);
                }
            });
            return proxyInstance;
        }
        return bean;
    }
}
