# 使用Gradle打包Spring Boot应用  
local_resource(
    name='build-config-service-jar',
    cmd='cd config-service && gradlew build',
    #inputs=['build.gradle', 'src/**/*', 'gradle/**/*'],  # 列出所有Gradle构建可能依赖的文件,Tilt 将监视这些文件或目录的变化。如果列表中的任何文件或目录发生变化，Tilt 将重新执行 command。
    #outputs=['build/libs/config-service.jar'],  # 假设jar包输出到这个位置，根据实际情况调整  
    deps=['Dockerfile'],  # 如果Dockerfile依赖于jar包的路径，可以将其列为依赖  
)
local_resource(
    name='build-catalog-service-jar',
    cmd='cd catalog-service && gradlew.bat build',
    deps=['Dockerfile'],  # 如果Dockerfile依赖于jar包的路径，可以将其列为依赖  
)

# 使用Dockerfile构建容器镜像  
# 假设Dockerfile在同一目录下，并且它知道如何找到并使用build/libs/config-service.jar  
docker_build('config-service', 'config-service')  
docker_build('catalog-service', 'catalog-service')  
# Kubernetes部署  
k8s_yaml('config-service/k8s/deployment.yml')  
k8s_yaml('config-service/k8s/service.yml')  
k8s_yaml('catalog-service/k8s/deployment.yml')  
k8s_yaml('catalog-service/k8s/service.yml')  

# 端口转发（可选，取决于您的Kubernetes服务配置）  
#port_forward(8888, 80)  

# 管理
k8s_resource('config-service', port_forwards=['8888'])
k8s_resource('catalog-service', port_forwards=['9003'])