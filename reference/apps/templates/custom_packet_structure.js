class PacketStructureEditor {
    constructor() {
        this.PACKET_TYPES = {
            'command': '명령 패킷',
            'state': '상태 패킷',
            'state_request': '상태 요청 패킷',
            'ack': '응답 패킷'
        };
        
        this.editorDiv = document.getElementById('customPacketTabContents');
        this.messageElement = document.getElementById('packetEditorMessage');
        this.tabButtons = document.getElementById('customPacketDeviceTabs');
        this.tabContents = document.getElementById('customPacketTabContents');
        
        this.bindEvents();
    }

    bindEvents() {
        document.getElementById('savePacketStructure')?.addEventListener('click', () => this.saveCustomPacketStructure());
        document.getElementById('resetPacketStructure')?.addEventListener('click', () => this.resetPacketStructure());
        document.getElementById('changeVendorButton')?.addEventListener('click', () => this.changeVendorToCustom());
    }

    openTab(evt, deviceName) {
        const tabcontents = document.getElementsByClassName("custom-tab-content");
        for (let content of tabcontents) {
            content.classList.add('hidden');
        }

        const tabButtons = this.tabButtons.getElementsByClassName('custum-buttons');
        for (let button of tabButtons) {
            button.className = button.className
                .replace('border-blue-500 text-blue-600', 'border-transparent text-gray-500')
                .replace('hover:text-gray-700 hover:border-gray-300', '');
            
            if (button.getAttribute('custom-data-tab') !== deviceName) {
                button.className += ' hover:text-gray-700 hover:border-gray-300';
            }
        }
        
        const selectedTab = document.getElementById(`custom-device-${deviceName}`);
        if (selectedTab) {
            selectedTab.classList.remove('hidden');
        }
        evt.currentTarget.className = evt.currentTarget.className
            .replace('border-transparent text-gray-500', 'border-blue-500 text-blue-600');
    }

    checkVendorSetting() {
        fetch('./api/config')
            .then(response => response.json())
            .then(data => {
                const vendorWarning = document.getElementById('vendorWarning');
                if (data.config && data.config.vendor === 'commax') {
                    vendorWarning.classList.remove('hidden');
                } else {
                    vendorWarning.classList.add('hidden');
                }
            });
    }

    changeVendorToCustom() {
        if (!confirm('vendor 설정을 변경하면 애드온이 재시작됩니다. 계속하시겠습니까?')) {
            return;
        }
        fetch('./api/config')
            .then(response => response.json())
            .then(data => {
                const configData = data.config || {};
                configData.vendor = 'custom';
                return configData;
            })
            .then(configData => {
                this.showPacketEditorMessage('vendor 설정을 변경하고 애드온을 재시작하는 중...', false);
                fetch('./api/config', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(configData)
                })
                setTimeout(() => {
                    window.location.reload();
                }, 3000);
            });
    }

    loadCustomPacketStructure() {
        fetch('./api/custom_packet_structure/editable')
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    this.renderPacketStructureEditor(data.content);
                } else {
                    this.showPacketEditorMessage(data.error, true);
                }
            })
            .catch(error => this.showPacketEditorMessage('패킷 구조를 불러오는 중 오류가 발생했습니다: ' + error, true));
    }

    showPacketEditorMessage(message, isError) {
        if (this.messageElement) {
            this.messageElement.textContent = message;
            this.messageElement.className = `fixed bottom-4 right-4 p-4 rounded-lg shadow-lg ${isError ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`;
            this.messageElement.classList.remove('hidden');
            setTimeout(() => {
                this.messageElement.classList.add('hidden');
            }, 15000);
        } else {
            console.error('메시지 표시 요소를 찾을 수 없습니다:', message);
        }
    }

    renderPacketStructureEditor(structure) {
        if (!this.tabButtons || !this.tabContents) return;

        // 탭 버튼 초기화
        this.tabButtons.innerHTML = '';
        this.tabContents.innerHTML = '';
        
        let isFirst = true;
        
        for (const [deviceName, deviceData] of Object.entries(structure)) {
            // 탭 버튼 생성
            const button = document.createElement('button');
            button.className = `custum-buttons px-4 py-2 text-sm font-medium border-b-2 focus:outline-none transition-colors ${
                isFirst ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`;
            button.setAttribute('custom-data-tab', deviceName);
            button.onclick = (evt) => this.openTab(evt, deviceName);
            button.textContent = deviceName;
            this.tabButtons.appendChild(button);

            // 탭 컨텐츠 생성
            const deviceSection = document.createElement('div');
            deviceSection.id = `custom-device-${deviceName}`;
            deviceSection.className = `custom-tab-content ${isFirst ? '' : 'hidden'}`;
            
            const deviceContent = document.createElement('div');
            deviceContent.className = 'border border-gray-700 dark:bg-gray-800 rounded-lg p-4 mb-4';
            
            deviceContent.innerHTML = `
                <div class="flex justify-between items-center mb-4">
                    <h3 class="text-lg font-medium dark:text-white">${deviceName}</h3>
                    <input type="text" value="${deviceData.type}" 
                        class="border border-gray-700 dark:bg-gray-700 dark:text-white rounded px-2 py-1 text-sm"
                        data-device="${deviceName}" data-field="type">
                </div>
            `;

            const packetContainer = document.createElement('div');
            packetContainer.className = 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4';

            Object.entries(this.PACKET_TYPES).forEach(([type, title]) => {
                if (deviceData[type]) {
                    const packetSection = this.createPacketSection(deviceName, type, deviceData[type], title);
                    packetContainer.appendChild(packetSection);
                }
            });

            deviceContent.appendChild(packetContainer);
            deviceSection.appendChild(deviceContent);
            this.tabContents.appendChild(deviceSection);
            
            isFirst = false;
        }
    }

    createPacketSection(deviceName, packetType, packetData, title) {
        const section = document.createElement('div');
        section.className = 'w-full';

        section.innerHTML = `
            <div class="bg-gray-50 dark:bg-gray-700 p-3 rounded-lg">
                <h4 class="font-medium mb-2 dark:text-white">${title}</h4>
                <div class="space-y-2">
                    <div class="flex items-center">
                        <span class="w-20 text-sm dark:text-gray-300">Header:</span>
                        <input type="text" value="${packetData.header}" 
                            class="border border-gray-700 dark:bg-gray-600 dark:text-white rounded px-2 py-1 text-sm flex-1"
                            data-device="${deviceName}" 
                            data-packet-type="${packetType}" 
                            data-field="header">
                    </div>
                    ${Object.entries(packetData)
                        .filter(([key]) => !['header', 'structure'].includes(key))
                        .map(([key, value]) => `
                            <div class="flex items-center mt-2">
                                <span class="w-32 text-sm dark:text-gray-300">${key}:</span>
                                <input type="text" value="${value}" 
                                    class="border border-gray-700 dark:bg-gray-600 dark:text-white rounded px-2 py-1 text-sm flex-1"
                                    data-device="${deviceName}" 
                                    data-packet-type="${packetType}" 
                                    data-field="${key}">
                            </div>
                        `).join('')}
                </div>
            </div>
        `;

        if (packetData.structure) {
            const structureDiv = this.createStructureDiv(deviceName, packetType, packetData.structure);
            section.appendChild(structureDiv);
        }

        return section;
    }

    createStructureDiv(deviceName, packetType, structure) {
        const structureDiv = document.createElement('div');
        structureDiv.className = 'mt-2';
        
        Object.entries(structure).forEach(([position, field]) => {
            const fieldDiv = document.createElement('div');
            fieldDiv.className = 'border-l-2 border-gray-200 pl-2 py-2 mt-2';
            fieldDiv.innerHTML = this.createFieldHTML(deviceName, packetType, position, field);
            structureDiv.appendChild(fieldDiv);
        });
        
        return structureDiv;
    }

    createFieldHTML(deviceName, packetType, position, field) {
        return `
            <div class="text-sm font-medium dark:text-white">Position ${position}</div>
            <div class="space-y-1 mt-1">
                <div>
                    <label class="block text-xs text-gray-600 dark:text-gray-400">Name:</label>
                    <input type="text" value="${field.name}" 
                        class="border border-gray-700 dark:bg-gray-700 dark:text-white rounded px-2 py-1 text-sm w-full"
                        data-device="${deviceName}" 
                        data-packet-type="${packetType}" 
                        data-position="${position}"
                        data-field="name">
                </div>
                <div>
                    <label class="block text-xs text-gray-600 dark:text-gray-400">Values:</label>
                    <div class="space-y-1" id="values-${deviceName}-${packetType}-${position}">
                        ${this.createValuesHTML(deviceName, packetType, position, field.values)}
                        <button class="text-sm text-blue-500 hover:text-blue-400 dark:text-blue-400 dark:hover:text-blue-300" 
                            onclick="window.packetEditor.addValue('${deviceName}', '${packetType}', '${position}')">
                            + 값 추가
                        </button>
                    </div>
                </div>
            </div>
        `;
    }

    createValuesHTML(deviceName, packetType, position, values = {}) {
        return Object.entries(values).map(([key, value]) => `
            <div class="grid grid-cols-9 gap-1">
                <input type="text" value="${key}" 
                    class="col-span-4 border border-gray-700 dark:bg-gray-700 dark:text-white rounded px-2 py-1 text-sm"
                    placeholder="키"
                    data-device="${deviceName}" 
                    data-packet-type="${packetType}" 
                    data-position="${position}"
                    data-field="value-key">
                <input type="text" value="${value}" 
                    class="col-span-4 border border-gray-700 dark:bg-gray-700 dark:text-white rounded px-2 py-1 text-sm"
                    placeholder="값"
                    data-device="${deviceName}" 
                    data-packet-type="${packetType}" 
                    data-position="${position}"
                    data-field="value-value">
                <button class="text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300" onclick="window.packetEditor.removeValue(this)">×</button>
            </div>
        `).join('');
    }

    addValue(deviceName, packetType, position) {
        const valuesDiv = document.getElementById(`values-${deviceName}-${packetType}-${position}`);
        const newValueDiv = document.createElement('div');
        newValueDiv.className = 'grid grid-cols-9 gap-1';
        newValueDiv.innerHTML = `
            <input type="text" class="col-span-4 border rounded px-2 py-1 text-sm" 
                placeholder="키"
                data-device="${deviceName}" 
                data-packet-type="${packetType}" 
                data-position="${position}"
                data-field="value-key">
            <input type="text" class="col-span-4 border rounded px-2 py-1 text-sm" 
                placeholder="값"
                data-device="${deviceName}" 
                data-packet-type="${packetType}" 
                data-position="${position}"
                data-field="value-value">
            <button class="text-red-500 hover:text-red-700" onclick="window.packetEditor.removeValue(this)">×</button>
        `;
        valuesDiv.insertBefore(newValueDiv, valuesDiv.lastElementChild);
    }

    removeValue(button) {
        button.parentElement.remove();
    }

    saveCustomPacketStructure() {
        const structure = this.collectStructureData();
        
        fetch('./api/custom_packet_structure/editable', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ content: structure })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                this.showPacketEditorMessage('패킷 구조가 성공적으로 저장되었습니다.', false);
            } else {
                this.showPacketEditorMessage(data.error, true);
            }
        })
        .catch(error => this.showPacketEditorMessage('저장 중 오류가 발생했습니다: ' + error, true));
    }

    collectStructureData() {
        const structure = {};
        
        // 기본 구조 데이터 수집
        this.editorDiv.querySelectorAll('[data-device]').forEach((element) => {
            const deviceName = element.getAttribute('data-device');
            const packetType = element.getAttribute('data-packet-type');
            const position = element.getAttribute('data-position');
            const field = element.getAttribute('data-field');

            if (!structure[deviceName]) {
                structure[deviceName] = { type: '' };
            }

            if (field === 'type') {
                structure[deviceName].type = element instanceof HTMLInputElement ? element.value : '';
                return;
            }

            if (!packetType) return;

            if (!structure[deviceName][packetType]) {
                structure[deviceName][packetType] = {
                    header: '',
                    structure: {}
                };
            }

            if (field === 'header') {
                structure[deviceName][packetType].header = element instanceof HTMLInputElement ? element.value : '';
                return;
            }

            // 추가 설정 항목 처리
            if (field && !['header', 'type'].includes(field) && !position) {
                structure[deviceName][packetType][field] = element instanceof HTMLInputElement ? element.value : '';
                return;
            }

            if (position) {
                if (!structure[deviceName][packetType].structure[position]) {
                    structure[deviceName][packetType].structure[position] = {
                        name: '',
                        values: {}
                    };
                }

                if (field === 'name') {
                    structure[deviceName][packetType].structure[position].name = element instanceof HTMLInputElement ? element.value : '';
                }
            }
        });

        // values 데이터 수집
        this.collectValuesData(structure);

        return structure;
    }

    collectValuesData(structure) {
        this.editorDiv.querySelectorAll('[data-field^="value-"]').forEach((element) => {
            if (!(element instanceof HTMLInputElement)) return;
            
            const deviceName = element.getAttribute('data-device');
            const packetType = element.getAttribute('data-packet-type');
            const position = element.getAttribute('data-position');
            
            if (!element.value) return;

            const values = structure[deviceName][packetType].structure[position].values;
            const row = element.parentElement;
            const keyInput = row.querySelector('[data-field="value-key"]');
            const valueInput = row.querySelector('[data-field="value-value"]');
            
            if (keyInput instanceof HTMLInputElement && valueInput instanceof HTMLInputElement && keyInput.value && valueInput.value) {
                values[keyInput.value] = valueInput.value;
            }
        });
    }

    resetPacketStructure() {
        if (!confirm('패킷 구조를 초기화하면 모든 커스텀 설정이 삭제되고 commax기본값으로 돌아갑니다. 계속하시겠습니까?')) {
            return;
        }

        fetch('./api/custom_packet_structure', {
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                this.showPacketEditorMessage('패킷 구조가 초기화되었습니다. 애드온을 재시작합니다...', false);
                fetch('./api/find_devices', { method: 'POST' });
                setTimeout(() => {
                    window.location.reload();
                }, 3000);
            } else {
                this.showPacketEditorMessage(data.error || '초기화 중 오류가 발생했습니다.', true);
            }
        })
        .catch(error => {
            this.showPacketEditorMessage('초기화 중 오류가 발생했습니다: ' + error, true);
        });
    }

    static initialize() {
        const editor = new PacketStructureEditor();
        editor.checkVendorSetting();
        editor.loadCustomPacketStructure();
        return editor;
    }
}