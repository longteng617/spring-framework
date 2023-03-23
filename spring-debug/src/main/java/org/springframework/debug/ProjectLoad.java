package org.springframework.debug;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.debug.editor.Customer;
import org.springframework.debug.method.overrides.FruitPlate;

import java.util.Arrays;

/**
 * 项目启动测试类
 * @author YangQinglong
 * @since  2022/6/17 10:57 AM
 */
public class ProjectLoad {

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("selfEditor-${user}.xml");
		Customer customer = applicationContext.getBean(Customer.class);
		System.out.println(customer);


//		ApplicationContext ac = new ClassPathXmlApplicationContext("method-overrides.xml");
//		FruitPlate fruitPlate = (FruitPlate) ac.getBean("fruitPlate");
//		System.out.println(fruitPlate.getFruit());
//
//		FruitPlate fruitPlate2 = (FruitPlate) ac.getBean("fruitPlate");
//		System.out.println(fruitPlate2.getFruit());


//		ApplicationContext ac = new ClassPathXmlApplicationContext("applicationContext.xml");
//		Customer customer = (Customer)ac.getBean("customer");
//		System.out.println(customer);





	}
}
