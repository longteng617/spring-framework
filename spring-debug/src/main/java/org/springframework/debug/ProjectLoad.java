package org.springframework.debug;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.debug.editor.Customer;
import org.springframework.debug.xsd.User;

/**
 * 项目启动测试类
 * @author YangQinglong
 * @since  2022/6/17 10:57 AM
 */
public class ProjectLoad {

	public static void main(String[] args) {
//		ApplicationContext applicationContext = new MyClassPathXmlApplicationContext("selfEditor-${user}.xml");
//		Person person = applicationContext.getBean(Person.class);
//		System.out.println(person);
//
//		User user = (User)applicationContext.getBean("fync");
//		System.out.println(user.toString());

		ApplicationContext ac = new ClassPathXmlApplicationContext("selfEditor-${user}.xml");
//		Customer customer = (Customer)ac.getBean("customer");
//		System.out.println(customer);

	}
}
