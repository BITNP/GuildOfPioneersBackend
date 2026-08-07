package net.bitnp.guildofpioneers.user;

import net.bitnp.guildofpioneers.user.entity.User;
import net.bitnp.guildofpioneers.user.repository.UserRepository;
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
     * Loads the user for the given username as a Spring Security principal.
     * The lookup ignores case, so any casing of the username matches.
     *
     * @param username the username of the user to load
     * @return a UserDetails built from the persisted user
     * @throws UsernameNotFoundException if no user matches the username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserNameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with username " + username + " not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password(user.getPassword())
                .authorities(List.of(() -> "ROLE_USER"))
                .build();
    }
}
