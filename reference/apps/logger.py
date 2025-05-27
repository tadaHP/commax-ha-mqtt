import logging
import sys
import os
from logging.handlers import RotatingFileHandler
from queue import Queue
from logging.handlers import QueueHandler, QueueListener

class Logger:
    # def __init__(self, debug=False, elfin_log=False, mqtt_log=False, log_file='/share/commax_wallpad.log'):
    def __init__(self, debug=False, elfin_log=False, mqtt_log=False, log_file='./logs/commax_wallpad.log'):
        self.logger = logging.getLogger('ComMaxWallpad')
        if self.logger.handlers:  # 이미 핸들러가 있다면 제거
            self.logger.handlers.clear()
            
        level = logging.DEBUG if debug else logging.INFO
        self.logger.setLevel(level)

        formatter = logging.Formatter(
            '[%(asctime)s] [%(levelname)s] %(message)s',
            datefmt='%Y-%m-%d %p %I:%M:%S'
        )

        # 로그 디렉토리 없으면 생성
        log_dir = os.path.dirname(log_file)
        os.makedirs(log_dir, exist_ok=True)

        # 파일 핸들러 설정
        try:
            file_handler = RotatingFileHandler(
                log_file, 
                maxBytes=1024*1024, 
                backupCount=5,
                encoding='utf-8'
            )
            file_handler.setFormatter(formatter)
            self.logger.addHandler(file_handler)
        except Exception as e:
            print(f"파일 핸들러 설정 실패: {e}")

        # 스트림 핸들러 설정
        stream_handler = logging.StreamHandler(sys.stderr)
        stream_handler.setFormatter(formatter)
        stream_handler.setLevel(logging.DEBUG)
        self.logger.addHandler(stream_handler)

        self.enable_elfin_log = elfin_log
        self.enable_mqtt_log = mqtt_log

    def __del__(self):
        # 명시적으로 모든 핸들러를 정리
        if hasattr(self, 'logger'):
            for handler in self.logger.handlers[:]:
                try:
                    handler.close()
                    self.logger.removeHandler(handler)
                except:
                    pass

    def _log(self, level, message):
        try:
            getattr(self.logger, level)(message)
        except Exception as e:
            print(f"Logging error: {e}")

    def info(self, message):
        self._log('info', message)

    def error(self, message):
        self._log('error', message)

    def warning(self, message):
        self._log('warning', message)

    def debug(self, message):
        self._log('debug', message)

    def signal(self, message):
        if self.enable_elfin_log:
            self._log('debug', f'[RS485] {message}')

    def mqtt(self, message):
        if self.enable_mqtt_log:
            self._log('debug', f'[MQTT] {message}')

    def set_level(self, level):
        self.logger.setLevel(level)
