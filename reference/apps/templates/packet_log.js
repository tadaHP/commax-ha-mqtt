// ===============================
// 패킷 로그 관련 클래스
// ===============================
class PacketLogger {
    constructor() {
        this.lastPackets = new Set();
        this.isPaused = false;
        this.isPolling = false;
        this.pollingInterval = null;
        this.packetLogInterval = null;
        this.pauseButton = document.getElementById('pauseButton');

        this.bindEvents();
    }

    bindEvents() {
        // 패킷 로그 초기화 버튼 이벤트 리스너
        const clearButton = document.getElementById('packetLogClearButton');
        if (clearButton) {
            clearButton.addEventListener('click', () => this.clearPacketLog());
        }
        // 일시정지 버튼 이벤트 리스너
        if (this.pauseButton) {
            this.pauseButton.addEventListener('click', () => {
                this.togglePause();
                this.pauseButton.textContent = this.isPaused ? '재개' : '일시정지';
                this.pauseButton.classList.toggle('bg-blue-500');
                this.pauseButton.classList.toggle('bg-green-500');
            });
        }
    }

    updatePacketDisplay() {
        const elements = document.getElementsByClassName('unknown-packet');
        const hideUnknown = document.getElementById('hideUnknown');
        if (!(hideUnknown instanceof HTMLInputElement)) return;
        const displayStyle = hideUnknown.checked ? 'none' : '';
        
        Array.from(elements).forEach(el => {
            if (el instanceof HTMLElement) {
                el.style.display = displayStyle;
            }
        });
    }

    createPacketLogEntry(packet, type) {
        const deviceInfo = packet.results;
        const deviceClass = deviceInfo.device === 'Unknown' ? 'unknown-packet' : '';
        const formattedPacket = packet.packet.match(/.{2}/g).join(' ');
        
        return `
            <div class="packet-log-entry ${deviceClass} p-2 border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800 cursor-pointer" onclick="packetLogger.handlePacketClick('${packet.packet}')">
                <span class="inline-block min-w-[50px] mr-2 text-sm font-semibold ${type === 'send' ? 'text-green-600 dark:text-green-400' : 'text-blue-600 dark:text-blue-400'}">[${type.toUpperCase()}]</span>
                <span class="font-mono dark:text-gray-300">${formattedPacket}</span>
                <span class="inline-block min-w-[120px] ml-2 text-sm text-gray-600 dark:text-gray-400">[${deviceInfo.device} - ${deviceInfo.packet_type}]</span>
            </div>`;
    }

    updatePacketLog() {
        fetch('./api/packet_logs')
            .then(response => response.json())
            .then(data => {
                const logDiv = document.getElementById('packetLog');
                const lastPacketSet = this.lastPackets;
                let newContent = '';
                
                // 송신 및 수신 패킷 처리
                ['send', 'recv'].forEach(type => {
                    data[type].forEach(packet => {
                        const packetKey = `${type}:${packet.packet}:${packet.results.device}:${packet.results.packet_type}`
                        if(!lastPacketSet.has(packetKey)){
                            lastPacketSet.add(packetKey)
                        }
                    });
                });
                let packetArray = Array.from(lastPacketSet).sort()
                for (const key of packetArray) {
                    const [_type, _packet, _device,_packet_type] = key.split(':');
                    newContent += this.createPacketLogEntry({
                        packet: _packet,
                        results: {
                            device:_device,
                            packet_type:_packet_type
                        }
                    }, _type);
                }
                if (newContent) {
                    logDiv.innerHTML = newContent;
                    this.updatePacketDisplay();
                }
            })
            .catch(error => console.error('패킷 로그 업데이트 실패:', error));
    }

    handlePacketClick(packet) {
        const packetInput = document.getElementById('analyzerPacketInput');
        if (!(packetInput instanceof HTMLInputElement)) return;
        packetInput.value = packet;
        packetAnalyzer.analyzePacket();
    }

    clearPacketLog() {
        const logDiv = document.getElementById('packetLog');
        logDiv.innerHTML = '';
        this.lastPackets.clear();
    }

    startPolling() {
        if (this.isPolling) return;
        
        this.isPolling = true;
        
        // 500ms마다 데이터 요청
        this.pollingInterval = setInterval(() => this.fetchPacketData(), 500);
    }

    stopPolling() {
        if (!this.isPolling) return;
        
        this.isPolling = false;
        
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
    }

    async fetchPacketData() {
        if (this.isPaused) return;
        
        try {
            const response = await fetch('./api/live_packets');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const data = await response.json();
            
            // 패킷 데이터 업데이트
            this.updateLivePacketDisplay(data);
        } catch (error) {
            console.error('패킷 데이터 요청 실패:', error);
        }
    }

    updateLivePacketDisplay(data) {
        const sendDataElement = document.getElementById('send-data');
        const recvDataElement = document.getElementById('recv-data');
        
        if (sendDataElement && data.send_data) {
            sendDataElement.textContent = data.send_data.join('\n');
        }
        if (recvDataElement && data.recv_data) {
            recvDataElement.textContent = data.recv_data.join('\n');
        }
    }

    togglePause() {
        this.isPaused = !this.isPaused;
        if (this.pauseButton) {
            this.pauseButton.textContent = this.isPaused ? '재개' : '일시정지';
        }
    }

    startPacketLogUpdate() {
        this.packetLogInterval = setInterval(() => this.updatePacketLog(), 1000);
    }

    stopPacketLogUpdate() {
        if (this.packetLogInterval) {
            clearInterval(this.packetLogInterval);
            this.packetLogInterval = null;
        }
    }
}
