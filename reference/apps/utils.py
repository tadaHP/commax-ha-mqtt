"""유틸리티 함수들을 모아둔 모듈입니다."""

def byte_to_hex_str(byte_val: int) -> str:
    """바이트를 16진수 문자열로 변환하는 유틸리티 함수
    
    Args:
        byte_val (int): 바이트 값 (예: 0x82)
        
    Returns:
        str: 16진수 문자열 (예: "82")
    """
    return format(byte_val, '02X').upper()

def checksum(input_hex: str) -> str | None:
    """
    input_hex에 checksum을 붙여주는 함수
    
    Args:
        input_hex (str): 기본 16진수 명령어 문자열
    
    Returns:
        str | None: 체크섬이 포함된 수정된 16진수 명령어. 실패시 None 반환
    """
    try:
        input_hex = input_hex[:14]
        s1 = sum([int(input_hex[val], 16) for val in range(0, 14, 2)])
        s2 = sum([int(input_hex[val + 1], 16) for val in range(0, 14, 2)])
        s1 = s1 + int(s2 // 16)
        s1 = s1 % 16
        s2 = s2 % 16
        return input_hex + format(s1, 'X') + format(s2, 'X')
    except:
        return None

# def pad(value: int | str) -> str:
#     """한 자리 숫자를 두 자리로 패딩하는 함수
    
#     Args:
#         value (int | str): 패딩할 값
        
#     Returns:
#         str: 패딩된 문자열
#     """
#     value = int(value)
#     return '0' + str(value) if value < 10 else str(value) 