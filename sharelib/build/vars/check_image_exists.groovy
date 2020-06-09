import hudson.model.*;
def call(String imageName) {
	result = sh(script: "docker image ls '${imageName}:latest' |wc -l", returnStdout: true).trim()
	//int sokore = sh """docker image ls '${imageName}' |wc -l"""
	if ("${result}" == "2") {
		println "镜像：${imageName}:latest存在。"
		return true
	} else {
		println "镜像：${imageName}:latest不存在。"
		return false
	}
}