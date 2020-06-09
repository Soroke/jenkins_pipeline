import hudson.model.*;
def call(String imageName) {
	result = sh(script: "docker image ls '${imageName}:backup' |wc -l", returnStdout: true).trim()
	if (result) {
		println "已有备份TAG：${imageName}:backup存在,故执行删除操作"
		sh """docker image rm ${imageName}:backup"""
	}
	println "执行备份TAG：${imageName}:backup"
	sh """docker tag '${imageName}' ${imageName}:backup"""
}