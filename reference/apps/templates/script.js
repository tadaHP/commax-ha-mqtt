const PACKET_TYPES = {
    'command': '명령 패킷',
    'state': '상태 패킷',
    'state_request': '상태 요청 패킷',
    'ack': '응답 패킷'
};
// ===============================
// 페이지 전환 함수
// ===============================
function showPage(pageId) {
    // 모든 페이지 숨기기
    document.querySelectorAll('.page').forEach(page => {
        page.classList.add('hidden');
    });
    
    // 선택된 페이지 보이기
    document.getElementById(pageId).classList.remove('hidden');
    
    // 네비게이션 메뉴 활성화 상태 변경
    document.querySelectorAll('nav a').forEach(link => {
        if (link.getAttribute('onclick').includes(pageId)) {
            link.classList.add('border-indigo-500', 'text-gray-900', 'dark:text-white');
            link.classList.remove('border-transparent', 'text-gray-500', 'dark:text-gray-400');
        } else {
            link.classList.remove('border-indigo-500', 'text-gray-900', 'dark:text-white');
            link.classList.add('border-transparent', 'text-gray-500', 'dark:text-gray-400');
        }
    });

    // 실시간 패킷 페이지인 경우 폴링 시작
    if (pageId === 'live_packets') {
        packetLogger.startPolling();
    } else {
        packetLogger.stopPolling();
    }
    if (pageId === 'playground') {
        packetLogger.startPacketLogUpdate();
    } else {
        packetLogger.stopPacketLogUpdate();
    }
}
// 모바일 메뉴 토글 함수
function toggleMobileMenu() {
    const mobileMenu = document.getElementById('mobileMenu');
    if (mobileMenu.classList.contains('hidden')) {
        mobileMenu.classList.remove('hidden');
    } else {
        mobileMenu.classList.add('hidden');
    }
}


// 패킷 히스토리 인스턴스 생성
const packetHistory = new PacketHistory();

// 패킷 분석기 인스턴스 생성
const packetAnalyzer = new PacketAnalyzer();

// 패킷 로그 인스턴스 생성
const packetLogger = new PacketLogger();

// ===============================
// 초기화 및 상태 업데이트 함수들
// ===============================

document.addEventListener('DOMContentLoaded', function() {
    // 대시보드 초기화
    const dashboard = new Dashboard();
        
    // 초기 상태 업데이트
    dashboard.deviceManager.updateDeviceList();
    dashboard.updateMqttStatus();
    dashboard.updateEW11Status();
    
    // 패킷 참조자료 초기화
    const packetReference = new PacketReference();
    packetReference.loadReferencePacketStructures();

    // 패킷 구조 에디터 초기화
    window.packetEditor = PacketStructureEditor.initialize();
});
