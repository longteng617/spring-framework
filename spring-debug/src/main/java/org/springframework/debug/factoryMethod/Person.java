package org.springframework.debug.factoryMethod;

public class Person {

	private Integer id;
	private String userName;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Override
	public String toString() {
		return "Person{" +
				"id=" + id +
				", userName='" + userName + '\'' +
				'}';
	}
}
