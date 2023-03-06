package org.springframework.debug.factoryMethod;

public class PersonStaticFactory {


	public static Person getPerson(String userName){
		Person person = new Person();
		person.setId(1);
		person.setUserName(userName);
		return person;
	}
}
