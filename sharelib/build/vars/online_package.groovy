import hudson.model.*;
import java.io.File;
def call(String platformName,String projectlist,String description,String packageId) {
	def remotepath = "/home/deploy/product_deploy/" + get_ini(platformName,"dir.remotedir") + "/"
	def now_date=new Date().format('yyyyMMdd');
	def miaoshu = common.escapeCharacterShell(description)
	def folderName=now_date + "." + miaoshu
	def dirPath=env.WORKSPACE + "/pack/" + folderName

	sh """if [ ! -d "${env.WORKSPACE}/pack" ];then
			mkdir -p ${env.WORKSPACE}/pack
		fi"""
	File dir = new File(dirPath)
	if (!dir.isDirectory()){
		dir.mkdir();
	} else {
		sh """rm -rf ${dirPath}"""
		dir.mkdir();
	}
	def projectNames=projectlist.split(",")
	//记录项目名称和项目版本号,项目管理系统回调使用
	def projectAndVersionList = ""
	for(int i=0;i<projectNames.size();i++) {
		//对比版本号
		checkSVNVersion(platformName,projectNames[i])
		def serviceCenterName=get_ini.getServiceCenter(platformName,projectNames[i])
		//获取项目的SVN版本号
		def versionFileContent = new File(env.JENKINS_HOME + "/workspace/" + platformName + "_pipeline/deployfile/" + projectNames[i] + "/version.txt").text
		def svnVersion = versionFileContent.split(" ")[1].split("\n")[0]
		projectAndVersionList += (projectNames[i] + "_" + svnVersion + ";")
		for (int sc=0;sc<serviceCenterName.size();sc++) {
			File centerDir = new File(dirPath + "/" + serviceCenterName[sc])
			if (!centerDir.isDirectory()){
				centerDir.mkdir();
			}
			//适配bot项目的线上打包，bot线上打包前需要修改连接地址为线上环境
			if(platformName.equals("bot") && projectNames[i].equals("smartqaui")) {
				common.modifyFile "${env.JENKINS_HOME}/workspace/${platformName}_pipeline/deployfile/smartqaui/static/demo_config.js","var DEMO_URL =","var DEMO_URL = \"http://www.renrenlv.com.cn:81/botps/portal.html?channelNo=0\";"
			}
			sh """cd ${env.JENKINS_HOME}/workspace/${platformName}_pipeline/deployfile/ && tar -zcf ${dirPath}/${serviceCenterName[sc]}/${projectNames[i]}.${svnVersion}.${now_date}.tgz ${projectNames[i]}"""
			//bot打包完成后还原为测试环境地址，防止需要回归版本时部署出错
			if(platformName.equals("bot") && projectNames[i].equals("smartqaui")) {
				common.modifyFile "${env.JENKINS_HOME}/workspace/${platformName}_pipeline/deployfile/smartqaui/static/demo_config.js","var DEMO_URL =","var DEMO_URL = \"http://rrl.t.faxuan.net/botps/portal.html?channelNo=0\";"
			}
		}
	}
	//去除最后一个分隔号
	projectAndVersionList = projectAndVersionList.substring(0,(projectAndVersionList.length()-1))

	sh """ssh -p 29050 deploy@192.168.1.20 'mkdir -p ${remotepath}${folderName}'"""
	sh """scp -P 29050 -r ${dirPath}/* deploy@192.168.1.20:${remotepath}${folderName}"""
	println "平台：" + platformName + ",线上打包完成，安装包已上传到192.168.1.20服务的" + remotepath + folderName + "目录下"
	
	if(packageId != "0") {
		println "开始执行项目管理系统回调"
		println "打包项目对应svn版本：" + projectAndVersionList
		sh """python3 /var/jenkins_home/xmgl_request.py ${packageId} '${projectAndVersionList}' ${remotepath}${folderName}"""
	}

}

//检查打包项目SVN版本号和当前测试环境项目SVN版本号是否一致
def checkSVNVersion(String platformName,String projecName){
	def serverName = get_ini.getServiceCenter(platformName,projecName)
	def serverIP = get_ini(platformName,"server." + serverName[0] + "_ip")
	//serverIP会有多个IP的情况，做一下处理
	if (serverIP.contains(",")) {
		serverIP = serverIP.split(",")[0]
	}
	def deployPath = get_ini(platformName,"server." + serverName[0] + "_deploay_path")
	//创建文件夹，用于拿取当前部署项目的测试环境实际svn版本号文件
	def localPath = env.WORKSPACE + "/tmp/" + platformName + "/" + projecName + "/local"
	def remotePath = env.WORKSPACE + "/tmp/" + platformName + "/" + projecName + "/remote"
	sh """if [ ! -d "${localPath}" ];then
			mkdir -p ${localPath}
		  else
		    rm -rf ${localPath}
		    mkdir -p ${localPath}
		fi"""
		sh """if [ ! -d "${remotePath}" ];then
			mkdir -p ${remotePath}
		  else
		    rm -rf ${remotePath}
		    mkdir -p ${remotePath}
		fi"""
	//判断项目类型,根据不同类型复制测试环境文件到指定目录
	//bot项目单独处理一下
	if (platformName.equals("bot")) {
		sh """scp -P 29050 -r root@${serverIP}:${deployPath}/version.txt ${remotePath}"""
	}else if (new File(JENKINS_HOME + "/workspace/" + platformName + "_pipeline/" + projecName + "/pom.xml").exists() || (new File(JENKINS_HOME + "/workspace/" + platformName + "_pipeline/" + projecName + "/WebRoot").exists() && new File(JENKINS_HOME + "/workspace/" + platformName + "_pipeline/" + projecName + "/build").exists())) {
		sh """scp -P 29050 -r root@${serverIP}:${deployPath}/webapps/${projecName}/version.txt ${remotePath}"""
	} else {
		sh """scp -P 29050 -r root@${serverIP}:${deployPath}/${projecName}/version.txt ${remotePath}"""
	}

	sh """cp ${env.JENKINS_HOME}/workspace/${platformName}_pipeline/deployfile/${projecName}/version.txt ${localPath}"""

	//对比文件
	def remoteversion = new File(remotePath + "/version.txt").text
	def localversion = new File(localPath + "/version.txt").text
	def lversion = localversion.split(" ")[1].split("\n")[0]
	def rversion = remoteversion.split(" ")[1].split("\n")[0]
	println "测试环境实际部署项目" + projecName + "的SVN版本号：" + rversion + ";deployfile下" + projecName + "的SVN版本号为：" + lversion
	if (! lversion.equals(rversion)) {
		println "测试环境实际部署项目SVN版本号和打包项目版本号对比不一致，实际部署项目" + projecName + "的SVN版本号：" + rversion + ";打包版本号为：" + lversion
		sh """exit -1"""
	}
}

// 不需要 groovyx.net.http.HTTPBuilder
// 请求http获取线上版本
def spider_http(mimvp_url) {
	def connection = new URL(mimvp_url).openConnection()
	connection.setRequestMethod('GET')
	connection.doOutput = true.
	connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

	//def writer = new OutputStreamWriter(connection.outputStream)
	//writer.flush()
	//writer.close()
	connection.connect()

	def respText = connection.content.text
	println respText
}
