import hudson.model.*;
def call(String projectlist) {
	sh """if [ ! -d "${env.WORKSPACE}/compfile" ];then
		echo "已编译完成项目存放文件夹：${env.WORKSPACE}/compfile不存在。"
		exit -1
		fi"""
	def deployProjectName=projectlist.split(",")
	def container_type=get_ini("server.container_type")
	def sshPort=get_ini("server.ssh_port")
	println "ssh_port:" + sshPort
	
	for(int i=0;i<deployProjectName.size();i++) {
		//增加判断，yml有更新时 所有项目都需要重新部署
		def ymlUpdate = false
		if(checkout_svn.checkUpdate("yml")) {
			ymlUpdate = true
		}
		//检查当前服务中心的部署项目中是否本次有更新
		def isUpdate = false
		if(checkout_svn.checkUpdate(deployProjectName[i])) {
			isUpdate = true
		}

		if(isUpdate || ymlUpdate) {
			def serverCenter = get_ini.getServiceCenter(deployProjectName[i])
			for(int sc=0;sc<serverCenter.size();sc++) {
				def ips = get_ini.getServiceIp(serverCenter[sc])
				println "工程：" + deployProjectName[i] + "对应的服务中心" + serverCenter[sc] + "的对应IP为：" + ips
				//println "工程：" + deployProjectName[i] + "的SVN版本号为：" + sh """cat ${WORKSPACE}/deployProjectName[i]/version.txt"""
				def deploay_path = get_ini("server." + serverCenter[sc] + "_deploay_path")
				for(int j=0;j<ips.size();j++) {
					if(ips[j] != '' && deploay_path != ''){
						//适配云平台的rdt项目
						if (deployProjectName[i].equals("rdt")) {
							sh """rsync  -vazrtopqg --password-file=/var/jenkins_home/workspace/rsyncd-win.pas ${env.WORKSPACE}/rdt/build/rdt rsync@192.168.0.33::rdt/"""
						//适配bot项目部署
						} else if(deployProjectName[i].contains("smartqa")) {
							sendProjectToService(ips[j],sshPort,deployProjectName[i],deploay_path)
							restartService(ips[j],sshPort,deploay_path,container_type)
						//当前部署项目不包含pom.xml文件、不包含文件WebRoot、build，3种条件同时成立，判定为前端部署项目，不需要重启服务
						} else if (! new File(WORKSPACE + "/" + deployProjectName[i] + "/pom.xml").exists() && ! new File(WORKSPACE + "/" + deployProjectName[i] + "/WebRoot").exists() && ! new File(WORKSPACE + "/" + deployProjectName[i] + "/build").exists()) {
							sendProjectToService(ips[j],sshPort,deployProjectName[i],deploay_path)
						} else {
							sendProjectToService(ips[j],sshPort,deployProjectName[i],deploay_path + "/webapps")
							restartService(ips[j],sshPort,deploay_path,container_type)
						}
					} else {
						println "项目：" + deployProjectName[i] + " 所属服务器未在ini文件配置IP或部署路径"
					}
				}
			}
			
		} else {
			println "项目：" + deployProjectName[i] + " 本次部署没有更新故不再做无用部署"
		}
	}
	//部署成功后复制部署项目到deploy目录
	scpdeployfile(projectlist)
	//复制部署项目的源码和classes文件
	scpSourceAndClassesfile(projectlist)
	//部署成功后删除update.txt文件
	common.delectUpdateFile(projectlist)
	common.delectUpdate("yml")
	log.info("项目：" + projectlist + "全部部署完成。")
}

