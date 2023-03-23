package org.springframework.debug.xsd;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CustomLabelTest {

	public static void main(String[] args) {

		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("custom-label.xml");
		User user = (User)applicationContext.getBean("fyncUser");
		System.out.println(user.toString());
	}

}
