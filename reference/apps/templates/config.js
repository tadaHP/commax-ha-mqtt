class ConfigManager {
    constructor() {
        this.configForm = document.getElementById('configForm');
        this.messageElement = document.getElementById('configMessage');
        this.bindEvents();
    }

    bindEvents() {
        document.addEventListener('DOMContentLoaded', () => {
            const saveButton = document.getElementById('saveConfig');
            if (saveButton) {
                saveButton.addEventListener('click', () => this.saveConfig());
            }
        });
    }

    // 기존 loadConfig 함수를 메서드로 변환
    async loadConfig() {
        try {
            const response = await fetch('./api/config');
            const data = await response.json();
            
            if (data.error) {
                this.showConfigMessage('설정을 불러오는 중 오류가 발생했습니다: ' + data.error, true);
                return;
            }

            this.configForm.innerHTML = '';
            
            // 설정을 2컬럼으로 표시하기 위한 컨테이너 생성
            const configContainer = document.createElement('div');
            configContainer.className = 'grid grid-cols-1 md:grid-cols-2 gap-4';
            this.configForm.appendChild(configContainer);

            // 스키마 기반으로 설정 UI 생성
            data.schema.forEach(schemaItem => {
                const value = data.config[schemaItem.name];
                configContainer.appendChild(this.createConfigField(schemaItem, value));
            });
        } catch (error) {
            this.showConfigMessage('설정을 불러오는 중 오류가 발생했습니다.', true);
        }
    }

    createConfigField(schemaItem, value) {
        const fieldDiv = document.createElement('div');
        fieldDiv.className = 'border-b border-gray-700 dark:border-gray-600 py-2';

        // 스키마 타입이 schema인 경우 하위 설정 처리
        if (schemaItem.type === 'schema') {
            fieldDiv.innerHTML = `
                <div class="mb-2">
                    <label class="text-sm font-medium text-gray-700 dark:text-gray-300 text-left block">${schemaItem.name}</label>
                </div>
                <div class="pl-3 space-y-1">
                    ${schemaItem.schema.map(subSchema => {
                        const subValue = value ? value[subSchema.name] : '';
                        return this.createSchemaSubField(subSchema, subValue, schemaItem.name);
                    }).join('')}
                </div>
            `;
            return fieldDiv;
        }

        // 기본 필드 생성
        const labelContainer = this.createLabelContainer(schemaItem);
        fieldDiv.appendChild(labelContainer);

        const input = this.createInputBasedOnSchema(schemaItem, value);
        labelContainer.appendChild(input);

        return fieldDiv;
    }

    createSchemaSubField(subSchema, value, parentKey) {
        let inputHtml = '';
        const isRequired = subSchema.required ? '*' : '';
        const fieldId = `${parentKey}-${subSchema.name}`;

        switch (subSchema.type) {
            case 'boolean':
                inputHtml = `
                    <select class="form-input block rounded-md border-gray-700 w-1/2 dark:border-gray-600 dark:bg-gray-700 dark:text-white text-sm px-2 py-1"
                            id="${fieldId}"
                            data-key="${parentKey}"
                            data-subkey="${subSchema.name}"
                            data-type="bool"
                            ${subSchema.required ? 'required' : ''}>
                        <option value="true" ${value === true ? 'selected' : ''}>예 (true)</option>
                        <option value="false" ${value === false ? 'selected' : ''}>아니오 (false)</option>
                    </select>`;
                break;
            case 'integer':
                inputHtml = `
                    <input type="number"
                           class="form-input block rounded-md border-gray-700 w-1/2 dark:border-gray-600 dark:bg-gray-700 dark:text-white text-sm px-2 py-1"
                           id="${fieldId}"
                           value="${value || ''}"
                           data-key="${parentKey}"
                           data-subkey="${subSchema.name}"
                           data-type="int"
                           ${subSchema.lengthMin !== undefined ? `min="${subSchema.lengthMin}"` : ''}
                           ${subSchema.lengthMax !== undefined ? `max="${subSchema.lengthMax}"` : ''}
                           ${subSchema.required ? 'required' : ''}>`;
                break;
            case 'float':
                inputHtml = `
                    <input type="number"
                           step="0.01"
                           class="form-input block rounded-md border-gray-700 w-1/2 dark:border-gray-600 dark:bg-gray-700 dark:text-white text-sm px-2 py-1"
                           id="${fieldId}"
                           value="${value || ''}"
                           data-key="${parentKey}"
                           data-subkey="${subSchema.name}"
                           data-type="float"
                           ${subSchema.lengthMin !== undefined ? `min="${subSchema.lengthMin}"` : ''}
                           ${subSchema.lengthMax !== undefined ? `max="${subSchema.lengthMax}"` : ''}
                           ${subSchema.required ? 'required' : ''}>`;
                break;
            default:
                inputHtml = `
                    <input type="text"
                           class="form-input block rounded-md border-gray-700 w-1/2 dark:border-gray-600 dark:bg-gray-700 dark:text-white text-sm px-2 py-1"
                           id="${fieldId}"
                           value="${value || ''}"
                           data-key="${parentKey}"
                           data-subkey="${subSchema.name}"
                           data-type="string"
                           ${subSchema.required ? 'required' : ''}>`;
        }

        return `
            <div class="flex items-start gap-2">
                <label class="text-sm text-gray-600 dark:text-gray-400 w-1/2 text-left pt-1 break-words" for="${fieldId}">
                    ${subSchema.name}${isRequired}:
                </label>
                ${inputHtml}
            </div>`;
    }

    createLabelContainer(schemaItem) {
        const labelContainer = document.createElement('div');
        labelContainer.className = 'flex items-start gap-2 mb-1';

        const label = document.createElement('label');
        label.className = 'text-sm w-1/2 font-medium text-gray-700 dark:text-gray-300 text-left';
        label.textContent = schemaItem.name;

        if (schemaItem.required) {
            label.textContent += ' *';
        }

        labelContainer.appendChild(label);

        return labelContainer;
    }

    createInputBasedOnSchema(schemaItem, value) {
        const input = document.createElement(schemaItem.type === 'select' ? 'select' : 'input');
        input.className = 'form-input block w-1/2 rounded-md border-gray-700 dark:border-gray-600 dark:bg-gray-700 dark:text-white text-sm px-2 py-1';
        input.id = `config-${schemaItem.name}`;
        input.dataset.key = schemaItem.name;
        input.dataset.schemaType = schemaItem.type;

        if (schemaItem.required) {
            input.required = true;
        }

        if (schemaItem.type === 'select') {
            schemaItem.options.forEach(option => {
                const optionElement = document.createElement('option');
                optionElement.value = option;
                optionElement.textContent = option;
                optionElement.selected = option === value;
                input.appendChild(optionElement);
            });
        } else {
            const inputType = schemaItem.type === 'string' ? 'text' : 
                            schemaItem.type === 'integer' || schemaItem.type === 'float' ? 'number' : 
                            'text';
            input.setAttribute('type', inputType);
            if (schemaItem.type === 'float') {
                input.setAttribute('step', '0.01');
            }
            input.value = value || '';
        }

        return input;
    }

    async saveConfig() {
        if (!confirm('설정을 저장하면 애드온이 재시작됩니다. 계속하시겠습니까?')) {
            return;
        }

        const configData = this.collectConfigData();
        this.showConfigMessage('설정을 저장하고 애드온을 재시작하는 중...', false);

        try {
            const response = await fetch('./api/config', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(configData)
            });
            
            const data = await response.json();
            
            if (!data.success) {
                if (data.error === '유효성 검사 실패' && data.details) {
                    const errorMessage = ['유효성 검사 실패:'].concat(data.details).join('\n');
                    this.showConfigMessage(errorMessage, true);
                    throw new Error('validation_failed');
                } else {
                    this.showConfigMessage(data.error || '설정 저장 실패', true);
                    throw new Error('save_failed');
                }
            }
        } catch (error) {
            if (error.message !== 'validation_failed' && error.message !== 'save_failed') {
                console.log('애드온이 재시작되는 중입니다...');
                setTimeout(() => {
                    window.location.reload();
                }, 10000);
            } else {
                console.error('설정 저장 실패:', error);
            }
        }
    }

    collectConfigData() {
        const configData = {};
        const inputs = this.configForm.querySelectorAll('input, select');
        
        inputs.forEach(input => {
            const key = input.getAttribute('data-key');
            const subKey = input.getAttribute('data-subkey');
            const schemaType = input.getAttribute('data-type');
            
            let value = this.parseInputValue(input, schemaType);
            
            if (subKey) {
                if (!configData[key]) {
                    configData[key] = {};
                }
                configData[key][subKey] = value;
            } else {
                configData[key] = value;
            }
        });
        
        return configData;
    }

    parseInputValue(input, schemaType) {
        switch(schemaType) {
            case 'bool':
                return input.value === 'true';
            case 'int':
                return parseInt(input.value);
            case 'float':
                return parseFloat(input.value);
            default:
                return input.value;
        }
    }

    showConfigMessage(message, isError) {
        this.messageElement.innerHTML = message.replace(/\n/g, '<br>');
        this.messageElement.className = `text-sm ${isError ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'} whitespace-pre-line`;
    }
}

// 인스턴스 생성 및 초기화
const configManager = new ConfigManager();
configManager.loadConfig();
