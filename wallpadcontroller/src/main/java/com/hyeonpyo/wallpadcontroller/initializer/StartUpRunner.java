package com.hyeonpyo.wallpadcontroller.initializer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.domain.definition.repository.DeviceTypeRepository;

// import com.hyeonpyo.wallpadcontroller.parser.DeviceStructureLoader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartUpRunner implements CommandLineRunner{

    // private final DeviceStructureLoader deviceStructureLoader;
    private final DeviceTypeRepository deviceTypeRepository;
    private final DataSource dataSource;



    @Override
    public void run(String... args) throws Exception {
        // deviceStructureLoader.loadDeviceStructure();

        // Map<String, Object> structure = deviceStructureLoader.getDeviceStructure();
        // structure.keySet().forEach(name -> System.out.println("로드된 기기: " + name));
        long count = deviceTypeRepository.count();
        if (count > 0) {
            log.info("✅ 기존 device_type 데이터가 존재합니다. 초기화 스킵.");
            return;
        }

        log.info("🧩 device_type 비어 있음. commax-initial.sql 실행 시작");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             InputStream input = getClass().getClassLoader().getResourceAsStream("commax-initial.sql")) {

            if (input == null) {
                throw new FileNotFoundException("리소스 파일을 찾을 수 없습니다: commax-initial.sql");
            }

            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String part : sql.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }

            System.out.println("✅ commax-initial.sql 실행 완료");

        } catch (SQLException e) {
            System.err.println("❌ SQL 실행 중 오류 발생: " + e.getMessage());
            throw e;
        }


    }
    
}
