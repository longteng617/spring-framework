package org.springframework.debug.populateBean;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestPopulateBean {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("populateBean.xml");
	}
}
