package org.springframework.debug.annotation.autowired;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author YangQinglong
 * @Date 2022/6/29 6:11 PM
 */
public class AnnotationApplication {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ApplicationConfig.class);
		UserService userService = (UserService) context.getBean("userServiceImpl");
		userService.selectUserById("1");
	}
}
