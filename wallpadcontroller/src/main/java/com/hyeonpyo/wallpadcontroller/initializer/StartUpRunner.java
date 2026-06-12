package com.hyeonpyo.wallpadcontroller.initializer;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartUpRunner implements CommandLineRunner {

    private final DeviceTypeRepository deviceTypeRepository;
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        long count = deviceTypeRepository.count();
        if (count > 0) {
            log.info("✅ 기존 device_type 데이터가 존재합니다. 초기화 스킵.");
            return;
        }

        log.info("🧩 device_type 비어 있음. commax-initial.sql 실행 시작");

        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("commax-initial.sql"));
        }

        log.info("✅ commax-initial.sql 실행 완료");
    }
}
