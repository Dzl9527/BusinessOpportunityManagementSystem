package com.boms.service;

import com.boms.model.SystemUser;
import com.boms.repository.SystemDepartmentRepository;
import com.boms.repository.SystemUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceTest {

    @Mock
    private SystemUserRepository userRepository;

    @Mock
    private SystemDepartmentRepository departmentRepository;

    @Mock
    private FeishuService feishuService;

    @InjectMocks
    private UserDirectoryService userDirectoryService;

    @Test
    void getOrCreateUserKeepsLegacyWecomIdInSync() {
        when(userRepository.findByPlatformUserIdOrWecomUserId("ou_test", "ou_test"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(SystemUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemUser user = userDirectoryService.getOrCreateUser("ou_test", "测试用户", "");

        assertEquals("ou_test", user.getPlatformUserId());
        assertEquals("ou_test", user.getWecomUserId());
        assertEquals("测试用户", user.getName());
    }
}
