package com.lastwar.ano2193.repository;

import com.lastwar.ano2193.model.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}