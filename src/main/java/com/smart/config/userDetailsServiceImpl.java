package com.smart.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.smart.dao.UserRepository;
import com.smart.entities.User;

public class userDetailsServiceImpl implements UserDetailsService {
	@Autowired
	private UserRepository UserRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		//Fetching user from database
		
		User user = UserRepository.getUserByUserNamUser(username);
		
		if (user == null) {
			
			throw new UsernameNotFoundException("Could nt fnd User!! ding ding");
			
		}
		
		CustomUserDetails customUserDetails = new CustomUserDetails(user);
		
		
		return customUserDetails;  
	}

}
