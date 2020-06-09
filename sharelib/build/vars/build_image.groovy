import hudson.model.*;
//(废弃)
def call(String serverip,String imageName,String dockerFileSvnPath,String projectlist,String centerName) {
	def now_date=new Date().format('yyyyMMdd');
	if (common.checkImageExists(serverip,imageName)) {
		//已存在镜像时，检查当前镜像是否有备份tag存在，如果有删除tag，重新执行备份；如果没有执行执行tag备份
		if (common.checkImageExists(serverip,imageName + "_backup")) {
			log.info "已有备份TAG：${imageName}_backup存在,故执行删除操作"
			sh """ssh -p 29050 ${serverip} 'docker image rm ${imageName}_backup'"""
			sh """ssh -p 29050 ${serverip} 'docker tag ${imageName} ${imageName}_backup'"""
		} else {
			log.info "执行备份TAG：${imageName}:backup"
			sh """ssh -p 29050 ${serverip} 'docker tag ${imageName} ${imageName}_backup'"""
		}
		copyProjectToService(imageName,projectlist,centerName,serverip)
		//已存在镜像时，启动镜像并在当前镜像的基础上重新打包镜像
		//sh """ssh -p 29050 ${serverip} 'docker run --name buildNewTag -d ${imageName}'"""
		
		//copyProjectToImage("buildNewTag",projectlist,centerName,serverip)

		//sh """ssh -p 29050 ${serverip} 'docker commit buildNewTag ${imageName}'
		//	ssh -p 29050 ${serverip} 'docker rm -f buildNewTag'"""
	} else {
		//首次执行镜像打包,下载dockerFile文件执行镜像打包
		checkout_svn(dockerFileSvnPath,"dockerFile")
		sh """ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${now_date} && mkdir -p /tmp/jenkins_docker_package/${now_date}'
			scp -P 29050 -r ${env.WORKSPACE}/dockerFile/* ${serverip}:/tmp/jenkins_docker_package/${now_date}
			ssh -p 29050 ${serverip} 'cd /tmp/jenkins_docker_package/${now_date} && docker build -t ${imageName} .'
			ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${now_date}'
		"""
		copyProjectToService(imageName,projectlist,centerName,serverip)
		//copyProjectToImage("buildNewTag",projectlist,centerName,serverip)
		//sh """ssh -p 29050 ${serverip} 'docker commit buildNewTag ${imageName}'
		//	ssh -p 29050 ${serverip} 'docker rm -f buildNewTag'"""
	}
}

//根据传入服务中心名称和项目列表获取改服务中心的所有服务名称，并打包进镜像中(废弃)
def copyProjectToService(String imageName,String projectlist,String centerName,String serverip) {
	//清理文件夹
	sh """ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/projectlist/${centerName} && mkdir -p /tmp/jenkins_docker_package/projectlist/${centerName}'"""
	def allCenterProj = get_ini.getCenterProjectlist(projectlist,centerName)
	allCenterProj.each {
		sh """scp -P 29050 -r ${env.WORKSPACE}/compfile/${it}/ ${serverip}:/tmp/jenkins_docker_package/projectlist/${centerName}/"""
	}
	sh """ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/projectlist/Dockerfile'
		ssh -p 29050 ${serverip} 'echo "FROM ${imageName}\nMAINTAINER songrenkun "songrenkun@faxuan.net"\nCOPY ./${centerName} /usr/local/resin/webapps/\nSTOPSIGNAL SIGQUIT" > /tmp/jenkins_docker_package/projectlist/Dockerfile'
		ssh -p 29050 ${serverip} 'cd /tmp/jenkins_docker_package/projectlist/ && docker build -t ${imageName} .'
	"""
}










//根据传入服务中心名称和项目列表获取该服务中心的所有服务名称，并重新执行镜像打包
def copyProjectToImageAndRebuildImage(String projectlist,String centerName,String serverip) {
	//获取镜像的名称
	def imageName = get_ini("docker." + centerName + "_image_name")
	def platformName = get_ini("dir.project")
	//镜像备份
	if (common.checkImageExists(serverip,imageName + ":latest_backup")) {
		log.info "已有备份TAG：${imageName}:latest_backup存在,故执行删除操作"
		sh """ssh -p 29050 ${serverip} 'docker image rm ${imageName}:latest_backup'"""
		sh """ssh -p 29050 ${serverip} 'docker tag ${imageName}:latest ${imageName}:latest_backup'"""
	} else {
		//检查本地是否存在latest tag的镜像，存在时才执行镜像备份上传
		if(common.checkImageExists(serverip,imageName + ":latest")){
			log.info "执行备份TAG：${imageName}:latest_backup"
			sh """ssh -p 29050 ${serverip} 'docker tag ${imageName}:latest ${imageName}:latest_backup'"""
		}
	}
	
	//检查构建镜像使用的基础镜像是否存在(存在先删除，保持最新)
	if (common.checkImageExists(serverip,imageName + ":base")) {
		sh """ssh -p 29050 ${serverip} 'docker image rm ${imageName}:base'"""
	}
	//生成新的镜像
	sh """ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${platformName}/${centerName} && mkdir -p /tmp/jenkins_docker_package/${platformName}/${centerName}'"""
	def allCenterProj = get_ini.getCenterProjectlist(projectlist,centerName)
	allCenterProj.each {
		sh """scp -P 29050 -r ${env.WORKSPACE}/compfile/${it}/ ${serverip}:/tmp/jenkins_docker_package/${platformName}/${centerName}/"""
	}
	sh """ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${platformName}/Dockerfile'
		ssh -p 29050 ${serverip} 'echo "FROM ${imageName}:base\nMAINTAINER songrenkun "songrenkun@faxuan.net"\nCOPY ./${centerName} /usr/local/webapps/\nSTOPSIGNAL SIGQUIT" > /tmp/jenkins_docker_package/${platformName}/Dockerfile'
		ssh -p 29050 ${serverip} 'cd /tmp/jenkins_docker_package/${platformName}/ && docker build -t ${imageName}:latest .'
	"""
	//上传新镜像
	sh """ssh -p 29050 ${serverip} 'docker push ${imageName}:latest'"""
	//新镜像上传成功后检查本地存在备份镜像就上传备份镜像
	if (common.checkImageExists(serverip,imageName + ":latest_backup")) {
		sh """ssh -p 29050 ${serverip} 'docker push ${imageName}:latest_backup'"""
	}
	//删除镜像打包临时文件
	sh """ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${platformName}/'"""
}



