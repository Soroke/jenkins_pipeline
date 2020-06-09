import hudson.model.*;
import java.io.File;
//checkout 单独的项目
def call(String url,String platname) {
	def svn_username="redmine"
	def svn_password="redmine"
	def filePath=WORKSPACE + "/" + platname + "/version.txt"
	File versionFile = new File(filePath)
	if(url.contains("@")){
		//清空工程目录
		sh """rm -rf ${env.WORKSPACE}/${platname}"""
		println "工程：" + platname + "指定版本号构建,开始执行"
		checkOut(url,platname)
	} else {
		if (versionFile.exists()){
			sh """cd ${env.WORKSPACE}/${platname} && echo "`svn info ${env.WORKSPACE}/${platname} --username ${svn_username} --password ${svn_password} --show-item last-changed-revision`" > lv.txt"""
			sh """cd ${env.WORKSPACE}/${platname} && echo "`svn info ${url} --username ${svn_username} --password ${svn_password} --show-item last-changed-revision`" > rv.txt"""
			//读取文件中记录的svn版本号并转换为int类型
			def lv = new File(WORKSPACE + "/" + platname + "/lv.txt").text.split("\n")[0].toInteger()
			def rv = new File(WORKSPACE + "/" + platname + "/rv.txt").text.split("\n")[0].toInteger()
			sh """rm -rf ${env.WORKSPACE}/${platname}/lv.txt"""
			sh """rm -rf ${env.WORKSPACE}/${platname}/rv.txt"""
			if(lv < rv) {
				println "项目有更新开始checkout " + platname + "项目"
				checkOut(url,platname)
				return true
			}else{
				println "项目：" + platname + ",未检查到更新。"
				return false
			}
		}else {
			println "首次checkout " + platname + "项目,开始执行"
			checkOut(url,platname)
			return true
		}
	}
}

//checkout多个项目
def deployProject(String projectlist) {
	projects=projectlist.split(",")
	for(int i=0;i<projects.size();i++){
		def projectName=projects[i]
		check(projectName)
	}
}


def check(platname) {
	def svn_username="redmine"
	def svn_password="redmine"
	def url=get_ini("svn." + platname)
	def filePath=WORKSPACE + "/" + platname + "/version.txt"
	File versionFile = new File(filePath)
	if(url.contains("@")){
		//清空工程目录
		sh """rm -rf ${env.WORKSPACE}/${platname}"""
		println "工程：" + platname + "指定版本号构建,开始执行"
		checkOut(url,platname)
	} else {
		if (versionFile.exists()){
			sh """cd ${env.WORKSPACE}/${platname} && echo "`svn info ${env.WORKSPACE}/${platname} --username ${svn_username} --password ${svn_password} --show-item last-changed-revision`" > lv.txt"""
			sh """cd ${env.WORKSPACE}/${platname} && echo "`svn info ${url} --username ${svn_username} --password ${svn_password} --show-item last-changed-revision`" > rv.txt"""
			//读取文件中记录的svn版本号并转换为int类型
			def lv = new File(WORKSPACE + "/" + platname + "/lv.txt").text.split("\n")[0].toInteger()
			def rv = new File(WORKSPACE + "/" + platname + "/rv.txt").text.split("\n")[0].toInteger()
			sh """rm -rf ${env.WORKSPACE}/${platname}/lv.txt"""
			sh """rm -rf ${env.WORKSPACE}/${platname}/rv.txt"""
			if(lv < rv) {
				println "项目有更新开始checkout " + platname + "项目"
				checkOut(url,platname)
			}else{
				println "项目：" + platname + ",未检查到更新。"
			}
		}else {
			println "首次checkout " + platname + "项目,开始执行"
			checkOut(url,platname)
		}
	}
}

def checkOut(String url,String platname) {
	def svn_username="redmine"
	def svn_password="redmine"
	checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[cancelProcessOnExternalsFail: true, credentialsId: "${env.svnUserID}", depthOption: 'infinity', ignoreExternalsOption: true, local: "${platname}", remote: "${url}"]], quietOperation: true, workspaceUpdater: [$class: 'UpdateUpdater']])
	sh """cd ${env.WORKSPACE}/${platname} && echo "${platname} `svn info --username ${svn_username} --password ${svn_password} --show-item last-changed-revision`" > version.txt """
	sh """cd ${env.WORKSPACE}/${platname} && echo "tmp file" > update.txt"""
}

//检查项目是否有更新(需要在项目checkout之后)
def checkUpdate(String platname) {
	//检查项目所在目录下是否有update.txt文件，该文件为checkout时标记项目本次部署有更新
	def filePath=WORKSPACE + "/" + platname + "/update.txt"
	File updatefile = new File(filePath)
	if (updatefile.exists()){
		return true
	} else {
		return false
	}
	return false
}