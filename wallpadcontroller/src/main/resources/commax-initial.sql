-- commax-sql: insert into device_type
INSERT INTO device_type (id, name, type) VALUES (1, 'Light', 'light');
INSERT INTO device_type (id, name, type) VALUES (2, 'LightBreaker', 'switch');
INSERT INTO device_type (id, name, type) VALUES (3, 'Thermo', 'climate');
INSERT INTO device_type (id, name, type) VALUES (4, 'Gas', 'button');
INSERT INTO device_type (id, name, type) VALUES (5, 'Outlet', 'switch');
INSERT INTO device_type (id, name, type) VALUES (6, 'Fan', 'fan');
INSERT INTO device_type (id, name, type) VALUES (7, 'EV', 'button');

-- insert into packet_type
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (1, 1, 'command', '31');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (2, 1, 'state_request', '30');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (3, 1, 'state', 'B0');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (4, 2, 'command', '22');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (5, 2, 'state_request', '20');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (6, 2, 'state', 'A0');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (7, 3, 'command', '04');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (8, 3, 'state_request', '02');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (9, 3, 'state', '82');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (10, 3, 'ack', '84');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (11, 4, 'command', '11');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (12, 4, 'state', '90');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (13, 5, 'command', '7A');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (14, 5, 'state_request', '79');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (15, 5, 'state', 'F9');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (16, 6, 'command', '78');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (17, 6, 'state_request', '76');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (18, 6, 'state', 'F6');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (19, 7, 'command', 'FF');
INSERT INTO packet_type (id, device_type_id, kind, header) VALUES (20, 7, 'state', '23');