//多环境选择部署（目前只有项目管理系统涉及到多环境部署）
def differentEnv(String projectlist,String environment) {
	sh """if [ ! -d "${env.WORKSPACE}/compfile" ];then
		echo "已编译完成项目存放文件夹：${env.WORKSPACE}/compfile不存在。"
		exit -1
		fi"""
	def deployProjectName=projectlist.split(",")
	def container_type=get_ini("server.container_type")
	def sshPort=get_ini("server.ssh_port")
	for(int i=0;i<deployProjectName.size();i++) {
		//获取项目所属服务中心
		def serverCenter = get_ini.getServiceCenter(deployProjectName[i])
		//循环服务中心开始部署项目
		for(int sc=0;sc<serverCenter.size();sc++) {
			//获取当前服务中心的ip地址
			def ip = get_ini("server." + environment + "_" + serverCenter[sc] + "_ip")
			println "工程：" + deployProjectName[i] + "对应的服务中心" + serverCenter[sc] + "的对应IP为：" + ip
			//println "工程：" + deployProjectName[i] + "的SVN版本号为：" + sh """cat ${WORKSPACE}/deployProjectName[i]/version.txt"""
			//获取当前服务中心的部署路径
			def deploay_path = get_ini("server." + environment + "_" + serverCenter[sc] + "_deploay_path")
			//判断IP和部署路径都不为空时，部署项目
			if(ip != '' && deploay_path != ''){
				//判断当前部署项目是否为前端项目，前端只部署；后台项目部署完成后需要重启容器
				if (! new File(WORKSPACE + "/" + deployProjectName[i] + "/pom.xml").exists() && ! new File(WORKSPACE + "/" + deployProjectName[i] + "/WebRoot").exists() && ! new File(WORKSPACE + "/" + deployProjectName[i] + "/build").exists()) {
					sendProjectToService(ip,sshPort,deployProjectName[i],deploay_path)
				} else {
					sendProjectToService(ip,sshPort,deployProjectName[i],deploay_path + "/webapps")
					restartService(ip,sshPort,deploay_path,container_type)
				}
			} else {
				println "项目：" + deployProjectName[i] + " 所属服务器未在ini文件配置IP或部署路径"
			}
		}
	}
	//部署成功后复制部署项目到deploy目录
	scpdeployfile(projectlist)
	scpSourceAndClassesfile(projectlist)
	//部署成功后删除update.txt文件
	common.delectUpdateFile(projectlist)
}

