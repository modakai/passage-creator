# 贝尔实验室 Spring 官方推荐镜像 JDK 下载地址：https://bell-sw.com/pages/downloads/
FROM bellsoft/liberica-openjdk-debian:17.0.11-cds

LABEL maintainer="sakura"

# 创建运行目录，日志和临时文件都固定在 /sakura/server 下。
RUN mkdir -p /sakura/server/logs \
    /sakura/server/temp \
    /sakura/skywalking/agent

WORKDIR /sakura/server

ENV SERVER_PORT=8101 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

# Maven 当前产物名来自 pom.xml 的 artifactId。
COPY --from=builder /workspace/target/springboot3_init-0.0.1-SNAPSHOT.jar ./app.jar

# 使用 sh -c 保留 JAVA_OPTS 扩展能力，便于容器运行时追加 JVM 参数。
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${SERVER_PORT} -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} -jar app.jar"]
