package net.bitnp.guildofpioneers.service;

import net.bitnp.guildofpioneers.entity.User;
import net.bitnp.guildofpioneers.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads user accounts for Spring Security authentication.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user for the given phone number as a Spring Security principal.
     *
     * @param phone the phone number of the user to load
     * @return a UserDetails built from the persisted user
     * @throws UsernameNotFoundException if no user matches the phone number
     */
    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("User with phone " + phone + " not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getPhone())
                .password(user.getPassword())
                .authorities(List.of(() -> "ROLE_USER"))
                .build();
    }
}
