package com.lastwar.ano2193.service;

import com.lastwar.ano2193.model.User;
import com.lastwar.ano2193.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("loadUserByUsername username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.debug("loadUserByUsername: no user found for '{}'", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        log.trace("loadUserByUsername: username={} enabled={} authorities={}",
                username, user.isEnabled(), authorities);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                authorities);
    }

    public User createUser(String username, String password, Set<String> roles) {
        log.debug("createUser username={} roles={}", username, roles);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(roles);
        user.setEnabled(true);
        User saved = userRepository.save(user);
        log.trace("createUser → id={} username={}", saved.getId(), saved.getUsername());
        return saved;
    }

    public List<User> findAll() {
        log.debug("findAll users");
        List<User> users = userRepository.findAll();
        log.trace("findAll users → count={}", users.size());
        return users;
    }

    public Optional<User> findById(Long id) {
        log.debug("findById id={}", id);
        Optional<User> result = userRepository.findById(id);
        log.trace("findById id={} → found={}", id, result.isPresent());
        return result;
    }

    public Optional<User> findByUsername(String username) {
        log.debug("findByUsername username={}", username);
        Optional<User> result = userRepository.findByUsername(username);
        log.trace("findByUsername username={} → found={}", username, result.isPresent());
        return result;
    }

    public void deleteUser(Long id) {
        log.debug("deleteUser id={}", id);
        userRepository.deleteById(id);
        log.trace("deleteUser id={} complete", id);
    }

    public boolean existsByUsername(String username) {
        boolean exists = userRepository.existsByUsername(username);
        log.trace("existsByUsername username={} → {}", username, exists);
        return exists;
    }
}
