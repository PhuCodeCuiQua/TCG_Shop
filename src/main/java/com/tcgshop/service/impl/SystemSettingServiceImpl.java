package com.tcgshop.service.impl;

import com.tcgshop.dto.request.SystemSettingRequest;
import com.tcgshop.dto.response.SystemSettingResponse;
import com.tcgshop.entity.enums.RoleName;
import com.tcgshop.exception.ForbiddenException;
import com.tcgshop.mapper.SystemSettingMapper;
import com.tcgshop.repository.SystemSettingRepository;
import com.tcgshop.security.CurrentUser;
import com.tcgshop.service.CloudinaryService;
import com.tcgshop.service.SystemSettingService;
import com.tcgshop.util.TransactionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {
    private final SystemSettingRepository repository;
    private final SystemSettingMapper mapper;
    private final CloudinaryService cloudinaryService;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public SystemSettingResponse getSettings() {
        return repository.findBySingletonKey(1).map(mapper::toResponse).orElseGet(SystemSettingResponse::defaults);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void save(SystemSettingRequest request) {
        if (!currentUser.isAuthenticated() || !currentUser.hasRole(RoleName.ROLE_ADMIN)) {
            throw new ForbiddenException("Chỉ Admin được thay đổi cấu hình hệ thống");
        }
        // Khởi tạo nguyên tử và khóa dòng chung để hai Admin không tạo trùng hoặc thay logo chồng nhau.
        repository.initializeIfAbsent();
        var setting = repository.findSingletonForUpdate().orElseThrow();
        mapper.update(request, setting);
        if (request.getLogo() != null && !request.getLogo().isEmpty()) {
            var uploaded = cloudinaryService.uploadImage(request.getLogo(), "tcgshop/system");
            TransactionUtils.runAfterRollback(() -> cloudinaryService.delete(uploaded.publicId(), "image"));
            String oldPublicId = setting.getLogoPublicId();
            setting.setLogoUrl(uploaded.url());
            setting.setLogoPublicId(uploaded.publicId());
            // Chỉ xóa ảnh cũ sau khi DB commit; rollback sẽ dọn ảnh mới và giữ ảnh cũ hoạt động.
            if (oldPublicId != null && !oldPublicId.isBlank() && !oldPublicId.equals(uploaded.publicId())) {
                TransactionUtils.runAfterCommit(() -> cloudinaryService.delete(oldPublicId, "image"));
            }
        }
        repository.saveAndFlush(setting);
    }
}
