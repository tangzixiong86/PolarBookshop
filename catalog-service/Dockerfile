# 第一阶段：构建阶段
FROM alpine:latest AS builder
# 安装 OpenJDK 21 JRE
RUN apk add --update openjdk21-jre
WORKDIR /workspace
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} catalog-service.jar
# 提取分层 JAR 文件
RUN java -Djarmode=tools -jar catalog-service.jar extract --launcher --layers --destination libs

# 第二阶段：生产阶段
FROM alpine:latest
# 安装 OpenJDK 21 JRE
RUN apk add --update openjdk21-jre
# 创建用户
RUN adduser -D -S spring
# 切换用户
USER spring
# 设置工作目录
WORKDIR /app
# 复制构建阶段的输出
COPY --from=builder /workspace/libs/dependencies/ ./
COPY --from=builder /workspace/libs/spring-boot-loader/ ./
COPY --from=builder /workspace/libs/snapshot-dependencies/ ./
COPY --from=builder /workspace/libs/application/ ./
# 设置容器启动命令
# 与spring boot 3.*与spring boot 2.*之间的区别，spring boot 2.*应用使用org.springframework.boot.loader.JarLauncher
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
