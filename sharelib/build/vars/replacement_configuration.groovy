import hudson.model.*;
//替换配置文件，配置文件替换完成后路径为：${env.WORKSPACE}/compfile
def call(String ymlFilePath,String deployProjects) {
	/**if(!deployProjects.contains(",")){
		//def servername=get_ini.getServiceCenter(deployProjects)
		if (new File(WORKSPACE + "/" + deployProjects + "/pom.xml").exists()) {
			sh """if [ ! -d "${env.WORKSPACE}/compfile/${deployProjects}" ];then
				mkdir -p ${env.WORKSPACE}/compfile/${deployProjects}
			else
				rm -rf ${env.WORKSPACE}/compfile/${deployProjects}
				mkdir -p ${env.WORKSPACE}/compfile/${deployProjects}
			fi"""
		//if(!servername.contains("mh")) {
			//sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${deployProjects}/target/${deployProjects}-1.0.0-SNAPSHOT/* ${env.WORKSPACE}/compfile/${deployProjects} && cp -r ${env.WORKSPACE}/${deployProjects}/version.txt ${env.WORKSPACE}/compfile/${deployProjects}"""
			def dirPath_version=WORKSPACE + "/" + deployProjects + "/target/" + deployProjects + "-1.0.0-SNAPSHOT"
			def dirPath_no_version=WORKSPACE + "/" + deployProjects + "/target/" + deployProjects
			File targetFile_v = new File(dirPath_version)
			File targetFile_nv = new File(dirPath_no_version)
			if (targetFile_v.exists()){
				sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${deployProjects}/target/${deployProjects}-1.0.0-SNAPSHOT/* ${env.WORKSPACE}/compfile/${deployProjects} && cp -r ${env.WORKSPACE}/${deployProjects}/version.txt ${env.WORKSPACE}/compfile/${deployProjects}"""
			} else if(targetFile_nv.exists()){
				sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${deployProjects}/target/${deployProjects}/* ${env.WORKSPACE}/compfile/${deployProjects} && cp -r ${env.WORKSPACE}/${deployProjects}/version.txt ${env.WORKSPACE}/compfile/${deployProjects}"""
			} else {
				sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${deployProjects}/target/${deployProjects}-1.0.0-SNAPSHOT/* ${env.WORKSPACE}/compfile/${deployProjects} && cp -r ${env.WORKSPACE}/${deployProjects}/version.txt ${env.WORKSPACE}/compfile/${deployProjects}"""
			}
		}
	} else {
		def names=deployProjects.split(",")
		for(int i=0;i<names.size();i++) {
			//def servername=get_ini.getServiceCenter(names[i])
			if (new File(WORKSPACE + "/" + names[i] + "/pom.xml").exists()) {
				sh """if [ ! -d "${env.WORKSPACE}/compfile/${names[i]}" ];then
					mkdir -p ${env.WORKSPACE}/compfile/${names[i]}
				else
					rm -rf ${env.WORKSPACE}/compfile/${names[i]}
					mkdir -p ${env.WORKSPACE}/compfile/${names[i]}
				fi"""
			//if(!servername.contains("mh")) {
				def dirPath_version=WORKSPACE + "/" + names[i] + "/target/" + names[i] + "-1.0.0-SNAPSHOT"
				def dirPath_no_version=WORKSPACE + "/" + names[i] + "/target/" + names[i]
				File targetFile_v = new File(dirPath_version)
				File targetFile_nv = new File(dirPath_no_version)
				if (targetFile_v.exists()){
					sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${names[i]}/target/${names[i]}-1.0.0-SNAPSHOT/* ${env.WORKSPACE}/compfile/${names[i]} && cp -r ${env.WORKSPACE}/${names[i]}/version.txt ${env.WORKSPACE}/compfile/${names[i]}"""
				} else if(targetFile_nv.exists()){
					sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${names[i]}/target/${names[i]}/* ${env.WORKSPACE}/compfile/${names[i]} && cp -r ${env.WORKSPACE}/${names[i]}/version.txt ${env.WORKSPACE}/compfile/${names[i]}"""
				} else {
					sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${names[i]}/target/${names[i]}-1.0.0-SNAPSHOT/* ${env.WORKSPACE}/compfile/${names[i]} && cp -r ${env.WORKSPACE}/${names[i]}/version.txt ${env.WORKSPACE}/compfile/${names[i]}"""
				}
			}
		}
	}**/
	sh """python /var/jenkins_home/render_conf.py ${env.WORKSPACE}/compfile ${ymlFilePath}"""
}