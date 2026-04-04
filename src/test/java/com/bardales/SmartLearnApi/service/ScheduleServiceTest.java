package com.bardales.SmartLearnApi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bardales.SmartLearnApi.domain.entity.ScheduleMembership;
import com.bardales.SmartLearnApi.domain.entity.ScheduleProfile;
import com.bardales.SmartLearnApi.domain.entity.User;
import com.bardales.SmartLearnApi.domain.repository.ScheduleActivityRepository;
import com.bardales.SmartLearnApi.domain.repository.ScheduleMembershipRepository;
import com.bardales.SmartLearnApi.domain.repository.ScheduleProfileRepository;
import com.bardales.SmartLearnApi.domain.repository.UserRepository;
import com.bardales.SmartLearnApi.dto.schedule.ScheduleModuleResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ScheduleProfileRepository scheduleProfileRepository;
    @Mock
    private ScheduleMembershipRepository scheduleMembershipRepository;
    @Mock
    private ScheduleActivityRepository scheduleActivityRepository;

    private ScheduleService service;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(
                userRepository,
                scheduleProfileRepository,
                scheduleMembershipRepository,
                scheduleActivityRepository);

        when(scheduleActivityRepository.findByScheduleProfileIdAndDeletedAtIsNullOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());
    }

    @Test
    void getModuleCreatesPersonalBaseEvenWhenUserOnlyHasSharedMembership() {
        User user = user(10L, "User B", "userb");
        User owner = user(20L, "User A", "usera");

        ScheduleProfile sharedProfile = scheduleProfile(201L, owner, "Horario de A");
        ScheduleMembership membership = membership(sharedProfile, user, "viewer", false);

        ScheduleProfile createdPersonal = scheduleProfile(301L, user, "Horario de User B");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(scheduleProfileRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(), List.of(createdPersonal), List.of(createdPersonal));
        when(scheduleMembershipRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(membership));
        when(scheduleProfileRepository.save(any(ScheduleProfile.class))).thenReturn(createdPersonal);

        ScheduleModuleResponse response = service.getModule(user.getId(), null);

        assertEquals(createdPersonal.getId(), response.profileId());
        assertEquals("owner", response.accessRole());
        assertTrue(response.canEdit());
        assertEquals(2, response.profiles().size());
        assertTrue(response.profiles().stream().anyMatch(p -> p.profileId().equals(createdPersonal.getId()) && "owner".equals(p.accessRole())));
        assertTrue(response.profiles().stream().anyMatch(p -> p.profileId().equals(sharedProfile.getId()) && "viewer".equals(p.accessRole())));
        verify(scheduleProfileRepository, times(1)).save(any(ScheduleProfile.class));
    }

    @Test
    void getModuleWithSharedScheduleKeepsOwnAndSharedProfilesVisible() {
        User user = user(10L, "User B", "userb");
        User owner = user(20L, "User A", "usera");

        ScheduleProfile ownProfile = scheduleProfile(101L, user, "Horario de B");
        ScheduleProfile sharedProfile = scheduleProfile(201L, owner, "Horario de A");
        ScheduleMembership membership = membership(sharedProfile, user, "viewer", false);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(scheduleProfileRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(ownProfile), List.of(ownProfile));
        when(scheduleProfileRepository.findByIdAndDeletedAtIsNull(sharedProfile.getId())).thenReturn(Optional.of(sharedProfile));
        when(scheduleMembershipRepository.findByScheduleProfileIdAndUserIdAndDeletedAtIsNull(sharedProfile.getId(), user.getId()))
                .thenReturn(Optional.of(membership));
        when(scheduleMembershipRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(membership));

        ScheduleModuleResponse response = service.getModule(user.getId(), sharedProfile.getId());

        assertEquals(sharedProfile.getId(), response.profileId());
        assertEquals("viewer", response.accessRole());
        assertFalse(response.canEdit());
        assertEquals(2, response.profiles().size());
        assertTrue(response.profiles().stream().anyMatch(p -> p.profileId().equals(ownProfile.getId()) && "owner".equals(p.accessRole())));
        assertTrue(response.profiles().stream().anyMatch(p -> p.profileId().equals(sharedProfile.getId()) && "viewer".equals(p.accessRole())));
    }

    private static User user(Long id, String name, String username) {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setEmail(username + "@mail.com");
        user.setPassword("x");
        setBaseFields(user, id);
        return user;
    }

    private static ScheduleProfile scheduleProfile(Long id, User owner, String name) {
        ScheduleProfile profile = new ScheduleProfile();
        profile.setOwnerUser(owner);
        profile.setName(name);
        profile.setVisibility("private");
        profile.setDeletedAt(null);
        setBaseFields(profile, id);
        return profile;
    }

    private static ScheduleMembership membership(ScheduleProfile profile, User user, String role, boolean canShare) {
        ScheduleMembership membership = new ScheduleMembership();
        membership.setScheduleProfile(profile);
        membership.setUser(user);
        membership.setRole(role);
        membership.setCanShare(canShare);
        membership.setDeletedAt(null);
        setBaseFields(membership, (profile.getId() * 100) + user.getId());
        return membership;
    }

    private static void setBaseFields(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "createdAt", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(target, "updatedAt", LocalDateTime.now());
        assertNotNull(ReflectionTestUtils.getField(target, "id"));
    }
}
