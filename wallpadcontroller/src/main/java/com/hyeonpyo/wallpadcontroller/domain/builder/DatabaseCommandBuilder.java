package com.hyeonpyo.wallpadcontroller.domain.builder;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.CommandMappingDetail;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.CommandMappingRule;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.CommandMappingRuleRepository;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.PacketTypeRepository;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.ParsingFieldRepository;
import com.hyeonpyo.wallpadcontroller.domain.definition.repository.ParsingFieldValueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCommandBuilder implements CommandBuilder {

    private final CommandMappingRuleRepository ruleRepository;
    private final PacketTypeRepository packetTypeRepository;
    private final ParsingFieldRepository parsingFieldRepository;
    private final ParsingFieldValueRepository parsingFieldValueRepository;

    private static final int PACKET_LENGTH = 8;

    @Override
    public Optional<byte[]> build(String type, int index, String field, String payload) {
        // deviceType 찾기 (ex: Light, Fan 등)
        Optional<CommandMappingRule> ruleOptional = ruleRepository.findWithDetailsByDeviceTypeNameAndExternalFieldAndExternalPayload(type, field, payload);
        if (ruleOptional.isEmpty()) {
            log.warn("⚠️ 매칭되는 CommandMappingRule 없음: type={}, field={}, payload={}", type, field, payload);
            return Optional.empty();
        }

        CommandMappingRule rule = ruleOptional.get();

        // 패킷 생성할 PacketType 가져오기 (always 'command')
        PacketType packetType = packetTypeRepository.findByDeviceTypeAndKind(rule.getDeviceType(), "command")
                .orElseThrow(() -> new IllegalStateException("PacketType not found for command"));

        // 패킷 전체 초기화
        byte[] packet = new byte[PACKET_LENGTH];
        packet[0] = (byte) Integer.parseInt(packetType.getHeader(), 16);

        // 패킷 필드 정보 읽어오기
        Map<Integer, ParsingField> parsingFieldMap = parsingFieldRepository.findByPacketType(packetType).stream()
                .collect(Collectors.toMap(ParsingField::getPosition, pf -> pf));

        // 값 세팅
        for (int pos = 1; pos < PACKET_LENGTH - 1; pos++) {
            ParsingField parsingField = parsingFieldMap.get(pos);
            if (parsingField == null || parsingField.getName().equals("empty")) {
                packet[pos] = 0x00;
                continue;
            }
            String hexValue = resolveHexValue(parsingField, rule, payload, index);
            packet[pos] = (byte) Integer.parseInt(hexValue, 16);
        }

        // 체크섬 계산
        packet[PACKET_LENGTH - 1] = calculateChecksum(packet);
        return Optional.of(packet);
    }

    // 내부 필드 → hex 값 매핑
    private String resolveHexValue(ParsingField field, CommandMappingRule rule, String payload, int deviceIndex) {
        String fieldName = field.getName();

        // deviceId 필드는 따로 index 사용
        if (fieldName.equals("deviceId")) {
            return String.format("%02X", deviceIndex);
        }

        // rule 기반 내부 필드값 조회
        Optional<CommandMappingDetail> detailOpt = rule.getDetails().stream()
                .filter(d -> d.getInternalField().equals(fieldName))
                .findFirst();

        if (detailOpt.isPresent()) {
            CommandMappingDetail detail = detailOpt.get();
            String rawKey = detail.isDirect() ? payload : detail.getInternalValue();
            return parsingFieldValueRepository.findHexByParsingFieldAndRawKey(field, rawKey)
                    .orElse("00");
        }

        // rule에 정의되지 않은 필드면 default 00
        return "00";
    }

    private byte calculateChecksum(byte[] packet) {
        int sum = 0;
        for (int i = 0; i < packet.length - 1; i++) {
            sum += packet[i] & 0xFF;
        }
        return (byte) (sum % 256);
    }
}
