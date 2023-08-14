package com.gigiboom.service;

import com.spring.*;

@Component("userService")
@Scope("prototype") // 原型bean
public class UserServiceImpl implements UserService {

    @Autowired // 加了Autowired后，Spring应该已经给orderService赋好值了----自动注入
    private OrderService orderService;

    private String name;

    public void setName(String name) {
        this.name = name;
    }

//    @Override
//    public void afterPropertiesSet() throws Exception {
//        // Spring提供的初始化机制，程序员自定义实现逻辑
//        System.out.println("初始化");
//    }

    public void test() {
        System.out.println(orderService);
        System.out.println(name);
    }
}
