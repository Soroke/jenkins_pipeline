import hudson.model.*;
def checkout(String url,int type = 0) {
	if (type == 0) {
		
			checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[cancelProcessOnExternalsFail: true, credentialsId: 'eb626597-f71f-4a3a-8b09-9871695adcaf', depthOption: 'infinity', ignoreExternalsOption: true, local: '.', remote: "${url}"]], quietOperation: true, workspaceUpdater: [$class: 'UpdateUpdater']])
		
	}else { 
		log.info "请联系管理员修改common.groovy脚本，新增其他类型的代码检出"
	}
}
//设置描述信息
def setDescription(String description) {
	currentBuild.setDescription(description)
}

//处理线上打包中描述信息中带有shell特殊字符的情况
def escapeCharacterShell(String chars) {
	def replacedChar = chars
	//描述中需要的特殊符号
	def tszf=["(",")","{","}","<",">","&",";","|","#","!","\$","+","-","——","`","%","[","]","^",","]
	for(int i=0;i<tszf.size();i++){
		if (chars.contains(tszf[i])) {
			replacedChar = replacedChar.replaceAll("\\" + tszf[i],"")
		}
	}
	return replacedChar
}

//删除本次部署项目的update.txt文件；项目部署成功后调用该方法执行文件删除
def delectUpdateFile(String projectlist) {
	def deployProjectName=projectlist.split(",")
	for(int i=0;i<deployProjectName.size();i++) {
		//检查项目所在目录下是否有update.txt文件如果有执行文件删除
		def filePath=WORKSPACE + "/" + deployProjectName[i] + "/update.txt"
		File updatefile = new File(filePath)
		if (updatefile.exists()){
			sh """rm -rf ${filePath}"""
		}
	}
}

//删除指定项目的update.txt文件
def delectUpdate(String projectName) {
	//检查项目所在目录下是否有update.txt文件如果有执行文件删除
	def filePath=WORKSPACE + "/" + projectName + "/update.txt"
	File updatefile = new File(filePath)
	if (updatefile.exists()){
		sh """rm -rf ${filePath}"""
	}
}

//检查指定服务器上是否存在镜像
def checkImageExists(String serverip,String imageName) {
	result = sh(script: "ssh -p 29050 ${serverip} 'docker image ls ${imageName} |wc -l'", returnStdout: true).trim()
	//result = sh(script: "docker image ls '${imageName}:latest' |wc -l", returnStdout: true).trim()
	if (result == "2") {
		return true
	} else {
		return false
	}
}

//检查服务器上是否存在镜像
def checkImageExists(String imageName) {
	result = sh(script: "docker image ls ${imageName} |wc -l", returnStdout: true).trim()
	//result = sh(script: "docker image ls '${imageName}:latest' |wc -l", returnStdout: true).trim()
	if (result == "2") {
		return true
	} else {
		return false
	}
}

//修改文件指定内容
def modifyFile(String filePath,String sourceString,String replaceString) {

	//读取文件信息，并替换指定内容
	def filecontent = new File(filePath).text
	def contents  = filecontent.split("\n")
	for(int i=0;i<contents.size();i++) {
		if(contents[i].contains(sourceString) && !contents[i].substring(0,2).equals("//")){
			contents[i] = replaceString + "\n"
		}
	}
	//获取替换完成的内容
	def content = ""
	for(int i=0;i<contents.size();i++) {
		//println contents[i]
		content += (contents[i] + "\n")
	}

	//println content

	//根据内容替换源文件内容
	def file = new File(filePath)
	if(file.exists()) {
		file.delete()
	}
	def printWriter = file.newPrintWriter()
	printWriter.write(content)
	printWriter.flush()
    printWriter.close()

    println "文件：" + filePath + "，内容“" + sourceString + "”替换完成"
    
}


//测试构建基础镜像不指定ip 直接在节点机运行
def buildBaseImage(String platformName,String imageNames){
	def allImages = get_ini.getAllKey(platformName)
	if (imageNames?.trim()) {
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
			if(common.checkImageExists(imageName_n)){
				sh """docker image rm ${imageName_n}"""
			}
			sh """rm -rf /tmp/jenkins_docker_package/${platformName}_${imNames[i]} && mkdir -p /tmp/jenkins_docker_package/${platformName}_${imNames[i]}
			cd ${env.WORKSPACE}/${platformName}_${imNames[i]}/ && docker build -t ${imageName_n} .'
			"""
			//上传基础镜像到Harbor平台
			sh """docker push ${imageName_n}"""
		}
	}
}