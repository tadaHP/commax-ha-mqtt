// ===============================
// 패킷 히스토리 관련 클래스
// ===============================
class PacketHistory {
    constructor() {
        this.HISTORY_KEY = 'packet_analysis_history';
        this.MAX_HISTORY = 20;
        this.historyIndex = -1;
        this.currentInput = '';
    }

    load() {
        try {
            return JSON.parse(localStorage.getItem(this.HISTORY_KEY) || '[]');
        } catch {
            return [];
        }
    }

    save(packet) {
        if (!packet) return;
        
        let history = this.load();
        history = history.filter(p => p !== packet); // 중복 제거
        history.unshift(packet); // 새 패킷을 앞에 추가
        
        // 최대 개수 유지
        if (history.length > this.MAX_HISTORY) {
            history = history.slice(0, this.MAX_HISTORY);
        }
        
        localStorage.setItem(this.HISTORY_KEY, JSON.stringify(history));
        this.historyIndex = -1; // 히스토리 인덱스 초기화
        
        // 드롭다운 목록 업데이트
        const historySelect = document.getElementById('packetHistory');
        if (historySelect) {
            historySelect.innerHTML = '<option value="">패킷 기록...</option>' +
                history.map(p => `<option value="${p}">${utils.formatPacket(p)}</option>`).join('');
        }
    }

    select() {
        const historySelect = document.getElementById('packetHistory');
        const packetInput = document.getElementById('packetInput');
        if (historySelect && historySelect.value) {
            packetInput.value = utils.formatPacket(historySelect.value);
            analyzePacket();
        }
    }

    handleKeyNavigation(event, inputElement) {
        const history = this.load();
        
        if (event.key === 'ArrowUp') {
            event.preventDefault();
            if (this.historyIndex === -1) {
                this.currentInput = inputElement.value;
            }
            if (this.historyIndex < history.length - 1) {
                this.historyIndex++;
                inputElement.value = history[this.historyIndex];
                handlePacketInput({target: inputElement});
            }
        } else if (event.key === 'ArrowDown') {
            event.preventDefault();
            if (this.historyIndex > -1) {
                this.historyIndex--;
                inputElement.value = this.historyIndex === -1 ? this.currentInput : history[this.historyIndex];
                handlePacketInput({target: inputElement});
            }
        }
    }
}

// ===============================
// 패킷 분석기 관련 클래스
// ===============================
class PacketAnalyzer {
    constructor() {
        this.packetSuggestions = null;
        this.utils = {
            formatPacket: packet => packet.match(/.{2}/g).join(' '),
            isValidPacket: packet => /^[0-9A-F]{14}$|^[0-9A-F]{16}$/.test(packet),
            getTimestamp: () => new Date().toLocaleTimeString('ko-KR', { hour12: false }),
            cleanPacket: input => input.replace(/[\s-]+/g, '').trim().toUpperCase(),
            isValidHex: packet => /^[0-9A-F]*$/.test(packet),
            padPacket: packet => packet.padEnd(14, '0'),
            validatePacket: (packet) => {
                if (!packet) return { isValid: false };
                if (!this.utils.isValidHex(packet)) {
                    return {
                        isValid: false,
                        error: "잘못된 문자가 포함되어 있습니다. 16진수만 입력해주세요."
                    };
                }
                if (!this.utils.isValidPacket(packet)) {
                    if (packet.length >= 2 && packet.length < 14) {
                        return {
                            isValid: false,
                            shouldPad: true
                        };
                    }
                    return {
                        isValid: false,
                        error: "패킷은 14자리 또는 16자리여야 합니다."
                    };
                }
                return { isValid: true };
            }
        };

        this.bindEvents();
        this.loadPacketSuggestions();
    }

    loadPacketSuggestions() {
        fetch('./api/packet_suggestions')
            .then(response => response.json())
            .then(data => {
                this.packetSuggestions = data;
                this.showAvailableHeaders();
            });
    }

