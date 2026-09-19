package com.lauriel.store.security;

import com.lauriel.store.entity.User;
import com.lauriel.store.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public StoreUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public StoreUserDetails loadUserByUsername(String username) {
        return details(userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không thể xác thực tài khoản")));
    }

    @Transactional(readOnly = true)
    public StoreUserDetails loadByUserId(Long userId) {
        return details(userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không thể xác thực tài khoản")));
    }

    private StoreUserDetails details(User user) {
        try {
            return new StoreUserDetails(user);
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException("Không thể xác thực tài khoản");
        }
    }
}