-- insert into packet_field
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (1, 1, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (2, 1, 2, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (3, 1, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (4, 1, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (5, 1, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (6, 1, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (7, 1, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (8, 2, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (9, 2, 2, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (10, 2, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (11, 2, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (12, 2, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (13, 2, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (14, 2, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (15, 3, 1, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (16, 3, 2, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (17, 3, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (18, 3, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (19, 3, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (20, 3, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (21, 3, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (22, 4, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (23, 4, 2, 'commandType');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (24, 4, 3, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (25, 4, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (26, 4, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (27, 4, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (28, 4, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (29, 5, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (30, 5, 2, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (31, 5, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (32, 5, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (33, 5, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (34, 5, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (35, 5, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (36, 6, 1, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (37, 6, 2, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (38, 6, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (39, 6, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (40, 6, 5, 'unknown');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (41, 6, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (42, 6, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (43, 7, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (44, 7, 2, 'commandType');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (45, 7, 3, 'value');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (46, 7, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (47, 7, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (48, 7, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (49, 7, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (50, 8, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (51, 8, 2, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (52, 8, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (53, 8, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (54, 8, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (55, 8, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (56, 8, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (57, 9, 1, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (58, 9, 2, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (59, 9, 3, 'currentTemp');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (60, 9, 4, 'targetTemp');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (61, 9, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (62, 9, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (63, 9, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (64, 10, 1, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (65, 10, 2, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (66, 10, 3, 'currentTemp');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (67, 10, 4, 'targetTemp');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (68, 10, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (69, 10, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (70, 10, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (71, 11, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (72, 11, 2, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (73, 11, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (74, 11, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (75, 11, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (76, 11, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (77, 11, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (78, 12, 1, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (79, 12, 2, 'powerRepeat');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (80, 12, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (81, 12, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (82, 12, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (83, 12, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (84, 12, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (85, 13, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (86, 13, 2, 'commandType');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (87, 13, 3, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (88, 13, 4, 'cutoffValue');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (89, 13, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (90, 13, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (91, 13, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (92, 14, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (93, 14, 2, 'requestType');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (94, 14, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (95, 14, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (96, 14, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (97, 14, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (98, 14, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (99, 15, 1, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (100, 15, 2, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (101, 15, 3, 'stateType');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (102, 15, 4, 'data1');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (103, 15, 5, 'data2');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (104, 15, 6, 'data3');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (105, 15, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (106, 16, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (107, 16, 2, 'commandType');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (108, 16, 3, 'value');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (109, 16, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (110, 16, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (111, 16, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (112, 16, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (113, 17, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (114, 17, 2, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (115, 17, 3, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (116, 17, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (117, 17, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (118, 17, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (119, 17, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (120, 18, 1, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (121, 18, 2, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (122, 18, 3, 'speed');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (123, 18, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (124, 18, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (125, 18, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (126, 18, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (127, 19, 1, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (128, 19, 2, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (129, 19, 3, 'unknown1');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (130, 19, 4, 'unknown2');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (131, 19, 5, 'unknown3');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (132, 19, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (133, 19, 7, 'checksum');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (134, 20, 1, 'power');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (135, 20, 2, 'deviceId');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (136, 20, 3, 'floor');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (137, 20, 4, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (138, 20, 5, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (139, 20, 6, 'empty');
INSERT INTO parsing_field (id, packet_type_id, position, name) VALUES (140, 20, 7, 'checksum');

-- insert into packet_field_value
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (1, 1, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (2, 2, 'ON', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (3, 2, 'OFF', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (4, 8, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (5, 15, 'ON', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (6, 15, 'OFF', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (7, 16, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (8, 22, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (9, 23, 'power', '01', '추정입니다');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (10, 24, 'ON', '01', '추정입니다');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (11, 24, 'OFF', '00', '추정입니다');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (12, 29, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (13, 36, 'ON', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (14, 36, 'OFF', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (15, 37, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (16, 40, 'unknown', '15', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (17, 43, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (18, 44, 'power', '04', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (19, 44, 'change', '03', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (20, 45, 'ON', '81', 'target은 10진수 온도');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (21, 45, 'OFF', '00', 'target은 10진수 온도');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (22, 45, 'target', 'FF', 'target은 10진수 온도');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (23, 50, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (24, 57, 'idle', '81', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (25, 57, 'heating', '83', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (26, 57, 'off', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (27, 58, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (28, 59, 'currentTemp', 'FF', '16진수가 아닌 10진수 그대로 (24도면 24)');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (29, 60, 'targetTemp', 'FF', '16진수가 아닌 10진수 그대로 (24도면 24)');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (30, 64, 'idle', '81', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (31, 64, 'heating', '83', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (32, 64, 'OFF', '80', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (33, 65, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (34, 66, 'currentTemp', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (35, 67, 'targetTemp', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (36, 71, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (37, 72, 'OFF', '80', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (38, 78, 'ON', 'A0', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (39, 78, 'OFF', '50', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (40, 79, 'ON', 'A0', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (41, 79, 'OFF', '50', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (42, 85, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (43, 86, 'power', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (44, 86, 'ecomode', '02', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (45, 86, 'setCutoff', '03', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (46, 87, 'ON', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (47, 87, 'OFF', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (48, 88, 'cutoffValue', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (49, 92, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (50, 93, 'wattage', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (51, 93, 'ecomode', '02', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (52, 99, 'ON', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (53, 99, 'OFF', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (54, 99, 'on_with_eco', '11', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (55, 99, 'off_with_eco', '10', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (56, 100, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (57, 101, 'wattage', '11', 'ecomode는 대기전력차단모드 cutoff value');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (58, 101, 'ecomode', '21', 'ecomode는 대기전력차단모드 cutoff value');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (59, 102, 'wattage', 'FF', 'wattage의 경우 data1~3까지 그대로 읽어서 x scailing factor');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (60, 102, 'ecomode', 'FF', 'wattage의 경우 data1~3까지 그대로 읽어서 x scailing factor');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (61, 103, 'wattage', 'FF', 'ecomode?는 000100 000060이 목격됨');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (62, 103, 'ecomode', 'FF', 'ecomode?는 000100 000060이 목격됨');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (63, 104, 'wattage', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (64, 104, 'ecomode', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (65, 106, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (66, 107, 'power', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (67, 107, 'setSpeed', '02', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (68, 108, 'OFF', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (69, 108, 'LOW', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (70, 108, 'MEDIUM', '02', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (71, 108, 'HIGH', '03', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (72, 108, 'ON', '04', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (73, 113, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (74, 120, 'NORMAL', '02', '최초 실행시 02');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (75, 120, 'NORMAL', '04', 'bypass에서 다시 변경시 04');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (76, 120, 'BYPASS', '07', '명령에 없는 night, auto가 존재한다고함.');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (77, 120, 'off', '00', '명령에 없는 night, auto가 존재한다고함.');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (78, 121, 'id', 'FF', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (79, 122, 'OFF', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (80, 122, 'LOW', '01', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (81, 122, 'MEDIUM', '02', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (82, 122, 'HIGH', '03', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (83, 127, 'id', 'FF', '기기 번호로 추정됨..  EV의 헤더는 A0로 동일, 추후 확인후 해야함');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (84, 128, 'ON', '01', '추정됨..');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (85, 129, 'fixed', '00', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (86, 130, 'fixed', '08', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (87, 131, 'fixed', '15', '');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (88, 134, 'ON', '01', 'power로 추정됨..');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (89, 135, 'id', 'FF', '기기 번호로 추정됨..');
INSERT INTO parsing_field_value (id, parsing_field_id, raw_key, hex, memo) VALUES (90, 136, 'floor', 'FF', '층으로 추정됨..');

-- command_mapping_rule
-- Light
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (1, 1, 'power', 'ON', 'light_power_on');
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (2, 1, 'power', 'OFF', 'light_power_off');

-- LightBreaker
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (3, 2, 'power', 'ON', 'lightbreaker_power_on');
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (4, 2, 'power', 'OFF', 'lightbreaker_power_off');

-- Thermo
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (5, 3, 'power', 'ON', 'thermo_power_on');
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (6, 3, 'power', 'OFF', 'thermo_power_off');
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (7, 3, 'setTemp', NULL, 'thermo_target');

-- Fan
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (8, 6, 'power', 'ON', 'fan_power_on');
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (9, 6, 'power', 'OFF', 'fan_power_off');
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (10, 6, 'speed', 'LOW', 'fan_speed_low');
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (11, 6, 'speed', 'MEDIUM', 'fan_speed_medium');
INSERT INTO command_mapping_rule (id, device_type_id, external_field, external_payload, rule_name) VALUES (12, 6, 'speed', 'HIGH', 'fan_speed_high');

-- command_mapping_detail
-- Light (ON)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (1, 'power', 'ON', false);
-- Light (OFF)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (2, 'power', 'OFF', false);

-- LightBreaker (ON)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (3, 'commandType', 'power', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (3, 'power', 'ON', false);
-- LightBreaker (OFF)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (4, 'commandType', 'power', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (4, 'power', 'OFF', false);

-- Thermo (ON)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (5, 'commandType', 'power', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (5, 'value', 'ON', false);
-- Thermo (OFF)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (6, 'commandType', 'power', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (6, 'value', 'OFF', false);
-- Thermo (target)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (7, 'commandType', 'change', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (7, 'value', NULL, true);

-- Fan (power ON)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (8, 'commandType', 'power', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (8, 'value', 'ON', false);
-- Fan (power OFF)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (9, 'commandType', 'power', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (9, 'value', 'OFF', false);
-- Fan (speed LOW)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (10, 'commandType', 'setSpeed', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (10, 'value', 'LOW', false);
-- Fan (speed MEDIUM)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (11, 'commandType', 'setSpeed', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (11, 'value', 'MEDIUM', false);
-- Fan (speed HIGH)
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (12, 'commandType', 'setSpeed', false);
INSERT INTO command_mapping_detail (rule_id, internal_field, internal_value, is_direct) VALUES (12, 'value', 'HIGH', false);