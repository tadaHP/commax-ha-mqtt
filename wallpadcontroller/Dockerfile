FROM eclipse-temurin:21-jdk-jammy

# 비루트 사용자 생성 (보안)
RUN useradd -r -u 10001 -g root appuser

# 앱 실행 디렉터리와 데이터 디렉터리 분리
WORKDIR /opt/app
COPY build/libs/wallpadcontroller-0.0.1-SNAPSHOT.jar app.jar

# 데이터 디렉터리 준비
RUN mkdir -p /data \
 && chown -R appuser:root /opt/app /data

USER appuser
EXPOSE 8080

# 컨테이너 환경변수로 프로필/자바옵션 제어 가능
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
