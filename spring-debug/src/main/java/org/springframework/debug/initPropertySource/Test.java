package org.springframework.debug.initPropertySource;

public class Test {

	public static void main(String[] args) {
		MyClassPathXmlApplicationContext applicationContext = new MyClassPathXmlApplicationContext("selfEditor-${user}.xml");
	}
}
