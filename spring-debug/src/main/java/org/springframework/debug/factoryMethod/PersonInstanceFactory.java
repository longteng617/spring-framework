package org.springframework.debug.factoryMethod;

public class PersonInstanceFactory {

	public Person getPerson(String userName){
		Person person = new Person();
		person.setId(1);
		person.setUserName(userName);
		return person;
	}
}
