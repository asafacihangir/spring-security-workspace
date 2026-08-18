package org.phoenix.springsecurity.service;

import org.phoenix.springsecurity.domain.User;


public interface UserContext {


	User getCurrentUser();


	void setCurrentUser(User user);
}
