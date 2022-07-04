package org.springframework.debug.annotation.autowired;

import org.springframework.stereotype.Repository;

/**
 * @Author YangQinglong
 * @Date 2022/6/29 6:07 PM
 */
@Repository
public class UserDAO {

	public void selectUserById(String id){
		System.out.println("id:"+id);
	}
}
