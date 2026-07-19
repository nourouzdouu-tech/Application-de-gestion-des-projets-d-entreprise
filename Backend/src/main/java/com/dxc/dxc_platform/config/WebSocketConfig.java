package com.dxc.dxc_platform.config;

import com.dxc.dxc_platform.security.jwt.JwtService;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public WebSocketConfig(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-messages")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        String authHeader = accessor.getFirstNativeHeader("Authorization");
                        System.out.println("🔑 WebSocket CONNECT - Auth header: " + (authHeader != null ? "présent" : "absent"));

                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            try {
                                String email = jwtService.extractUsername(token);
                                System.out.println("  → Email extrait du token: " + email);

                                if (email != null) {
                                    UserDetails userDetails =
                                            userDetailsService.loadUserByUsername(email);
                                    if (jwtService.isTokenValid(token, userDetails)) {
                                        UsernamePasswordAuthenticationToken auth =
                                                new UsernamePasswordAuthenticationToken(
                                                        userDetails, null, userDetails.getAuthorities());
                                        accessor.setUser(auth);

                                        // Stocker l'email dans les attributs de session
                                        accessor.getSessionAttributes().put("email", email);
                                        System.out.println("  ✅ Authentification WebSocket réussie pour: " + email);
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("❌ WebSocket JWT invalide : " + e.getMessage());
                            }
                        }
                    }

                    if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        String email = (String) accessor.getSessionAttributes().get("email");
                        if (email != null) {
                            System.out.println("🔌 WebSocket DISCONNECT - Utilisateur: " + email);
                        }
                    }
                }
                return message;
            }
        });
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.modules(new JavaTimeModule());
        };
    }
    // Ajoutez cette méthode dans votre WebSocketConfig existant
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Canal sur lequel les clients écoutent
        config.enableSimpleBroker("/topic", "/queue", "/user");
        // Préfixe pour envoyer du client vers le serveur
        config.setApplicationDestinationPrefixes("/app");
        // Pour cibler un utilisateur précis
        config.setUserDestinationPrefix("/user");
    }

}
