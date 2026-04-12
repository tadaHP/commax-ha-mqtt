FROM eclipse-temurin:21-jdk-jammy

# 비루트 사용자 생성 (보안)
RUN useradd -r -u 10001 -g root appuser

# 앱 실행 디렉터리와 데이터 디렉터리 분리
WORKDIR /opt/app
COPY build/libs/wallpadcontroller-0.0.1-SNAPSHOT.jar app.jar

# 데이터 디렉터리 준비 (/app: H2 파일 DB 기본 경로)
RUN mkdir -p /data /app \
 && chown -R appuser:root /opt/app /data /app

USER appuser
EXPOSE 8080

# 컨테이너 환경변수로 프로필/자바옵션 제어 가능
# @CreationTimestamp·검색(from/to)과 맞추기: JVM 기본 타임존(한국 벽시계 권장)
ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
