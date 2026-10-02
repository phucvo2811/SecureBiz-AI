package com.securebiz.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.identity.security.SecurityPrincipal;
import com.securebiz.inventory.repository.InventoryRepository;
import com.securebiz.inventory.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MethodSecurityRbacTest.Config.class)
class MethodSecurityRbacTest {
    @Autowired InventoryService service;
    @Autowired InventoryRepository repository;

    @AfterEach void clearSecurityContext(){SecurityContextHolder.clearContext();}

    @Test void missingPermissionIsDeniedBeforeRepositoryAccess(){
        authenticate(Set.of());
        assertThatThrownBy(service::warehouses).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test void matchingPermissionAllowsTheServiceCall(){
        when(repository.warehouses()).thenReturn(List.of());
        authenticate(Set.of("inventory.view"));
        assertThat(service.warehouses()).isEmpty();
    }

    private static void authenticate(Set<String> permissions){
        var principal=new SecurityPrincipal(UUID.randomUUID(),UUID.randomUUID(),"rbac-test",1,permissions);
        var authorities=permissions.stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal,"",authorities));
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean InventoryRepository inventoryRepository(){return mock(InventoryRepository.class);}
        @Bean ActivityLogRepository activityLogRepository(){return mock(ActivityLogRepository.class);}
        @Bean ObjectMapper objectMapper(){return new ObjectMapper().findAndRegisterModules();}
        @Bean InventoryService inventoryService(InventoryRepository inventoryRepository,
                ActivityLogRepository activityLogRepository,ObjectMapper objectMapper){
            return new InventoryService(inventoryRepository,activityLogRepository,objectMapper);
        }
    }
}
