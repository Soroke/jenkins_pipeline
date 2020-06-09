import hudson.model.*;
import java.io.File;
def call() {
	generateXml()

	generateReport()

	generateTrendGraph()

	setReportView()
}

//生成xml文件
def generateXml() {
	def project = getProjectName()
	//xml文件头部部分
	def xmlFileContent = ""
	xmlFileContent += "<?xml version=\"1.0\" ?>\n"
	xmlFileContent += "<project name=\"statistical\" xmlns:jacoco=\"antlib:org.jacoco.ant\" default=\"jacoco\">\n"
	xmlFileContent += "	<property environment=\"env\"/>\n"
	xmlFileContent += "	<!--Jacoco的安装路径-->\n	<property name=\"jacocoantPath\" value=\"/var/jenkins_home/jacoco/jacocoant.jar\"/>\n\n"
	xmlFileContent += "	<!--最终生成.exec文件的路径，Jacoco就是根据这个文件生成最终的报告的-->\n	"
	xmlFileContent += "<property name=\"jacocoexecPath\" value=\"/var/jenkins_home/jacoco/generateFile/" + project + "/jacoco.exec\"/>\n\n"
	xmlFileContent += "	<!--生成覆盖率报告的路径-->\n	<property name=\"reportfolderPath\" value=\"/var/jenkins_home/jacoco/generateFile/" + project + "/report/\"/>\n\n"
	//获取服务IP和端口
	def coverageServer = get_ini("coverage.server_ip")
	def servers = coverageServer.split(",")
	def coveragePort = get_ini("coverage.port")
	xmlFileContent += "	<!--远程tomcat服务的ip地址-->\n"
	for (int i=0;i<servers.size();i++) {
		xmlFileContent += "	<property name=\"server_ip" + (i+1) + "\" value=\"" + get_ini("server." + servers[i] + "_ip") + "\"/>\n"
	}
	xmlFileContent += "\n	<!--前面配置的远程tomcat服务打开的端口，要跟上面配置的一样-->\n"
	xmlFileContent += "	<property name=\"server_port\" value=\"" + coveragePort + "\"/>\n\n"
	xmlFileContent += "	<!--源代码和classes路径-->\n"

	//检查服务中心所有工程是否存在源代码和class文件
	def projectlist = ""
	//获取所有覆盖率项目名称
	for(int i=0;i<servers.size();i++) {
		projectlist = projectlist + get_ini("projectlist." + servers[i]) + ","
	}
	//去除最后一个,
	projectlist = projectlist.substring(0,(projectlist.length()-1))

	//println projectlist
	//检查源代码和class
	checksourceCodeAndCompile(project,projectlist)
	//复制文件
	copyFile(project,projectlist)

	def lists = projectlist.split(",")
	for(int i=0;i<lists.size();i++) {
		xmlFileContent += "	<property name=\"" + lists[i] + ".src\" value=\"/var/jenkins_home/workspace/" + project + "_pipeline/coverage/sourcecode/" + lists[i] + "\"/>\n"
		xmlFileContent += "	<property name=\"" + lists[i] + ".classes\" value=\"/var/jenkins_home/workspace/" + project + "_pipeline/coverage/compfile/" + lists[i] + "\"/>\n"
	}
	xmlFileContent += "\n	<!--让ant知道去哪儿找Jacoco-->\n"
	xmlFileContent += "	<taskdef uri=\"antlib:org.jacoco.ant\" resource=\"org/jacoco/ant/antlib.xml\">\n"
	xmlFileContent += "		<classpath path=\"\${jacocoantPath}\" />\n"
	xmlFileContent += "	</taskdef>\n\n"
	xmlFileContent += "	<!--dump任务:根据前面配置的ip地址，和端口号，访问目标tomcat服务，并生成.exec文件。-->\n"
	xmlFileContent += "	<target name=\"dump\">\n"
	for (int i=0;i<servers.size();i++) {
		xmlFileContent += "		<jacoco:dump address=\"\${server_ip" + (i+1) + "}\" reset=\"false\" destfile=\"\${jacocoexecPath}\" port=\"\${server_port}\" append=\"true\"/>\n"
	}
	xmlFileContent += "	</target>\n\n"
	xmlFileContent += "	<!--jacoco任务:根据前面配置的源代码路径和.class文件路径，根据dump后，生成的.exec文件，生成最终的html覆盖率报告。-->\n"

	xmlFileContent += "	<target name=\"report\">\n"
	xmlFileContent += "		<delete dir=\"\${reportfolderPath}\" />\n"
	xmlFileContent += "		<mkdir dir=\"\${reportfolderPath}\" />\n"
	xmlFileContent += "		<jacoco:report>\n"
	xmlFileContent += "			<executiondata>\n"
	xmlFileContent += "				<file file=\"\${jacocoexecPath}\" />\n"
	xmlFileContent += "			</executiondata>\n\n"
	xmlFileContent += "			<structure name=\"测试覆盖率报告\">\n"
	for(int i=0;i<lists.size();i++) {
		xmlFileContent += "				<group name=\""+ lists[i] + "\">\n"
		xmlFileContent += "					<classfiles>\n"
		xmlFileContent += "						<fileset dir=\"\${" + lists[i] + ".classes}\" />\n"
		xmlFileContent += "					</classfiles>\n"
		xmlFileContent += "					<sourcefiles encoding=\"utf-8\">\n"
		xmlFileContent += "						<fileset dir=\"\${" + lists[i] + ".src}\" />\n"
		xmlFileContent += "					</sourcefiles>\n"
		xmlFileContent += "				</group>\n"
	}

	xmlFileContent += "			</structure>\n"
	xmlFileContent += "			<html destdir=\"\${reportfolderPath}\" encoding=\"utf-8\" />\n"
	xmlFileContent += "		</jacoco:report>\n"
	xmlFileContent += "	</target>\n"
	xmlFileContent += "</project>"

	//println xmlFileContent
	//生成xml文件到当前工作目录
	def file = new File(WORKSPACE + "/build.xml")
	if(file.exists()){
		file.delete();
	}
	file.write(xmlFileContent)

	//sh """cd ${WORKSPACE} && touch ${WORKSPACE}/build.xml && echo ${xmlFileContent}"""
	//sh """echo ${xmlFileContent} > ${WORKSPACE}/build.xml"""
}

