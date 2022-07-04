package org.springframework.debug.annotation.autowired;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author YangQinglong
 * @Date 2022/6/29 6:09 PM
 */
@Service
public class UserServiceImpl implements UserService{
	@Autowired
	private UserDAO userDAO;

	@Override
	public void selectUserById(String id) {
		userDAO.selectUserById(id);
	}
}