    bindEvents() {
        // 분석 버튼 이벤트 리스너
        const analyzeButton = document.getElementById('analyzePacketButton');
        if (analyzeButton) {
            analyzeButton.addEventListener('click', () => this.analyzePacket());
        }

        // 전송 버튼 이벤트 리스너
        const sendButton = document.getElementById('sendPacketButton');
        if (sendButton) {
            sendButton.addEventListener('click', () => this.sendPacket());
        }

        // 패킷 입력 필드 이벤트 리스너
        const packetInput = document.getElementById('analyzerPacketInput');
        if (packetInput) {
            packetInput.addEventListener('input', (e) => this.handlePacketInput(e));
            packetInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    this.analyzePacket();
                } else if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
                    packetHistory.handleKeyNavigation(e, e.target);
                }
            });
            packetInput.addEventListener('focus', (e) => {
                if (!e.target.value) {
                    this.showAvailableHeaders();
                }
            });
        }
    }

    detectPacketType(header) {
        if (!this.packetSuggestions || !this.packetSuggestions.headers) {
            return 'command';  // 기본값
        }
        
        const types = {
            'state': 'state',
            'state_request': 'state_request',
            'ack': 'ack'
        };
        
        for (const [type, value] of Object.entries(types)) {
            if (this.packetSuggestions.headers[type].some(h => h.header === header)) {
                return value;
            }
        }
        
        return 'command';
    }

    showAvailableHeaders() {
        if (!this.packetSuggestions) return;
        const resultDiv = document.getElementById('packetResult');
        let html = '<h3 class="text-lg font-bold mb-2 dark:text-white">사용 가능한 헤더:</h3>';
        html += '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">';
        
        // 명령 패킷 헤더
        html += '<div class="space-y-2">';
        html += '<h4 class="font-bold text-sm text-gray-600 dark:text-gray-400">명령 패킷</h4>';
        this.packetSuggestions.headers.command.forEach(header => {
            html += `<div class="text-sm"><span class="font-mono bg-gray-100 dark:bg-gray-700 px-1 dark:text-gray-300">${header.header}</span> - <span class="dark:text-gray-400">${header.device}</span></div>`;
        });
        html += '</div>';
        
        // 상태 패킷 헤더
        html += '<div class="space-y-2">';
        html += '<h4 class="font-bold text-sm text-gray-600 dark:text-gray-400">상태 패킷</h4>';
        this.packetSuggestions.headers.state.forEach(header => {
            html += `<div class="text-sm"><span class="font-mono bg-gray-100 dark:bg-gray-700 px-1 dark:text-gray-300">${header.header}</span> - <span class="dark:text-gray-400">${header.device}</span></div>`;
        });
        html += '</div>';
        
        // 상태 요청 패킷 헤더
        html += '<div class="space-y-2">';
        html += '<h4 class="font-bold text-sm text-gray-600 dark:text-gray-400">상태 요청 패킷</h4>';
        this.packetSuggestions.headers.state_request.forEach(header => {
            html += `<div class="text-sm"><span class="font-mono bg-gray-100 dark:bg-gray-700 px-1 dark:text-gray-300">${header.header}</span> - <span class="dark:text-gray-400">${header.device}</span></div>`;
        });
        html += '</div>';
        
        // 응답 패킷 헤더
        html += '<div class="space-y-2">';
        html += '<h4 class="font-bold text-sm text-gray-600 dark:text-gray-400">응답 패킷</h4>';
        this.packetSuggestions.headers.ack.forEach(header => {
            html += `<div class="text-sm"><span class="font-mono bg-gray-100 dark:bg-gray-700 px-1 dark:text-gray-300">${header.header}</span> - <span class="dark:text-gray-400">${header.device}</span></div>`;
        });
        html += '</div>';
        
        html += '</div>';
        resultDiv.innerHTML = html;
    }

    handlePacketInput(e) {
        const input = e.target;
        if (!(input instanceof HTMLInputElement)) return;
        const packet = input.value.replace(/[\s-]+/g, '').trim().toUpperCase();
        
        if (packet.length === 0) {
            this.showAvailableHeaders();
            return;
        }
        if (packet.length >= 2) {
            // 입력된 패킷이 2자리 이상이면 나머지를 00으로 채워서 분석
            const paddedPacket = packet.padEnd(14, '0');
            if (/^[0-9A-F]+$/.test(packet)) {  // 유효한 16진수인 경우에만 분석
                this.analyzePacket(paddedPacket);
            }
        }
    }

    analyzePacket(paddedPacket) {
        const packetInput = document.getElementById('analyzerPacketInput');
        if (!(packetInput instanceof HTMLInputElement)) return;
        const resultDiv = document.getElementById('packetResult');
        
        // 입력값 정리
        const packet = this.utils.cleanPacket(paddedPacket || packetInput.value);
        
        if (!packet) {
            this.showAvailableHeaders();
            return;
        }
        
        // 패킷 유효성 검사
        const validation = this.utils.validatePacket(packet);
        if (!validation.isValid) {
            if (validation.shouldPad && !paddedPacket) {
                this.analyzePacket(this.utils.padPacket(packet));
                return;
            }
            if (validation.error) {
                resultDiv.innerHTML = `<p class="text-red-500 dark:text-red-400">${validation.error}</p>`;
                return;
            }
            return;
        }
        
        // Enter 키로 분석한 경우에만 히스토리에 저장
        if (!paddedPacket) {
            packetHistory.save(packet);
        }
        
        // 헤더로 패킷 타입 자동 감지
        const header = packet.substring(0, 2);
        const packetType = this.detectPacketType(header);
        
        fetch('./api/analyze_packet', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ 
                command: packet,
                type: packetType
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                this.displayPacketAnalysis([{
                    device: data.device,
                    packet_type: PACKET_TYPES[data.packet_type || 'command'],
                    byte_meanings: data.analysis.reduce((acc, desc) => {
                        const match = desc.match(/Byte (\d+): (.+)/);
                        if (match) {
                            const [, byteNum, description] = match;
                            if (byteNum === '0' && description.startsWith('header')) {
                                acc[byteNum] = description;
                            } 
                            else if (description.includes('체크섬')) {
                                acc[byteNum] = description;
                            }
                            else {
                                const [name, value] = description.split(' = ');
                                if (value) {
                                    acc[byteNum] = `${name} = ${value}`;
                                } else {
                                    acc[byteNum] = description;
                                }
                            }
                        }
                        return acc;
                    }, {}),
                    checksum: data.checksum,
                    expected_state: data.expected_state
                }]);
            } else {
                resultDiv.innerHTML = `<p class="text-red-500 dark:text-red-400">오류: ${data.error}</p>`;
            }
        })
        .catch(error => {
            resultDiv.innerHTML = `<p class="text-red-500 dark:text-red-400">요청 실패: ${error}</p>`;
        });
    }

    displayPacketAnalysis(results) {
        const resultDiv = document.getElementById('packetResult');
        if (!results.length) {
            resultDiv.innerHTML = `<div class="text-red-500 dark:text-red-400">매칭되는 패킷 구조를 찾을 수 없습니다.</div>`;
            return;
        }

        const result = results[0];
        const _device = result.device;
        const _packet_type = result.packet_type;
        const _required_bytes = result.expected_state?.required_bytes || [];
        const _possible_values = result.expected_state?.possible_values || {};

        const darkTextClass = 'dark:text-gray-400';
        const darkHeadingClass = 'dark:text-white';
        const accentClass = 'font-bold text-blue-600 dark:text-blue-400';
        const topBorderClass = 'mt-4 pt-4 border-t dark:border-gray-700';
        
        function generateTableRow(label, values, isKey) {
            if (isKey) {
                return `
                    <tr>
                        <th class="text-left dark:text-gray-300 pr-2">${label}:</th>
                        ${values.map(value => `
                            <td class="${darkTextClass} font-mono px-2 ${isKey && _required_bytes.includes(value) ? accentClass : ''}">${value}</td>
                        `).join('')}
                    </tr>
                `;
            } else {
                return `<tr>
                    <td class="text-left dark:text-gray-300 pr-2">${label}:</td>
                    ${values.map(valueList => `
                        <td class="${darkTextClass} font-mono px-2">${Array.isArray(valueList) ? valueList.join('<br>') : valueList}</td>
                    `).join('')}
                </tr>`;
            }
        }

        const byteMeanings = Object.entries(result.byte_meanings || {})
            .map(([byte, meaning]) => `
                <div class="mb-2">
                    <span class="font-medium dark:text-gray-300">Byte ${byte}:</span>
                    <span class="ml-2 ${darkTextClass}">${meaning}</span>
                </div>
            `).join('');

        const expectedStateContent = result.expected_state ? `
            <div class="${topBorderClass}">
                <h4 class="text-md font-medium mb-2 ${darkHeadingClass}">예상 상태 패킷</h4>
                <div class="space-y-2">
                    ${_required_bytes ? `
                        <div class="text-sm">
                            <span class="font-medium dark:text-gray-300">필수 바이트:</span>
                            <span class="ml-2 font-mono ${darkTextClass}">${_required_bytes}</span>
                        </div>
                    ` : ''}
                    ${_possible_values ? `
                        <div class="text-sm">
                            <div class="ml-4">
                                <table class="w-full">
                                    ${generateTableRow('위치', Object.keys(_possible_values), true)}
                                    ${generateTableRow('가능한 값', Object.values(_possible_values))}
                                </table>
                            </div>
                        </div>
                    ` : ''}
                </div>
            </div>
        ` : '';

        resultDiv.innerHTML = `
            <div class="bg-white dark:bg-gray-800 p-4 rounded-lg shadow mb-4">
                <div class="flex items-center gap-2 mb-2">
                    <h3 class="text-lg font-medium ${darkHeadingClass}">${_device}</h3>
                    <span class="text-sm text-gray-500 ${darkTextClass}">${_packet_type}</span>
                </div>
                ${result.checksum ? `
                    <div class="${topBorderClass} text-gray-600 text-sm ${darkTextClass}">
                        <span class="font-medium dark:text-gray-300">체크섬 포함 패킷:</span>
                        <span class="ml-2">${result.checksum}</span>
                    </div>
                ` : ''}
                <div class="${topBorderClass}">
                    ${byteMeanings}
                </div>
                ${expectedStateContent}
            </div>
        `;
    }

    analyzeExpectedState(packet) {
        document.getElementById('analyzerPacketInput').value = packet;
        this.analyzePacket();
    }

    sendPacket() {
        const packetInput = document.getElementById('analyzerPacketInput');
        if (!(packetInput instanceof HTMLInputElement)) return;
        const packet = packetInput.value.replace(/[\s-]+/g, '').trim();

        fetch('./api/send_packet', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ packet: packet })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('패킷을 전송했습니다.');
            } else {
                alert('패킷 전송에 실패했습니다.');
            }
        });
    }
}
