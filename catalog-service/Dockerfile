
FROM ccr.ccs.tencentyun.com/library/alpine:latest AS base
# 安装 OpenJDK 21 JRE
RUN apk add --update openjdk21-jre

# 第一阶段：构建阶段
FROM base AS builder
WORKDIR /workspace
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} catalog-service.jar
# 提取分层 JAR 文件
RUN java -Djarmode=tools -jar catalog-service.jar extract --launcher --layers --destination libs

# 第二阶段：生产阶段
FROM base AS final
# 创建用户
RUN adduser -D -S spring
# 切换用户
USER spring
# 设置工作目录
WORKDIR /app
# 复制构建阶段的输出
# 以下效果都一样，都是将/workspace/libs/dependencies目录下面文件或目录(注意不是dependencies目录本身)复制到app目录下面
#COPY --from=builder /workspace/libs/dependencies ./
#COPY --from=builder /workspace/libs/dependencies .
#COPY --from=builder /workspace/libs/dependencies/ .
COPY --from=builder /workspace/libs/dependencies/ ./
COPY --from=builder /workspace/libs/spring-boot-loader/ ./
COPY --from=builder /workspace/libs/snapshot-dependencies/ .
COPY --from=builder /workspace/libs/application/ ./
#为输出目录内容，需执行docker build命令时添加--progress=plain
#因为缓存也有可能RUN ls -laR /src/SpecialOffers不会输出
#这时可以修改它之前的命令，破坏缓存，比如修改RUN echo，这就会有礊了
#RUN echo "Listing contents of /app"
#RUN ls -laR /app
# 设置容器启动命令
# 与spring boot 3.*与spring boot 2.*之间的区别，spring boot 2.*应用使用org.springframework.boot.loader.JarLauncher
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