//复制指定项目到指定服务器
def sendProjectToService(String ip,String port,String projectname,String deployPath){
	//修改复制部署项目到指定服务器的方法
	//适配学法平台的u_xxx,k_xxx,m_xxx,userrg等项目的部署
	def proNa = get_ini("dir.project")
	if(proNa.equals("xf")) {
		if(projectname.contains("_")) {
			//首先检查多语言目录是否存在
			sh """
				if [ ! -d "${deployPath}/uygur" ];then
					mkdir -p ${deployPath}/uygur
				fi
				if [ ! -d "${deployPath}/kazakh" ];then
					mkdir -p ${deployPath}/kazakh
				fi
				if [ ! -d "${deployPath}/mongolian" ];then
					mkdir -p ${deployPath}/mongolian
				fi
			"""
			def projectnames = projectname.tokenize("_")
			if(projectnames[0].equals("u")){	//维语
				sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
				ssh -p ${port} ${ip} "rm -rf ${deployPath}/uygur/${projectname}"
				scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/uygur/"""
			} else if (projectnames[0].equals("k")){	//哈萨克语
				sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
				ssh -p ${port} ${ip} "rm -rf ${deployPath}/kazakh/${projectname}"
				scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/kazakh/"""
			} else if (projectnames[0].equals("m")){	//蒙语
				sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
				ssh -p ${port} ${ip} "rm -rf ${deployPath}/mongolian/${projectname}"
				scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/mongolian/"""
			}
		} else if(projectname.equals("userrg")){		//学法的userrg项目部署完成后，需要复制rg.html
			sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
			ssh -p ${port} ${ip} "rm -rf ${deployPath}/${projectname}"
			ssh -p ${port} ${ip} "rm -rf ${deployPath}/rg.html"
			scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/
			scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname}/rg.html ${ip}:${deployPath}/"""
		} else if(projectname.equals("ss")){		//ss项目部署完成后需要授予可执行权限
			sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
			ssh -p ${port} ${ip} "rm -rf ${deployPath}/${projectname}"
			scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/
			ssh -p ${port} ${ip} 'chmod +x ${deployPath}/ss/datash/*'"""
		} else {		//其余学法项目，直接复制就行
			sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
			ssh -p ${port} ${ip} "rm -rf ${deployPath}/${projectname}"
			scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/"""
		}
	} else if (proNa.equals("fxshop")){
		if(projectname.equals("fss")) { //法宣电商fss项目特殊处理（原部署脚本就是这么处理的这里不做修改）
			sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
			ssh -p ${port} ${ip} "rm -rf  ${env.WORKSPACE}/compfile/${projectname}/WEB-INF/classes/conf/template"
			scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/"""
		} else {		//其余项目，直接复制就行
			sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
			ssh -p ${port} ${ip} "rm -rf ${deployPath}/${projectname}"
			scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/"""
		}
	} else if (proNa.equals("bot")){
		sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
		ssh -p ${port} ${ip} "rm -rf ${deployPath}/*"
		scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname}/* ${ip}:${deployPath}/
		"""
	} else {
		sh """echo `date '+%Y-%m-%d %T'`": cp $projectname to ${ip}"
		ssh -p ${port} ${ip} "rm -rf ${deployPath}/${projectname}"
		scp -P ${port} -r ${env.WORKSPACE}/compfile/${projectname} ${ip}:${deployPath}/"""
	}
}

//针对法治宝典项目部署单独处理
def deployFZBD(String projectlist,String type) {
	sh """if [ ! -d "${env.WORKSPACE}/compfile" ];then
	echo "已编译完成项目存放文件夹：${env.WORKSPACE}/compfile不存在。"
	exit -1
	fi"""
	def deployProjectName=projectlist.split(",")
	for(int i=0;i<deployProjectName.size();i++) {
		//增加判断，yml有更新时 所有项目都需要重新部署
		def ymlUpdate = false
		if(checkout_svn.checkUpdate("yml")) {
			ymlUpdate = true
		}
		//检查当前服务中心的部署项目中是否本次有更新
		def isUpdate = false
		if(checkout_svn.checkUpdate(deployProjectName[i])) {
			isUpdate = true
		}

		if(isUpdate || ymlUpdate) {
			def pt_container_type=get_ini("server.container_type")
			def pt_sshPort=get_ini("server.ssh_port")
			def docker_container_type=get_ini("server1.container_type")
			def docker_sshPort=get_ini("server1.ssh_port")
			def serverCenter = get_ini.getServiceCenter(deployProjectName[i])
			def centerN = serverCenter[0]
			def pt_ips = get_ini.getServiceIp(centerN)
			def docker_ips = get_ini.getServiceIpSections("server1",centerN)
			println "工程：" + deployProjectName[i] + "对应的服务中心" + centerN + "的对应IP为：" + pt_ips + docker_ips

			def pt_deploay_path = get_ini("server." + centerN + "_deploay_path")
			def docker_deploay_path = get_ini("server1." + centerN + "_deploay_path")

			if (type.equals("docker")) {
				for(int j=0;j<docker_ips.size();j++) {
					sendProjectToService(docker_ips[j],docker_sshPort,deployProjectName[i],docker_deploay_path)
					restartServiceFZBD(docker_ips[j],docker_sshPort,centerN,docker_container_type)
				}
			} else {
				for(int j=0;j<pt_ips.size();j++) {
					if (! new File(WORKSPACE + "/" + deployProjectName[i] + "/pom.xml").exists() && ! new File(WORKSPACE + "/" + deployProjectName[i] + "/WebRoot").exists() && ! new File(WORKSPACE + "/" + deployProjectName[i] + "/build").exists()) {
						sendProjectToService(pt_ips[j],pt_sshPort,deployProjectName[i],pt_deploay_path)
					} else {
						sendProjectToService(pt_ips[j],pt_sshPort,deployProjectName[i],pt_deploay_path + "/webapps")
						restartService(pt_ips[j],pt_sshPort,pt_deploay_path,pt_container_type)
					}
				}
			}
		}else {
			println "项目：" + deployProjectName[i] + " 本次部署没有更新故不再做无用部署"
		}
	}

	if (!type.equals("docker")) {
		//部署成功后复制部署项目到deploy目录
		scpdeployfile(projectlist)
		//复制部署项目的源码和classes文件
		scpSourceAndClassesfile(projectlist)
		//部署成功后删除update.txt文件
		common.delectUpdateFile(projectlist)
		common.delectUpdate("yml")
		log.info("项目：" + projectlist + "全部部署完成。")
	}

}

//服务重启宝典项目特殊处理
def restartServiceFZBD(String serverIp,String port,String serverCenter,String containerType) {
	if(containerType == "docker-compose-fzbd") {
		def dockerComposePath = get_ini("server1." + serverCenter + "_docker-compose_path")
		sh """ssh -p ${port} ${serverIp} 'docker-compose -f ${dockerComposePath} down && docker-compose -f ${dockerComposePath} up -d'"""
	} 
}
//根据容器类型重启服务
def restartService(String serverIp,String port,String path,String containerType) {
	if (containerType == "resin") {
		sh """ssh -p ${port} ${serverIp} 'rm -rf ${path}/resin-data/*;${path}/bin/resinctl restart'"""
	}else if(containerType == "tomcat") {
		sh """ssh -p ${port} ${serverIp} '${path}/bin/shutdown.sh'
			sleep 4
			ssh -p ${port} ${serverIp} '${path}/bin/startup.sh'
			sleep 4"""
	}else if(containerType == "docker-compose") {
		def dockerComposePath = get_ini("server.docker-compose_path")
		sh """ssh -p ${port} ${serverIp} 'docker-compose -f ${dockerComposePath} down && docker-compose -f ${dockerComposePath} up -d'"""
	} else {
		println "类型:" + containerType + "不属于已知(resin/tomcat)后台容器,请检查您的ini配置文件。"
	}
}

//复制部署项目到deploy文件夹
def scpdeployfile(String projectlist) {
	def names=projectlist.split(",")
	for(int i=0;i<names.size();i++) {
		sh """if [ ! -d "${env.WORKSPACE}/deployfile/${names[i]}" ];then
				mkdir -p ${env.WORKSPACE}/deployfile/${names[i]}
			  else
			  	rm -rf ${env.WORKSPACE}/deployfile/${names[i]}
			  	mkdir -p ${env.WORKSPACE}/deployfile/${names[i]}
			fi"""
		sh """cd ${env.WORKSPACE}/deployfile && cp -r ${env.WORKSPACE}/compfile/${names[i]}/* ${env.WORKSPACE}/deployfile/${names[i]}"""
	}
}

//复制部署项目的源码和编译完成的class文件到指定目录
def scpSourceAndClassesfile(String projectlist) {
	sh """
		if [ -f "${env.WORKSPACE}/coverage" ];then
			rm -rf ${env.WORKSPACE}/coverage
		fi
		if [ ! -d "${env.WORKSPACE}/coverage/sourcecode" ];then
			mkdir -p ${env.WORKSPACE}/coverage/sourcecode
		fi
		if [ ! -d "${env.WORKSPACE}/coverage/compfile" ];then
			mkdir -p ${env.WORKSPACE}/coverage/compfile
		fi"""
	def names=projectlist.split(",")
	for(int i=0;i<names.size();i++) {
		if (new File(WORKSPACE + "/" + names[i] + "/pom.xml").exists() ){
			sh """mkdir -p ${env.WORKSPACE}/coverage/sourcecode/${names[i]} && mkdir -p ${env.WORKSPACE}/coverage/compfile/${names[i]}"""
			//复制 源码
			sh """cp -r ${env.WORKSPACE}/${names[i]}/src/main/java/* ${env.WORKSPACE}/coverage/sourcecode/${names[i]}"""
			//复制class文件
			//单独处理一下bot项目的class文件复制
			if (names[i].contains("smartqa")) {
				sh """cp -r ${env.WORKSPACE}/${names[i]}/target/classes/* ${env.WORKSPACE}/coverage/compfile/${names[i]}"""
			} else{
				sh """cp -r ${env.WORKSPACE}/compfile/${names[i]}/WEB-INF/classes/* ${env.WORKSPACE}/coverage/compfile/${names[i]}"""
			}
		} else if(new File(WORKSPACE + "/" + names[i] + "/WebRoot").exists() && new File(WORKSPACE + "/" + names[i] + "/build").exists()){
			sh """mkdir -p ${env.WORKSPACE}/coverage/sourcecode/${names[i]} && mkdir -p ${env.WORKSPACE}/coverage/compfile/${names[i]}"""
			//复制 源码
			sh """cp -r ${env.WORKSPACE}/${names[i]}/src/* ${env.WORKSPACE}/coverage/sourcecode/${names[i]}"""
			//复制class文件
			sh """cp -r ${env.WORKSPACE}/compfile/${names[i]}/WEB-INF/classes/* ${env.WORKSPACE}/coverage/compfile/${names[i]}"""
		}
	}
}