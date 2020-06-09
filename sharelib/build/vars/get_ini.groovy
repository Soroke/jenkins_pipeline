import hudson.model.*;

def call(String key){
	def sections = read()
	def keys = key.split("\\.")
	if (keys.size() == 2) {
		def section = sections[keys[0]]
		return section[keys[1]]
	}else {
		println "key:'" + key + "'错误"
	}
}

def call(String platformName,String key){
	def sections = read(platformName)
	def keys = key.split("\\.")
	if (keys.size() == 2) {
		def section = sections[keys[0]]
		return section[keys[1]]
	}else {
		println "key:'" + key + "'错误"
	}
}

//读取配置文件，内部方法(出现问题后检查ini配置文件的换行模式是否为LF)
def read() {
	checkIniExists()
	def initxt = new File(env.iniLocalPath).text
	def iniContents = initxt.split("\\n")
	def sections = [:]
	def currentSectionName, properties
	for(int i=0;i<iniContents.size();i++) {
		def line = iniContents[i]
		if(line != '') {
			if(line.startsWith('[') && line.endsWith(']')) {
				currentSectionName=line - '[' - ']'
				properties = [:]
			} else {
				if(line.contains("=")) {
					def kv = line.split('=')
					properties[kv[0].replace(" ", "")] = kv[1].replace(" ", "")
					sections[currentSectionName] = properties
				}
			}
		}
	}
	return sections
}

//读取配置文件(根据项目名称读取不同项目的ini配置文件)，内部方法(出现问题后检查ini配置文件的换行模式是否为LF)
def read(String platformName) {
	def initxt = new File(env.JENKINS_HOME + "/workspace/" + platformName + "_pipeline/ini/" + platformName + ".ini").text
	def iniContents = initxt.split("\\n")
	def sections = [:]
	def currentSectionName, properties
	for(int i=0;i<iniContents.size();i++) {
		def line = iniContents[i]
		if(line != '') {
			if(line.startsWith('[') && line.endsWith(']')) {
				currentSectionName=line - '[' - ']'
				properties = [:]
			} else {
				if(line.contains("=")) {
					def kv = line.split('=')
					properties[kv[0].replace(" ", "")] = kv[1].replace(" ", "")
					sections[currentSectionName] = properties
				}
			}
		}
	}
	return sections
}

//获取所有的项目名称
def getAllProjectName(){
	def sections = read()
	def section = sections['projectlist']
	def allProjectName = ''
	section.each { key, value ->
		allProjectName = allProjectName + value + ','
	}
	return allProjectName.substring(0,(allProjectName.length()-1))
}

//获取指定平台的所有的项目名称
def getAllProjectName(String platformName){
	def sections = read(platformName)
	def section = sections['projectlist']
	def allProjectName = ''
	section.each { key, value ->
		allProjectName = allProjectName + value + ','
	}
	return allProjectName.substring(0,(allProjectName.length()-1))
}

//获取所有的服务中心名称
def getAllServerCenter(){
	def sections = read()
	def section = sections['projectlist']
	def allServerCenter = ''
	section.each { key, value ->
		allServerCenter = allServerCenter + key + ','
	}
	return allServerCenter.substring(0,(allServerCenter.length()-1))
}

//获取指定平台的所有服务中心名称
def getAllServerCenter(String platformName){
	def sections = read(platformName)
	def section = sections['projectlist']
	def allServerCenter = ''
	section.each { key, value ->
		allServerCenter = allServerCenter + key + ','
	}
	return allServerCenter.substring(0,(allServerCenter.length()-1))
}


//获取指定项目所属服务中心名称(解决一个项目部署多个服务器的情况)
def getServiceCenter(String jobName){
	def sections = read()
	def section = sections['projectlist']
	def containsServer = []
	section.each { key, value ->
		def values = value.tokenize(',')
		for(int i=0;i<values.size();i++) {
			if(values[i].equals(jobName)){
				containsServer.add(key)
			}
		}
	}
	return containsServer
}

//获取指定平台下指定项目所属服务中心名称(解决一个项目部署多个服务器的情况)
def getServiceCenter(String platformName,String jobName){
	def sections = read(platformName)
	def section = sections['projectlist']
	def containsServer = []
	section.each { key, value ->
		def values = value.tokenize(',')
		for(int i=0;i<values.size();i++) {
			if(values[i].equals(jobName)){
				containsServer.add(key)
			}
		}
	}
	return containsServer
}