//检查指定工程是否有生成覆盖率报告的源码和class文件,
def checksourceCodeAndCompile(String project,String list) {
	//def allProj = get_ini.getAllProjectName(project)
	def lists = list.split(",")
	for(int i=0; i<lists.size();i++) {
		if(! new File("/var/jenkins_home/workspace/" + project + "_pipeline/coverage/sourcecode/"+ lists[i] + "/").exists() || ! new File("/var/jenkins_home/workspace/" + project + "_pipeline/coverage/compfile/"+ lists[i] + "/").exists()){
			sh """echo '工程：${lists[i]}的源码或编译完成的class代码不存在,无法生成覆盖率.请在重新部署之后再生成覆盖率。' && exit -1"""
		}
	}
}

//复制当前项目指定工程class文件和源码到当前工程下
def copyFile(String project,String list) {
	//def allProj = get_ini.getAllProjectName(project)
	def lists = list.split(",")
	sh """rm -rf ${WORKSPACE}/sourcecode && mkdir ${WORKSPACE}/sourcecode
		rm -rf ${WORKSPACE}/compfile && mkdir ${WORKSPACE}/compfile"""
	for(int i=0; i<lists.size();i++) {
		sh """mkdir -p ${WORKSPACE}/sourcecode/${lists[i]} && cp -r /var/jenkins_home/workspace/${project}_pipeline/coverage/sourcecode/${lists[i]}/* ${WORKSPACE}/sourcecode/
			mkdir -p ${WORKSPACE}/compfile/${lists[i]} && cp -r /var/jenkins_home/workspace/${project}_pipeline/coverage/compfile/${lists[i]}/* ${WORKSPACE}/compfile/"""
	}
}

//生成报告
def generateReport() {
	//生成jacoco.exec备份
	sh """ant dump -buildfile ${WORKSPACE}/build.xml"""
	//根据xml文件生成覆盖率报告
	sh """ant report -buildfile ${WORKSPACE}/build.xml"""
}

//生成趋势图
def generateTrendGraph() {
	def project = getProjectName()
	//复制生成exec文件和report到当前job的workspace下
	sh """rm -rf ${WORKSPACE}/jacoco.exec
		cp -rf /var/jenkins_home/jacoco/generateFile/${project}/jacoco.exec ${WORKSPACE}
		rm -rf ${WORKSPACE}/report
		cp -rf /var/jenkins_home/jacoco/generateFile/${project}/report ${WORKSPACE}"""
	//生成jacoco的趋势图
	jacoco classPattern: 'compfile', execPattern: 'jacoco.exec', sourcePattern: 'sourcecode'
}

//设置报告的展示
def setReportView() {
	//设置HTML报告的展示
	publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: '', reportFiles: 'report/index.html', reportName: '测试覆盖率报告', reportTitles: '覆盖率全局报告'])
}

//获取项目名称简称
def getProjectName() {
	return get_ini("dir.project")
}

//清理当前覆盖率
def isCleanCoverageReport(String clean){
	def projectName = getProjectName()
	if(clean.equals("YES")) {
		sh """rm -rf /var/jenkins_home/jacoco/generateFile/${projectName}/jacoco.exec"""
	}
}