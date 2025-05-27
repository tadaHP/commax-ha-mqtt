// ===============================
// 대시보드 관련 클래스
// ===============================
class Dashboard {
    constructor() {
        this.deviceManager = new DeviceManager();
        this.initializeIntervals();
    }

    initializeIntervals() {
        // 주기적 업데이트 설정
        setInterval(() => this.updateMqttStatus(), 5000);   // 5초마다 MQTT 상태 업데이트
        setInterval(() => this.updateEW11Status(), 5000);   // 5초마다 EW11 상태 업데이트
        setInterval(() => this.updateRecentMessages(), 2000); // 2초마다 최근 메시지 업데이트
        setInterval(() => this.deviceManager.updateDeviceList(), 10000);  // 10초마다 기기목록 업데이트
    }
    
    updateEW11Status() {
        fetch('./api/ew11_status')
            .then(response => response.json())
            .then(data => {
                const statusElement = document.getElementById('ew11ConnectionStatus');
                
                if (!data.last_recv_time) {
                    statusElement.textContent = '응답 없음';
                    statusElement.className = 'px-2 py-1 rounded text-sm bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-100';
                    return;
                }
                
                const currentTime = Math.floor(Date.now() / 1000);
                const lastRecvTime = Math.floor(data.last_recv_time / 1000000000);
                const timeDiff = currentTime - lastRecvTime;
                
                const isConnected = timeDiff <= data.elfin_reboot_interval;
                
                statusElement.textContent = isConnected ? '정상' : '응답 없음';
                statusElement.className = `px-2 py-1 rounded text-sm ${isConnected ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-100' : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-100'}`;
            })
            .catch(error => {
                console.error('EW11 상태 업데이트 실패:', error);
                const statusElement = document.getElementById('ew11ConnectionStatus');
                statusElement.textContent = '상태 확인 실패';
                statusElement.className = 'px-2 py-1 rounded text-sm bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-100';
            });
    }
    
    updateMqttStatus() {
        fetch('./api/mqtt_status')
            .then(response => response.json())
            .then(data => {
                const statusElement = document.getElementById('connectionStatus');
                statusElement.textContent = data.connected ? '연결됨' : '연결 끊김';
                statusElement.className = data.connected ? 
                    'px-2 py-1 rounded text-sm bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-100' : 
                    'px-2 py-1 rounded text-sm bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-100';
                
                document.getElementById('brokerInfo').textContent = data.broker || '-';
                document.getElementById('clientId').textContent = data.client_id || '-';
                
                // 구독 중인 토픽 표시
                const topicsContainer = document.getElementById('subscribedTopicsWithMessages');
                if (!data.subscribed_topics || data.subscribed_topics.length === 0) {
                    topicsContainer.innerHTML = `
                        <div class="text-center text-gray-500 py-4">
                            <p>구독 중인 채널이 없습니다.</p>
                        </div>
                    `;
                    return;
                }
                // 기존에 없는 토픽에 대한 div 추가
                data.subscribed_topics.forEach(topic => {
                    // 특수문자를 안전하게 처리하도록 수정
                    const topicId = `topic-${topic.replace(/[^a-zA-Z0-9]/g, function(match) {
                        // '/'와 '+' 문자를 각각 다르게 처리
                        if (match === '/') return '-';
                        if (match === '+') return 'plus';
                        return '';
                    })}`;
                    
                    // 기존 div가 없는 경우에만 새로 생성
                    if (!document.getElementById(topicId)) {
                        const topicDiv = document.createElement('div');
                        topicDiv.id = topicId;
                        topicDiv.className = 'bg-gray-50 dark:bg-gray-800 p-2 rounded mb-1';
                        topicDiv.innerHTML = `
                            <div class="flex justify-between items-center">
                                <div class="flex flex-col gap-1">
                                    <div class="font-medium text-gray-700 dark:text-gray-300">${topic}</div>
                                    <pre class="text-xs text-gray-600 dark:text-gray-400 whitespace-pre-wrap break-all">-</pre>
                                </div>
                                <span class="text-xs text-gray-500 dark:text-gray-400">-</span>
                            </div>
                        `;
                        topicsContainer.appendChild(topicDiv);
                    } else {
                        // 기존 div가 있는 경우 토픽 이름만 업데이트
                        const existingDiv = document.getElementById(topicId);
                        const topicSpan = existingDiv.querySelector('.font-medium');
                        if (topicSpan) {
                            topicSpan.textContent = topic;
                        }
                    }
                });
    
                // 더 이상 구독하지 않는 토픽의 div 제거
                const existingTopicDivs = topicsContainer.querySelectorAll('[id^="topic-"]');
                existingTopicDivs.forEach(div => {
                    // ID를 토픽으로 변환할 때도 동일한 규칙 적용
                    const topicFromId = div.id.replace('topic-', '')
                        .replace(/-/g, '/')
                        .replace(/plus/g, '+');
                    if (!data.subscribed_topics.includes(topicFromId)) {
                        div.remove();
                    }
                });
            });
    }

