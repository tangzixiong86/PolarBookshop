FROM ccr.ccs.tencentyun.com/library/alpine:latest AS base
# 安装 OpenJDK 21 JRE
RUN apk add --update openjdk21-jre

# 第一阶段：构建阶段
FROM base AS builder
WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} order-service.jar
RUN java -Djarmode=tools -jar order-service.jar extract --launcher --layers --destination libs

# 第二阶段：生产阶段
FROM base AS final
# 创建用户
RUN adduser -D -S spring
# 切换用户
USER spring

WORKDIR /app
COPY --from=builder /app/libs/dependencies .
COPY --from=builder /app/libs/spring-boot-loader .
COPY --from=builder /app/libs/snapshot-dependencies .
COPY --from=builder /app/libs/application .

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
