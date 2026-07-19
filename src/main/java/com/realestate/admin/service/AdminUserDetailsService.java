package com.realestate.admin.service;

import com.realestate.admin.entity.Admin;
import com.realestate.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    /** Login form accepts either the admin's email or phone number as "username". */
    @Override
    public UserDetails loadUserByUsername(String usernameOrPhone) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(usernameOrPhone)
                .or(() -> adminRepository.findByPhone(usernameOrPhone))
                .orElseThrow(() -> new UsernameNotFoundException("No admin found for: " + usernameOrPhone));
        return new AdminPrincipal(admin);
    }
}
