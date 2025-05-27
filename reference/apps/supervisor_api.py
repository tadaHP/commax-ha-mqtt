import os
import requests
from typing import Optional, Dict, Any, TypeVar, Generic
from enum import Enum
from dataclasses import dataclass

T = TypeVar('T')

@dataclass
class APIResult(Generic[T]):
    """API 응답 결과를 담는 클래스"""
    success: bool
    message: str
    data: Optional[T] = None

class SupervisorEndpoint(Enum):
    INFO = '/addons/self/info'
    OPTIONS = '/addons/self/options'
    RESTART = '/addons/self/restart'
    NOTIFICATION = '/core/api/services/notify/persistent_notification'

class SupervisorAPI:
    BASE_URL = 'http://supervisor'

    def __init__(self):
        self.supervisor_token = os.environ.get('SUPERVISOR_TOKEN')
        
    def _make_request(self, method: str, endpoint: SupervisorEndpoint, data: Optional[Dict] = None) -> APIResult:
        """공통 API 요청 처리 메서드"""
        if not self.supervisor_token:
            return APIResult(success=False, message="Supervisor 토큰이 설정되지 않았습니다.")
            
        headers = {
            'Authorization': f'Bearer {self.supervisor_token}',
            'Content-Type': 'application/json'
        }
        
        try:
            url = f"{self.BASE_URL}{endpoint.value}"
            response = requests.request(
                method=method,
                url=url,
                headers=headers,
                json=data
            )
            response.raise_for_status()
            
            if response.content:
                if method == 'GET':
                    return APIResult(
                        success=True,
                        message="데이터를 성공적으로 가져왔습니다.",
                        data=response.json().get('data')
                    )
                return APIResult(
                    success=True,
                    message="요청이 성공적으로 처리되었습니다."
                )
            return APIResult(
                success=response.status_code == 200,
                message="요청이 성공적으로 처리되었습니다." if response.status_code == 200 else "요청 처리 중 오류가 발생했습니다."
            )
            
        except requests.exceptions.RequestException as e:
            error_message = f"API 요청 중 오류 발생 ({endpoint.value}): {str(e)}"
            print(error_message)
            return APIResult(success=False, message=error_message)
        except Exception as e:
            error_message = f"예상치 못한 오류 발생: {str(e)}"
            print(error_message)
            return APIResult(success=False, message=error_message)
    
    def get_addon_info(self) -> APIResult[Dict[str, Any]]:
        """애드온 정보 조회"""
        return self._make_request('GET', SupervisorEndpoint.INFO)
            
    def update_addon_options(self, options: Dict[str, Any]) -> APIResult:
        """애드온 옵션 업데이트"""
        return self._make_request('POST', SupervisorEndpoint.OPTIONS, {'options': options})
            
    def restart_addon(self) -> APIResult:
        """애드온 재시작"""
        return self._make_request('POST', SupervisorEndpoint.RESTART)
            
    def send_notification(self, title: str, message: str) -> APIResult:
        """Home Assistant에 persistent notification 전송
        
        Args:
            title (str): 알림 제목
            message (str): 알림 내용
            
        Returns:
            APIResult: API 호출 결과
        """
        notification_data = {
            "title": title,
            "message": message
        }
        return self._make_request('POST', SupervisorEndpoint.NOTIFICATION, notification_data) 