//获取指定服务中心的IP(解决一个服务中心有多个IP的情况，例如：LVS组成的负载)
def getServiceIp(String serverCenter){
	def sections = read()
	def section = sections['server']
	def ips = []
	section.each { key, value ->
		if(key.equals(serverCenter + "_ip")){
			if (value.contains(",")) {
				def valuess = value.tokenize(',')
				valuess.each{ it ->
					ips.add(it)
				}
			} else {
				ips.add(value)
			}
		}
	}
	return ips
}

//获取指定服务中心的IP(解决一个服务中心有多个IP的情况，例如：LVS组成的负载)
//sectionsName 为ini配置文件中标签名称
def getServiceIpSections(String sectionsName,String serverCenter){
	def sections = read()
	def section = sections[sectionsName]
	def ips = []
	section.each { key, value ->
		if(key.equals(serverCenter + "_ip")){
			if (value.contains(",")) {
				def valuess = value.tokenize(',')
				valuess.each{ it ->
					ips.add(it)
				}
			} else {
				ips.add(value)
			}
		}
	}
	return ips
}

//获取指定平台下指定服务中心的IP(解决一个服务中心有多个IP的情况，例如：LVS组成的负载)
//sectionsName 为ini配置文件中标签名称
def getServiceIpSections(String sectionsName,String platformName,String serverCenter){
	def sections = read(platformName)
	def section = sections[sectionsName]
	def ips = []
	section.each { key, value ->
		if(key.equals(serverCenter + "_ip")){
			ips.add(value)
		}
	}
	return ips
}

//获取指定平台下指定服务中心的IP(解决一个服务中心有多个IP的情况，例如：LVS组成的负载)
def getServiceIp(String platformName,String serverCenter){
	def sections = read(platformName)
	def section = sections['server']
	def ips = []
	section.each { key, value ->
		if(key.equals(serverCenter + "_ip")){
			ips.add(value)
		}
	}
	return ips
}


//获取projectlist中项目属于scenter的所有项目
def getCenterProjectlist(String projectlist,String centerName){
	def pn=call("projectlist." + centerName)
	def apn=pn.split(",")
	def deployProjectName=projectlist.split(",")
	def allCenterProject = []
	for(int i=0;i<apn.size();i++){
		for(int j=0;j<deployProjectName.size();j++) {
			if(deployProjectName[j].equals(apn[i])){
				allCenterProject.add(deployProjectName[j])
			}
		}
	}
	return allCenterProject.unique()
}

//获取指定平台下projectlist中项目属于指定服务中心(centerName)的所有项目
def getCenterProjectlist(String platformName,String projectlist,String centerName){
	def pn=call("projectlist." + centerName)
	def apn=pn.split(",")
	def deployProjectName=projectlist.split(",")
	def allCenterProject = []
	for(int i=0;i<apn.size();i++){
		for(int j=0;j<deployProjectName.size();j++) {
			if(deployProjectName[j].equals(apn[i])){
				allCenterProject.add(deployProjectName[j])
			}
		}
	}
	return allCenterProject.unique()
}


//获取指定sheet中所有key的合计
//例如:
//	[server]
//	ip=127.0.0.1
//	path=/usr/local
//getAllKey("server") 返回值为：ip,path
def getAllKey(String sheetName){
	def sections = read()
	def section = sections[sheetName]
	def AllKey = ''
	section.each { key, value ->
		AllKey = AllKey + key + ','
	}
	return AllKey.substring(0,(AllKey.length()-1))
}

//检查ini配置文件参数是否为空且检查ini配置文件是否存在，不存在时异常退出
def checkIniExists() {
	if (!env.iniLocalPath?.trim()) {
		println "Jenkinfile中未配置env.iniLocalPath的环境变量,请配置后重试"
		shell """exit -1"""
	}
	File iniFile = new File(env.iniLocalPath)
	if(!iniFile.exists()){
		println "ini文件不存在,请检查Jenkinsfile文件中是否配置了ini文件的checkout"
		shell """exit -1"""
	}
}
