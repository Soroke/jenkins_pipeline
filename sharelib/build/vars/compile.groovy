import hudson.model.*;
import java.io.File;

//执行maven编译命令
def compileMavenFrame(String projectName) {
	if(checkout_svn.checkUpdate(projectName)) {
		sh """cd ${env.WORKSPACE}/${projectName} && mvn clean package install"""
	}else {
		println "项目：" + projectName + "本次部署没有更新,故不再重复编译。"
	}
}

//用于单独构建service和ioif框架的方法
def compileAntFrame(String projectName) {
	job = JOB_NAME.split("_")
	jobname = job[0]
	sh """cd ${env.WORKSPACE}/${projectName} && ant -buildfile /var/jenkins_home/antxml/javaant.xml -Dprojectname=${projectName} -Dplatname=${jobname}"""
}

//判断当前工程是否包含pom文件，如果包含执行maven编译,如果不包含 再判断是否包含src目录，如果包含即为ant后台项目执行ant编译
def ht(String projectlist) {
	projects=projectlist.split(",")
	for(int i=0;i<projects.size();i++){
		def projectName=projects[i]
		//def center = get_ini.getServiceCenter(projectName)
		//判断当前工程的类型（pom.xml为maven项目，build.xml为ant编译项目，其他为前端项目）
		if (new File(WORKSPACE + "/" + projectName + "/pom.xml").exists()) {
			compileMaven(projectName)
		} else if (new File(WORKSPACE + "/" + projectName + "/src").exists() && new File(WORKSPACE + "/" + projectName + "/WebRoot").exists() && ! new File(WORKSPACE + "/" + projectName + "/index.html").exists()) {
			//适配法宣商城fxpay项目的部署
			def proNa = get_ini("dir.project")
			if (proNa.equals("fxshop") && projectName.equals("fxpay")) {
				compileAntFXShopFxPay(projectName)
			} else{
				compileAntHT(projectName)
			}
		}
	}
}
//判断当前工程是否包含pom文件，如果不包含 再判断是否包含src目录，如果包含即为ant后台项目执行ant编译
/**def antht(String projectlist) {
	projects=projectlist.split(",")
	for(int i=0;i<projects.size();i++){
		def projectName=projects[i]
		//def center = get_ini.getServiceCenter(projectName)
		//判断当前工程的类型（pom.xml为maven项目，build.xml为ant编译项目，其他为前端项目）
		if (! new File(WORKSPACE + "/" + projectName + "/pom.xml").exists() && (new File(WORKSPACE + "/" + projectName + "/src").exists() && new File(WORKSPACE + "/" + projectName + "/src").isDirectory())) {
			compileAntHT(projectName)
		}
	}
}**/

//判断不包含pom文件，切不包含src目录，即为ant前端项目执行ant编译
def qd(String projectlist) {
	projects=projectlist.split(",")
	for(int i=0;i<projects.size();i++){
		def projectName=projects[i]
		//def center = get_ini.getServiceCenter(projectName)
		//判断当前工程的类型（pom.xml为maven项目，build.xml为ant编译项目，其他为前端项目）
		if (! new File(WORKSPACE + "/" + projectName + "/pom.xml").exists() && ! new File(WORKSPACE + "/" + projectName + "/WebRoot").exists() && ! new File(WORKSPACE + "/" + projectName + "/build").exists()) {
			compileAntQD(projectName)
		}
	}
}

