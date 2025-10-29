FROM openjdk:17-jdk-slim
# ✨ 바로 여기! 폰트 엔진(freetype)과 폰트 설정(fontconfig) 라이브러리를 설치한다.
RUN apt-get update && apt-get install -y libfreetype6 fontconfig
WORKDIR /app
COPY build/libs/*.jar app.jar

# ✨ 여기가 핵심! JMX 원격 접속을 위한 JVM 옵션을 환경변수로 추가
ENV _JAVA_OPTIONS="\
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9010 \
-Dcom.sun.management.jmxremote.rmi.port=9010 \
-Dcom.sun.management.jmxremote.local.only=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Djava.awt.headless=true \
-Djava.rmi.server.hostname=0.0.0.0 \
"
ENTRYPOINT ["java", "-jar", "app.jar"]