    updateRecentMessages() {
        fetch('./api/recent_messages')
            .then(response => response.json())
            .then(data => {
                if (!data.messages) return;
    
                // 각 토픽의 div 업데이트
                Object.entries(data.messages).forEach(([topic, messageData]) => {
                    // 와일드카드 토픽 매칭을 위한 함수
                    function matchTopic(pattern, topic) {
                        const patternParts = pattern.split('/');
                        const topicParts = topic.split('/');
                        
                        if (patternParts.length !== topicParts.length) return false;
                        
                        return patternParts.every((part, i) => 
                            part === '+' || part === topicParts[i]
                        );
                    }
    
                    // 모든 구독 중인 토픽에 대해 매칭 확인
                    document.querySelectorAll('[id^="topic-"]').forEach(topicDiv => {
                        const subscribedTopic = topicDiv.id
                            .replace('topic-', '')
                            .replace(/-/g, '/')
                            .replace(/plus/g, '+');
                        
                        if (matchTopic(subscribedTopic, topic)) {
                            const timestamp = topicDiv.querySelector('span:last-child');
                            const payload = topicDiv.querySelector('pre');
                            if (timestamp && payload) {
                                timestamp.textContent = messageData.timestamp;
                                // 와일드카드(+)가 포함된 토픽인 경우 전체 토픽 정보 표시
                                if (subscribedTopic.includes('+')) {
                                    payload.textContent = `[${topic}] ${messageData.payload}`;
                                } else {
                                    payload.textContent = messageData.payload;
                                }
                            }
                        }
                    });
                });
            });
    }
}

// ===============================
// 기기 목록 관련 클래스
// ===============================
class DeviceManager {
    constructor() {
        this.bindEvents();
    }

    bindEvents() {
        // 기기 새로고침 버튼 이벤트 바인딩
        const refreshButton = document.getElementById('refreshDevicesButton');
        if (refreshButton) {
            refreshButton.addEventListener('click', () => this.refreshDevices());
        }
    }

    refreshDevices() {
        if (!confirm('기기를 다시 검색하기 위해 애드온을 재시작합니다. 재시작 후 30초정도 후에 기기가 검색됩니다. 계속하시겠습니까?')) {
            return;
        }
    
        fetch('./api/find_devices', {
            method: 'POST'
        });
    }
    
    updateDeviceList() {
        fetch('./api/devices')
            .then(response => response.json())
            .then(data => {
                const deviceListDiv = document.getElementById('deviceList');
                if (!deviceListDiv) return;
    
                let html = '';
                for (const [deviceName, info] of Object.entries(data)) {
                    if (info.count > 0) {
                        html += `
                            <div class="mb-2 p-3 bg-gray-50 dark:bg-gray-800 rounded">
                                <div class="flex justify-between">
                                    <h3 class="dark:text-gray-300">${deviceName}</h3>
                                    <span class="text-sm text-gray-500">${info.type}</span>
                                </div>
                                <div class="text-sm text-gray-600">개수: ${info.count}개</div>
                            </div>
                        `;
                    }
                }
                deviceListDiv.innerHTML = html || '<p class="text-gray-500 dark:text-gray-400">연결된 기기가 없습니다. 기기를 검색하는 중일 수도 있습니다.</p>';
            })
            .catch(error => console.error('기기 목록 업데이트 실패:', error));
    }
}
