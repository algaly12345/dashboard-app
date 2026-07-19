package com.realestate.admin.service;

import com.realestate.admin.entity.BusinessSetting;
import com.realestate.admin.repository.BusinessSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final BusinessSettingRepository repository;

    /** Latest value for this setting type, or the given default if it has never been set. */
    public String get(String type, String defaultValue) {
        List<BusinessSetting> rows = repository.findByTypeOrderByIdDesc(type);
        return rows.isEmpty() ? defaultValue : rows.get(0).getValue();
    }

    /** Updates the latest row for this type in place, or creates the first one if none exists yet. */
    public void set(String type, String value) {
        List<BusinessSetting> rows = repository.findByTypeOrderByIdDesc(type);
        BusinessSetting setting = rows.isEmpty() ? new BusinessSetting() : rows.get(0);
        setting.setType(type);
        setting.setValue(value == null ? "" : value);
        setting.setUpdatedAt(LocalDateTime.now());
        if (setting.getCreatedAt() == null) setting.setCreatedAt(LocalDateTime.now());
        repository.save(setting);
    }
}
