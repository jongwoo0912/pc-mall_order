package com.oraclejava.pcproduct.customer;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 'ADMIN' 역할 행이 없으면 시작 시 한 번 추가 (이미 있으면 아무것도 하지 않음) */
@Component
public class RoleInitializer implements ApplicationRunner {

	private final RoleRepository roleRepository;

	public RoleInitializer(RoleRepository roleRepository) {
		this.roleRepository = roleRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (roleRepository.findByName(Role.ADMIN).isEmpty()) {
			roleRepository.save(new Role(Role.ADMIN));
		}
	}
}