//按照ini配置文件中iamge_type配置的镜像类型，构建所有类型的基础镜像（废弃）
def buildBaseImage(String serverip){
	def imageType = get_ini("docker.iamge_type")
	def types = imageType.split(",")
	for (int i=0;i<types.size();i++) {
		def imageName = get_ini("docker." + types[i] + "_image_name")
		def dockerfile = get_ini("docker." + types[i] + "_dockerfile")
		//checkout Dockerfile文件是否有更新，有更新时执行镜像构建
		if (checkout_svn(dockerfile,types[i])){
			if(common.checkImageExists(serverip,imageName)){
				sh """ssh -p 29050 ${serverip} 'docker image rm ${imageName}'"""
			}
			sh """ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${types[i]} && mkdir -p /tmp/jenkins_docker_package/${types[i]}'
			scp -P 29050 -r ${env.WORKSPACE}/${types[i]}/* ${serverip}:/tmp/jenkins_docker_package/${types[i]}
			ssh -p 29050 ${serverip} 'cd /tmp/jenkins_docker_package/${types[i]} && docker build -t ${imageName} .'
			ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${types[i]}'
			"""
			//上传基础镜像
			sh """ssh -p 29050 ${serverip} 'docker push ${imageName}'"""
		}
	}
}


//构建基础镜像(构建基础镜像job使用该方法执行基础镜像的构建,用户只需要输入mysql,resin等前置名称就行)
def buildBaseImage(String platformName,String imageNames){
	def serverip = get_ini("docker-server.ip")
	def allImages = get_ini.getAllKey(platformName)
	if (env.imageNames?.trim()) {
		//记录用户镜像名称是否正确
		def isTure = turn
		def imageName = imageNames.split(",")
		for(int i=0;i<imageName.size();i++) {
			def isContains=false
			if (allImages.contains(imageName[i])){
				isContains=turn
			}
			if(isContains != turn){
				isTure = false
			}
		}
		if(isTure){
			allImages = imageNames
		} else {
			println "输入image名称不正确，请参照ini配置文件中当前平台下有哪些基础镜像"
			shell """exit -1"""
		}
	}
	
	
	def images = allImages.split(",")
	def imNames = []
	for(int i=0;i<images.size();i++) {
		def test = images[i].split("_")
		if(test.size() != 1){
			if(test.size() == 2){
				imNames.add(test[0])
			}
		} else {
			imNames = images
		}
	}
	//去重
	imNames.unique()
	
	//检查用户输入基础镜像名称或ini配置文件下当前平台基础镜像是否为空，为空时退出执行
	if(imNames.size() == 0) {
		println "ini配置文件下当前平台：" + platformName + "的基础镜像为空。"
		shell """exit -1"""
	}
	
	//循环执行镜像构建
	for (int i=0;i<imNames.size();i++) {
		def imageName_n = get_ini(platformName +"."+ imNames[i] + "_image_name")
		def dockerfile = get_ini(platformName +"."+ imNames[i] + "_dockerfile")
		//checkout Dockerfile文件是否有更新，有更新时执行镜像构建
		if (checkout_svn(dockerfile,platformName + "_" + imNames[i])){
			if(common.checkImageExists(serverip,imageName_n)){
				sh """ssh -p 29050 ${serverip} 'docker image rm -f ${imageName_n}'"""
			}
			sh """ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${platformName}_${imNames[i]} && mkdir -p /tmp/jenkins_docker_package/${platformName}_${imNames[i]}'
			scp -P 29050 -r ${env.WORKSPACE}/${platformName}_${imNames[i]}/* ${serverip}:/tmp/jenkins_docker_package/${platformName}_${imNames[i]}
			ssh -p 29050 ${serverip} 'cd /tmp/jenkins_docker_package/${platformName}_${imNames[i]} && docker build -t ${imageName_n} .'
			ssh -p 29050 ${serverip} 'rm -rf /tmp/jenkins_docker_package/${platformName}_${imNames[i]}'
			"""
			//上传基础镜像到Harbor平台
			sh """ssh -p 29050 ${serverip} 'docker push ${imageName_n}'"""
		}
	}
}
