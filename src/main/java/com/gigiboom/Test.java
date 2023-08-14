package com.gigiboom;

import com.gigiboom.service.UserService;
import com.gigiboom.service.UserServiceImpl;
import com.spring.MyApplicationContext;

public class Test {
    public static void main(String[] args) {
        MyApplicationContext applicationContext = new MyApplicationContext(AppConfig.class);

        UserService userService = (UserService) applicationContext.getBean("userService");
        userService.test();
    }
}
