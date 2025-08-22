FROM openjdk:17-jdk-slim
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
-Djava.rmi.server.hostname=0.0.0.0 \
"
ENTRYPOINT ["java", "-jar", "app.jar"]