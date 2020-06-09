import hudson.model.*;
def call(String projectlist) {
	//检查编译文件夹是否存在
	sh """if [ ! -d "${env.WORKSPACE}/compfile" ];then
		echo "已编译完成项目存放文件夹：${env.WORKSPACE}/compfile不存在。"
		exit -1
		fi"""

	//获取镜像部署服务器IP
	def serverip = get_ini("docker.image_deploy_ip")
	//构建基础镜像
	//build_image.buildBaseImage(serverip)
	//获取所有部署项目的服务中心
	def allCenter = classification(projectlist)
	allCenter.each {
		//获取部署项目列表中属于当前服务中心的所有项目
		def centerProject = get_ini.getCenterProjectlist(projectlist,it)
		//检查当前服务中心的部署项目中是否本次有更新
		def isUpdate = false
		for(int i=0;i<centerProject.size();i++) {
			if(checkout_svn.checkUpdate(centerProject[i])) {
				isUpdate = true
				break
			}
		}
		if(isUpdate) {
			//如果当前服务中心有更新时执行新镜像构建
			build_image.copyProjectToImageAndRebuildImage(projectlist,it,serverip)
		}
	}
	//复制本次部署项目到deploy文件夹下
	scpdeployfile(projectlist)
	//启动项目
	startProject(serverip)
	//部署成功后删除update.txt文件
	common.delectUpdateFile(projectlist)
}


//多环境选择部署
def differentEnv(String projectlist,String environment) {
	//检查编译文件夹是否存在
	sh """if [ ! -d "${env.WORKSPACE}/compfile" ];then
		echo "已编译完成项目存放文件夹：${env.WORKSPACE}/compfile不存在。"
		exit -1
		fi"""

	//获取镜像部署服务器IP
	def serverip = get_ini("docker." + environment  + "_image_deploy_ip")
	//构建基础镜像
	//build_image.buildBaseImage(serverip)
	//获取所有部署项目的服务中心
	def allCenter = classification(projectlist)
	allCenter.each {
		//涉及到多个环境的项目，无法根据当前项目是否有更新来判断是否重新生成镜像
		//故涉及多环境的部署每次部署都统一生成新的镜像
		build_image.copyProjectToImageAndRebuildImage(projectlist,it,serverip)
		//获取部署项目列表中属于当前服务中心的所有项目
		//def centerProject = get_ini.getCenterProjectlist(projectlist,it)
		//检查当前服务中心的部署项目中是否本次有更新
		//def isUpdate = false
		//for(int i=0;i<centerProject.size();i++) {
		//	if(checkout_svn.checkUpdate(centerProject[i])) {
		//		isUpdate = true
		//		break
		//	}
		//}
		//if(isUpdate) {
			//如果当前服务中心有更新时执行新镜像构建
		//	build_image.copyProjectToImageAndRebuildImage(projectlist,it,serverip)
		//}
	}
	//复制本次部署项目到deploy文件夹下
	scpdeployfile(projectlist)
	//启动项目
	startProject(serverip)
	//部署成功后删除update.txt文件
	common.delectUpdateFile(projectlist)
}

//获取待部署项目所有的服务中心名称
def classification(String projectlist) {
	def deployProjectName=projectlist.split(",")
	def allCenter = []
	for(int i=0;i<deployProjectName.size();i++) {
		allCenter = allCenter + get_ini.getServiceCenter(deployProjectName[i])
	}
	return allCenter.unique()
}

//项目启动
def startProject(String serverip) {
	def ymlSvn = get_ini("docker.start_yml_svn")
	def project = get_ini("dir.project")
	checkout_svn(ymlSvn,"dockerYml")
	//判断如果项目已启动,停止项目
	sh """ssh -p 29050 ${serverip} 'if [ -d "/usr/local/docker/${project}" ];then\ncd /usr/local/docker/${project} && docker-compose down && rm -rf /usr/local/docker/${project}/docker-compose.yml\nelse\nmkdir -p /usr/local/docker/${project}\nfi'"""
	//判断如果本地存在yml文件中使用的镜像，首先删除本地镜像，保证每次都获取最新镜像
	def imageNames = getDockerYmlAllImageNames(env.WORKSPACE + "/dockerYml/docker-compose.yml")
	imageNames.each {
		if(common.checkImageExists(serverip,it)){
			sh """ssh -p 29050 ${serverip} 'docker image rm -f ${it}'"""
		}
	}
	//复制最新docker-compose.yml到服务器并启动
	sh """scp -P 29050 -r ${env.WORKSPACE}/dockerYml/docker-compose.yml ${serverip}:/usr/local/docker/${project}
		ssh -p 29050 ${serverip} 'cd /usr/local/docker/${project} && docker-compose up -d'"""
}

//复制项目到部署文件夹,线上打包使用
def scpdeployfile(String projectlist) {
	sh """if [ ! -d "${env.WORKSPACE}/deployfile" ];then
			mkdir -p ${env.WORKSPACE}/deployfile/
		else
			rm -rf ${env.WORKSPACE}/deployfile/
			mkdir -p ${env.WORKSPACE}/deployfile/
		fi"""
	def names=projectlist.split(",")
	for(int i=0;i<names.size();i++) {
		sh """mkdir -p ${env.WORKSPACE}/deployfile/${names[i]}"""
		sh """cd ${env.WORKSPACE}/deployfile && cp -r ${env.WORKSPACE}/compfile/${names[i]}/* ${env.WORKSPACE}/deployfile/${names[i]}"""
	}
}

//获取docker-compose.yml文件中所有的镜像名称数组
def getDockerYmlAllImageNames(String ymlPath){
	def initxt = new File(ymlPath).text
	def iniContents = initxt.split("\\n")
	def imageNames = []
	for(int i=0;i<iniContents.size();i++) {
		def line = iniContents[i]
		if(line.contains("image") || line.contains("Image")) {
			def contents = line.split(":")
			imageNames.add(contents[1].replace(" ", "") + ":" + contents[2].replace(" ", ""))
		}
		imageNames.unique()
	}
	return imageNames
}