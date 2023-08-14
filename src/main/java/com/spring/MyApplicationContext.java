package com.spring;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyApplicationContext {

    private Class configClass;

    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>(); // 单例池，存储单例对象
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(); // 存所有bean的定义
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public MyApplicationContext(Class configClass) {
        this.configClass = configClass;

        // 解析配置类：ComponentScan注解-->扫描路径-->扫描-->BeanDefinition-->BeanDefinitionMap
        scan(configClass);

        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);// 单例bean
                singletonObjects.put(beanName, bean);
            }
        }

    }

    public Object createBean(String beanName, BeanDefinition beanDefinition) {

        Class clazz = beanDefinition.getClazz();
        try {
            // 调用无参构造方法反射一个实例对象
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // 依赖注入--对实例对象的属性赋值
            for (Field declaredField : clazz.getDeclaredFields()) {
                // 加了Autowired注解的属性才需要Spring自动赋值
                if (declaredField.isAnnotationPresent(Autowired.class)) {

                    // 通过name得到bean对象
                    Object bean = getBean(declaredField.getName());

                    // 设置一个类的私有成员变量（declaredField）为可访问
                    declaredField.setAccessible(true);

                    // 给instance对象里面的declaredField属性赋值
                    declaredField.set(instance, bean);
                }
            }
            // Aware回调
            // 判断instance对象是否实现BeanNameAware接口
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }


            // 初始化前
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            // 初始化
            if (instance instanceof InitializingBean) {
                try {
                    ((InitializingBean) instance).afterPropertiesSet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // 初始化后
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }

            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void scan(Class configClass) {

        // 获取ComponentScan注解
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);

        // 获取扫描路径
        String path = componentScanAnnotation.value();
        path = path.replace(".", "/");
        System.out.println(path);

        // 扫描
        // 三种类加载器
        // BootStrap-->jre/lib
        // Ext-------->jre/ext/lib
        // App-------->classpath
        ClassLoader classLoader = MyApplicationContext.class.getClassLoader(); // app
        URL resource = classLoader.getResource(path);// 相对路径获取资源
        File file = new File(resource.getFile());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) { // 遍历目录下的文件
                System.out.println(f);

                String fileName = f.getAbsolutePath();
                if (fileName.endsWith(".class")) {  // 处理类文件

                    String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    className = className.replace("/", "."); // 转换类的全路径名
                    System.out.println(className);

                    try {
                        Class<?> clazz = classLoader.loadClass(className); // 加载类
                        if (clazz.isAnnotationPresent(Component.class)) {
                            // 表示当前类是一个Bean

                            // 判断clazz对象是否实现了BeanPostProcessor接口
                            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }

                            // 解析类，判断当前是单例bean，还是prototype的bean-原型bean,
                            // 解析类-->BeanDefinition

                            // 获取bean的名字
                            Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
                            String beanName = componentAnnotation.value();

                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(clazz);

                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            } else {
                                // 单例bean
                                beanDefinition.setScope("singleton");
                            }
                            beanDefinitionMap.put(beanName, beanDefinition);

                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }
    }

    public Object getBean(String beanName) {
        if (beanDefinitionMap.containsKey(beanName)) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")) {
                // 单例对象从单例池中取
                Object o = singletonObjects.get(beanName);
                return o;
            } else {
                // 非单例，创建bean对象返回
                Object bean = createBean(beanName, beanDefinition);
                return bean;
            }
        } else {
            // 对应的bean不存在
            throw new NullPointerException();
        }
    }

}