//执行maven编译命令，编译完成后复制文件到${env.WORKSPACE}/compile/${projectName}目录
def compileMaven(String projectName) {
	if(checkout_svn.checkUpdate(projectName)) {
		sh """cd ${env.WORKSPACE}/${projectName} && mvn clean package install"""
		//检查编译完成后target文件夹下的目录是否存在，存在copy到compile目录下
		sh """if [ ! -d "${env.WORKSPACE}/compfile/${projectName}" ];then
					mkdir -p ${env.WORKSPACE}/compfile/${projectName}
				else
					rm -rf ${env.WORKSPACE}/compfile/${projectName}
					mkdir -p ${env.WORKSPACE}/compfile/${projectName}
				fi"""
		def dirPath_version=WORKSPACE + "/" + projectName + "/target/" + projectName + "-1.0.0-SNAPSHOT"
		//def dirPath_no_version=WORKSPACE + "/" + projectName + "/target/" + projectName
		File targetFile_v = new File(dirPath_version)
		//File targetFile_nv = new File(dirPath_no_version)
		if (targetFile_v.exists()){
			sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${projectName}/target/${projectName}-1.0.0-SNAPSHOT/* ${env.WORKSPACE}/compfile/${projectName} && cp -r ${env.WORKSPACE}/${projectName}/version.txt ${env.WORKSPACE}/compfile/${projectName}"""
		} else{
			sh """cd ${env.WORKSPACE}/compfile && cp -r ${env.WORKSPACE}/${projectName}/target/${projectName}/* ${env.WORKSPACE}/compfile/${projectName} && cp -r ${env.WORKSPACE}/${projectName}/version.txt ${env.WORKSPACE}/compfile/${projectName}"""
		}
	}else {
		println "项目：" + projectName + "本次部署没有更新,故不再重复编译。"
	}
}

//执行ant后台项目编译命令，编译完成后复制文件到${env.WORKSPACE}/compile/${projectName}目录
def compileAntHT(String projectName) {
	if(checkout_svn.checkUpdate(projectName)) {
		//云平台rdt项目比较特殊单独处理一下
		if(projectName.equals("rdt")) {
			sh """cd ${env.WORKSPACE}/${projectName} && ant"""
		} else if(projectName.equals("appbss")) {
			job = JOB_NAME.split("_")
			jobname = job[0]
			sh """
			if  [ ! -d "${env.WORKSPACE}/service" ] || [ ! -d "${env.WORKSPACE}/appbssioif" ];then
				echo "frame not exit！"
			    exit 1 
			fi
			cp -rf ${env.WORKSPACE}/service/src/com/service ${env.WORKSPACE}/${projectName}/src/com
			cp -rf ${env.WORKSPACE}/appbssioif/src/com/ioif ${env.WORKSPACE}/${projectName}/src/com
			cd ${env.WORKSPACE}/${projectName} && ant -buildfile /var/jenkins_home/antxml/javaant.xml -Dprojectname=${projectName} -Dplatname=${jobname}"""
		}else{
			job = JOB_NAME.split("_")
			jobname = job[0]
			sh """
			if  [ ! -d "${env.WORKSPACE}/service" ] || [ ! -d "${env.WORKSPACE}/ioif" ];then
				echo "frame not exit！"
			    exit 1 
			fi
			cp -rf ${env.WORKSPACE}/service/src/com/service ${env.WORKSPACE}/${projectName}/src/com
			cp -rf ${env.WORKSPACE}/ioif/src/com/ioif ${env.WORKSPACE}/${projectName}/src/com
			cd ${env.WORKSPACE}/${projectName} && ant -buildfile /var/jenkins_home/antxml/javaant.xml -Dprojectname=${projectName} -Dplatname=${jobname}"""
		}
	}else {
		println "项目：" + projectName + "本次部署没有更新,故不再重复编译。"
	}
}

//该方法目前仅有电商的fxpay项目使用
//执行ant后台项目编译命令，编译完成后复制文件到${env.WORKSPACE}/compile/${projectName}目录
def compileAntFXShopFxPay(String projectName) {
	if(checkout_svn.checkUpdate(projectName)) {
		//云平台rdt项目比较特殊单独处理一下
		if(projectName.equals("rdt")) {
			sh """cd ${env.WORKSPACE}/${projectName} && ant"""
		} else {
			job = JOB_NAME.split("_")
			jobname = job[0]
			sh """
			if  [ ! -d "${env.WORKSPACE}/service" ] || [ ! -d "${env.WORKSPACE}/ioif" ];then
				echo "frame not exit！"
			    exit 1 
			fi
			cd ${env.WORKSPACE}/${projectName} && ant -buildfile /var/jenkins_home/antxml/javaant2.xml -Dprojectname=${projectName} -Dplatname=${jobname}"""
		}
	}else {
		println "项目：" + projectName + "本次部署没有更新,故不再重复编译。"
	}
}

//执行ant前端项目编译命令，编译完成后复制文件到${env.WORKSPACE}/compile/${projectName}目录
def compileAntQD(String projectName) {
	if(checkout_svn.checkUpdate(projectName)) {
		//云平台的cps项目和法考网的fkwps项目需要使用grunt编译，故不作编译
		//if(projectName.equals("cps") || projectName.equals("fkwps")) {
		if(projectName.equals("cps") || projectName.equals("fkwps")) {
			sh """
			#npm install -g grunt-cli
			#使用grunt编译代码增加版本号
			#清理jenkins的job下的文件夹和grunt工具目录下的文件夹
			rm -rf ${env.WORKSPACE}/compfile/${projectName} && mkdir -p ${env.WORKSPACE}/compfile/${projectName}
			if [ ${projectName} = "fkwps" ];then
				rm -rf ${env.JENKINS_HOME}/automatic-version-increment-master-fkwps/${projectName}
				cp -R ${env.WORKSPACE}/${projectName} ${env.JENKINS_HOME}/automatic-version-increment-master-fkwps/
				cd ${env.JENKINS_HOME}/automatic-version-increment-master-fkwps/
				#执行编译
				grunt
				#编译完成复制编译完成文件到compose文件夹下
				cp -R ${env.JENKINS_HOME}/automatic-version-increment-master-fkwps/${projectName}/* ${env.WORKSPACE}/compfile/${projectName}
			elif [ ${projectName} = "cps" ];then
				rm -rf ${env.JENKINS_HOME}/automatic-version-increment-master-yuncps/${projectName}
				cp -R ${env.WORKSPACE}/${projectName} ${env.JENKINS_HOME}/automatic-version-increment-master-yuncps/
				cd ${env.JENKINS_HOME}/automatic-version-increment-master-yuncps/
				#执行编译
				grunt
				#编译完成复制编译完成文件到compose文件夹下
				cp -R ${env.JENKINS_HOME}/automatic-version-increment-master-yuncps/${projectName}/* ${env.WORKSPACE}/compfile/${projectName}
			fi
			"""
			//普法工作子平台的pfbase 法考网的fkwms不需要压缩编译
		}else if(projectName.equals("pfbase") || projectName.equals("fkwms")) {
			//仅复制项目到编译目录供部署使用，防止报错
			sh """rm -rf ${env.WORKSPACE}/compfile/${projectName} && mkdir -p ${env.WORKSPACE}/compfile/${projectName}
			cp -r ${env.WORKSPACE}/${projectName}/* ${env.WORKSPACE}/compfile/${projectName}"""
		} else {
			sh """cd ${env.WORKSPACE}/${projectName} && ant -buildfile /var/jenkins_home/antxml/jsant.xml -Dprojectname=${projectName}"""
		}
	} else {
		println "项目：" + projectName + "本次部署没有更新,故不再重复编译。"
	}
}

/**
def compileVue(String projectName) {
	if(checkout_svn.checkUpdate(projectName)) {
		bat """set PATH=%PATH%;D:\soft\nodejs
			cd %WORKSPACE%/%projectName%
			npm install
			rd /s /Q dist
			md dist
			npm run build"""
	}
}**/
