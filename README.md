# PolarBookshop
## 部署
1.使用指定的资源和配置来启动一个名为 "polar" 的Kubernetes集群。如果之前没有创建过名为 "polar" 的集群，minikube 将会创建一个新的；如果已经存在，则会重启该集群并应用新的配置。
```bash
minikube start --cpus 2 --memory 4g --driver docker --profile polar  --force --base-image='registry.cn-hangzhou.aliyuncs.com/google_containers/kicbase:v0.0.44'
```
> **Warning:** 因为国内网络限制原因，应该从国内镜像源拉取镜像，运行minikube start时指定--force 和 --base-image参数

> **Note:** 从我的实际测试来看这里非常奇怪：cmd中运行命令仍然会从dokcer.io上面拉取镜像，导致失败，但是从PowerShell运行时则不会，它们成功创建集群

2.打开一个终端窗口，导航到位于 polar-deployment/kubernetes/platform/development 文件夹，并运行以下命令在您的本地集群中部署 PostgreSQL：
```bash
kubectl apply -f services
```
> **Warning:** 因为国内网络限制原因，拉取postgres:latest时会失败，解决办法：<br/>1.在清单文件中指定镜像拉取策略：IfNotPresent<br/>
>2.使用docker pull postgres:latest，从国内镜像源拉取镜像<br/>
>3.使用minikube image load postgres:latest --profile polar，将镜像加载到本地集群。

3.构建catalog-service镜像
```bash
gradlew bootBuildImage
```
> **Note:** 因为国内网络限制原因,以上命令可能会因为拉取镜像失败而出错。

通过Dockerfile来构建镜像，导航到PolarBookshop\catalog-service目录，该目录有一个Dockerfile文件，内容如下：
```Dockerfile
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
COPY --from=builder /workspace/libs/snapshot-dependencies/ .
COPY --from=builder /workspace/libs/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

```
在catalog-service目录下运行以下命令构建镜像：
```bash
docker build -t catalog-service .
```
4.将catalog-service镜像导入到本地集群
```bash
minikube image load catalog-service --profile polar
```
5.将部署清单应用于集群

打开一个终端窗口，导航到您的 Catalog Service 根文件夹（catalog-service），在该目录下有一个k8s文件夹，k8s文件夹下有一个deployment.yml，并运行以下命令：
```bash
kubectl applay -f k8s/deployment.yml
```
deployment.yml文件内容如下：
```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: catalog-service
  labels:
    app: catalog-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: catalog-service
  template:
    metadata:
      labels:
        app: catalog-service
    spec:
      containers:
        - name: catalog-service
          image: catalog-service
          imagePullPolicy: IfNotPresent
          lifecycle:
            preStop:
              exec:
                command: [ "sh", "-c", "sleep 5" ]
          ports:
            - containerPort: 9001
          env:
            - name: BPL_JVM_THREAD_COUNT
              value: "50"
            - name: SPRING_CLOUD_CONFIG_URI
              value: http://config-service
            - name: SPRING_DATASOURCE_URL
              value: jdbc:postgresql://polar-postgres/polardb_catalog
            - name: SPRING_PROFILES_ACTIVE
              value: testdata

```
6.将服务清单应用到集群
打开一个终端窗口，导航到您的 Catalog Service 根文件夹（catalog-service），在该目录下有一个k8s文件夹，k8s文件夹下有一个service.yml，并运行以下命令：
```bash
kubectl apply -f k8s/service.yml
```
service.yml文件内容如下：
```yml
apiVersion: apps/v1
kind: Service
metadata:
  name: catalog-service
  labels:
    app: catalog-service
spec:
  type: ClusterIP
  selector:
    app: catalog-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 9003

```
7.将catalog-service服务暴露到集群外部，以便可以从集群外部访问它
```bash
kubectl port-forward service/catalog-service 9003:80
```
## 使用Tilt实现自动化
1.在catalog-service目录下有一个Tiltfile，在该目录下执行以下命令：
```bash
tilt up
```
2.运行以下命令来取消部署应用程序:
```bash
tilt down
```
## 在集群里启用ingress附加组件 
```bash
minikube addons enable ingress --images="KubeWebhookCertgenCreate=google_containers/kube-webhook-certgen:v1.4.1,KubeWebhookCertgenPatch=google_containers/kube-webhook-certgen:v1.4.1,IngressController=google_containers/nginx-ingress-controller:v1.10.1" --registries="IngressController=registry.cn-hangzhou.aliyuncs.com,KubeWebhookCertgenCreate=registry.cn-hangzhou.aliyuncs.com,KubeWebhookCertgenPatch=registry.cn-hangzhou.aliyuncs.com" --profile polar
```
> **Warning:** 因国内网络限制，需使用国内镜像源

## 命令参考
|    命令                         |               说明                  |
|:--------------------------------|:-----------------------------------|
|kubectl get nodes                |获取集群中所有节点的列表              |
|kubectl get pods                  |获取集群中所有Pod                    |
|kubectl get pod -l app=catalog-service| 获取集群中所有标签app=catalog-service|
|kubectl delete pod <pod-name>    |删除指定pod                         |
|kubectl logs <pod-name>          |检查容器的日志                         |
|kubectl logs --previous <pod-name>|如果kubectl logs命令没有返回任何结果，尝试加上--previous标志，以查看容器上一次运行时的日志。|
|kubectl config get-contexts      |列出所有可用的上下文                  |
|kubectl config current-context   |当前上下文                           |
|kubectl config use-context polar |更改当前上下文                       |
|kubectl describe pod <pod 名称>  |获取指定 Pod 的详细信息                       |
|kubectl apply -f 文件名或者目录名 |如果是目录名，会递归地查找该目录下的所有 YAML 或 JSON 格式的文件，并对它们依次执行 apply 操作。 |
|minikube stop --profile polar    |停止polar集群                        |
|minikube start --profile polar   |启动polar集群                        |
|minikube delete --profile polar  |删除polar集群                        |
|minikube addons enable ingress --profile polar  |在集群里启用ingress附加组件                        |
|minikube ip --profile polar      |在Linux系统中获取分配给 minikube 集群的 IP 地址                        |
|minikube ip --profile polar      |在Linux系统中获取分配给 minikube 集群的 IP 地址                        |
|minikube tunnel --profile polar  |在 macOS 和 Windows 上，当在 Docker 上运行时，入口附加组件尚不支持使用 minikube 集群的 IP 地址。相反，我们需要使用以下命令将集群暴露到本地环境，然后使用 127.0.0.1 IP 地址来调用集群|
|docker-compose -p <项目名称> up -d|创建和启动服务所声明的容器，d 标志代表 "detached mode"（后台模式）。这意味着服务将在后台运行，不会阻塞终端窗口。|


