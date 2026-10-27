package com.tcgshop.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tcgshop.dto.request.SystemSettingRequest;
import com.tcgshop.entity.SystemSetting;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.SystemSettingMapper;
import com.tcgshop.repository.SystemSettingRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.UploadResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceImplTest {
    @Mock private SystemSettingRepository repository;
    @Mock private CloudinaryService cloudinary;
    @Mock private CurrentUser currentUser;
    private SystemSettingServiceImpl service() {
        return new SystemSettingServiceImpl(repository, org.mapstruct.factory.Mappers.getMapper(SystemSettingMapper.class), cloudinary, currentUser);
    }
    private SystemSettingRequest request() {
        var request = new SystemSettingRequest();
        request.setSiteName(" New Market "); request.setContactPhone("0901234567"); request.setContactEmail("help@example.com");
        return request;
    }
    private SystemSetting existing() {
        when(currentUser.isAuthenticated()).thenReturn(true);
        when(currentUser.hasRole(RoleName.ROLE_ADMIN)).thenReturn(true);
        var setting = new SystemSetting(); setting.setId(1L); setting.setLogoUrl("https://old"); setting.setLogoPublicId("old");
        when(repository.findSingletonForUpdate()).thenReturn(Optional.of(setting));
        return setting;
    }
    @Test void defaultsDoNotCreateADatabaseRow() {
        when(repository.findBySingletonKey(1)).thenReturn(Optional.empty());
        assertThat(service().getSettings().siteName()).isEqualTo("TCGSHOP");
        verify(repository, never()).initializeIfAbsent();
    }
    @Test void nonAdminCannotSave() {
        assertThatThrownBy(() -> service().save(request())).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(repository, cloudinary);
    }
    @Test void savesTextAndPreservesLogoWithoutUpload() {
        var setting = existing(); service().save(request());
        assertThat(setting.getSiteName()).isEqualTo("New Market");
        assertThat(setting.getLogoPublicId()).isEqualTo("old");
        verify(repository).saveAndFlush(setting); verifyNoInteractions(cloudinary);
    }
    @Test void replacementDeletesOldLogoOnlyAfterCommitAndNewLogoOnRollback() {
        var setting = existing(); var request = request();
        var logo = new MockMultipartFile("logo", "logo.png", "image/png", new byte[]{1}); request.setLogo(logo);
        when(cloudinary.uploadImage(logo, "tcgshop/system")).thenReturn(new UploadResult("https://new", "new"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service().save(request);
            verify(cloudinary, never()).delete(anyString(), anyString());
            assertThat(setting.getLogoUrl()).isEqualTo("https://new");
            var callbacks = TransactionSynchronizationManager.getSynchronizations();
            callbacks.forEach(TransactionSynchronization::afterCommit);
            verify(cloudinary).delete("old", "image");
            callbacks.forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            verify(cloudinary, never()).delete("new", "image");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void failedDatabaseSaveCleansNewLogoAndKeepsOldCloudinaryImage() {
        var setting = existing(); var request = request();
        var logo = new MockMultipartFile("logo", "logo.png", "image/png", new byte[]{1}); request.setLogo(logo);
        when(cloudinary.uploadImage(logo, "tcgshop/system")).thenReturn(new UploadResult("https://new", "new"));
        doThrow(new IllegalStateException("db failure")).when(repository).saveAndFlush(setting);
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> service().save(request)).isInstanceOf(IllegalStateException.class);
            TransactionSynchronizationManager.getSynchronizations().forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(cloudinary).delete("new", "image"); verify(cloudinary, never()).delete("old", "image");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
