// ===============================
// 패킷 참조자료 관련 클래스
// ===============================
class PacketReference {
    constructor() {
    }

    createTable(deviceData) {
        const table = document.createElement('table');
        table.className = 'min-w-full divide-y divide-gray-200 dark:divide-gray-700';
        
        const headerRow = document.createElement('tr');
        const headers = ['Byte', ...Object.values(PACKET_TYPES)];
        headers.forEach(header => {
            const th = document.createElement('th');
            th.className = 'px-4 py-2 bg-gray-50 dark:bg-gray-800 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider';
            th.textContent = header;
            headerRow.appendChild(th);
        });
        table.appendChild(headerRow);
        
        for (let byte = 0; byte < 8; byte++) {
            const row = document.createElement('tr');
            row.className = byte % 2 === 0 ? 'bg-white dark:bg-gray-900' : 'bg-gray-50 dark:bg-gray-800';
            
            const byteCell = document.createElement('td');
            byteCell.className = 'px-4 py-2 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100';
            byteCell.textContent = `Byte ${byte}`;
            row.appendChild(byteCell);
            
            Object.keys(PACKET_TYPES).forEach(type => {
                const td = document.createElement('td');
                td.className = 'px-4 py-2 text-sm text-gray-500 dark:text-gray-400';
                
                if (deviceData[type]) {
                    this.appendCellContent(td, deviceData[type], byte);
                }
                
                row.appendChild(td);
            });
            
            table.appendChild(row);
        }
        
        return table;
    }

    appendCellContent(td, typeData, byte) {
        td.className = 'px-4 py-2 text-sm text-gray-500 dark:text-gray-400';
        
        if (typeData.byte_desc && typeData.byte_desc[byte] !== undefined) {
            const descDiv = document.createElement('div');
            descDiv.className = 'font-medium text-gray-900 dark:text-gray-100 mb-2';
            descDiv.textContent = typeData.byte_desc[byte];
            td.appendChild(descDiv);
        }
        
        if (typeData.byte_values && typeData.byte_values[byte]) {
            const valuesDiv = document.createElement('div');
            valuesDiv.className = 'space-y-1';
            Object.entries(typeData.byte_values[byte]).forEach(([key, value]) => {
                const valueSpan = document.createElement('div');
                valueSpan.className = 'text-sm text-gray-600 dark:text-gray-300';
                valueSpan.textContent = `${key}: ${value}`;
                valuesDiv.appendChild(valueSpan);
            });
            td.appendChild(valuesDiv);
        }
        
        if (typeData.byte_memos && typeData.byte_memos[byte]) {
            const memoDiv = document.createElement('div');
            memoDiv.className = 'mt-2 text-sm text-gray-500 dark:text-gray-400 italic whitespace-pre-wrap break-words';
            memoDiv.textContent = `💡 ${typeData.byte_memos[byte]}`;
            td.appendChild(memoDiv);
        }
    }

    update(data) {
        const tabContents = document.getElementById('referenceTabContents');
        if (!tabContents) return;
        
        tabContents.innerHTML = '';
        Object.entries(data).forEach(([deviceName, deviceData], index) => {
            const deviceSection = document.createElement('div');
            deviceSection.id = `device-${deviceName}`;
            deviceSection.className = `reference-tab-content ${index !== 0 ? 'hidden' : ''}`;
            
            const table = this.createTable(deviceData);
            deviceSection.appendChild(table);
            
            tabContents.appendChild(deviceSection);
        });
    }

    openTab(evt, deviceName) {
        const tabcontents = document.getElementsByClassName("reference-tab-content");
        for (let content of tabcontents) {
            content.classList.add('hidden');
        }

        const tabButtons = document.getElementById('deviceTabs').getElementsByClassName('reference-button');
        for (let button of tabButtons) {
            button.className = button.className
                .replace('border-blue-500 text-blue-600', 'border-transparent text-gray-500')
                .replace('hover:text-gray-700 hover:border-gray-300', '');
            
            if (button.getAttribute('reference-data-tab') !== deviceName) {
                button.className += ' hover:text-gray-700 hover:border-gray-300';
            }
        }
        
        const selectedTab = document.getElementById(`device-${deviceName}`);
        if (selectedTab) {
            selectedTab.classList.remove('hidden');
        }
        evt.currentTarget.className = evt.currentTarget.className
            .replace('border-transparent text-gray-500', 'border-blue-500 text-blue-600');
    }

    loadReferencePacketStructures() {
        fetch('./api/packet_structures')
            .then(response => response.json())
            .then(structures => {
                const tabButtons = document.getElementById('deviceTabs');
                const tabContents = document.getElementById('referenceTabContents');
                if (!tabButtons || !tabContents) return;
                
                tabButtons.innerHTML = '';
                let isFirst = true;
                
                for (const [deviceName, deviceData] of Object.entries(structures)) {
                    const button = document.createElement('button');
                    button.className = `reference-button px-4 py-2 text-sm font-medium border-b-2 focus:outline-none transition-colors ${
                        isFirst ? 'border-blue-500 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                    }`;
                    button.setAttribute('reference-data-tab', deviceName);
                    button.onclick = (evt) => this.openTab(evt, deviceName);
                    button.textContent = deviceName;
                    tabButtons.appendChild(button);
                    isFirst = false;
                }
                
                this.update(structures);
            })
            .catch(error => {
                console.error('패킷 구조 로드 실패:', error);
                const tabContents = document.getElementById('referenceTabContents');
                if (tabContents) {
                    tabContents.innerHTML = `
                        <div class="text-red-500 p-4">
                            패킷 구조를 로드하는 중 오류가 발생했습니다.<br>
                            ${error.message}
                        </div>
                    `;
                }
            });
    }
}
