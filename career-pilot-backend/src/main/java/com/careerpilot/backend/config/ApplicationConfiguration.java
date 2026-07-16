package com.careerpilot.backend.config;

import com.careerpilot.backend.repository.IUserRepository;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfiguration {

    private final IUserRepository userRepository;

    @Bean
    UserDetailsService userDetailsService() {
        return identifier -> {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            String phoneRegex = "^\\+?[0-9]{7,15}$";

            if (Pattern.compile(emailRegex).matcher(identifier).matches()) {
                return userRepository.findByEmail(identifier)
                        .map(CustomUserDetails::new)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
            } else if (Pattern.compile(phoneRegex).matcher(identifier).matches()) {
                return userRepository.findByPhoneNumber(identifier)
                        .map(CustomUserDetails::new)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
            } else {
                return userRepository.findByUsername(identifier)
                        .map(CustomUserDetails::new)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
            }
        };
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
    @Bean 
    ChatClient chatClient(ChatClient.Builder builder){
      return builder.defaultSystem("You are Career Pilot AI, an interview preparation assistant. Respond only with the exact JSON format requested.").build();
    }
}
