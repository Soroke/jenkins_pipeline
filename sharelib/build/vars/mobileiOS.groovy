import hudson.model.*;
import java.io.File;

/**
 * 修改iOS的打包版本号
 * 参数：job的workSpace、打包版本号、项目构建ID
 **/
def modifyVersion(String version,String bundleVersion) {

	//获取当前iOS项目的实际名称
	def projectName = getIOSProjectName()
    
	//读取文件信息，并替换指定内容
	def filecontent = new File(WORKSPACE + "/" + projectName + "/Info.plist").text
	def contents  = filecontent.split("\n")
	for(int i=0;i<contents.size();i++) {
		if (contents[i].contains("<key>CFBundleShortVersionString</key>")) {
			contents[i + 1] = "\t<string>" + version + "</string>\n"
			i++;
        } else if (contents[i].contains("<key>CFBundleVersion</key>")) {
            contents[i + 1] = "\t<string>" + bundleVersion + "</string>\n"
        }
	}
	//获取替换完成的内容
	def content = ""
	for(int i=0;i<contents.size();i++) {
		//println contents[i]
		content += (contents[i] + "\n")
	}

	//根据内容替换源文件内容
	def file = new File(WORKSPACE + "/" + projectName + "/Info.plist")
	if(file.exists()) {
		file.delete()
	}
	def printWriter = file.newPrintWriter()
	printWriter.write(content)
	printWriter.flush()
    printWriter.close()

    printColorInfo "打包版本号替换完成"
    
}

/**
 * 获取iOS项目的实际名称，当前项目必须是iOS才能获取
 **/
def getIOSProjectName() {
	def projectName="";
    def files = new File(WORKSPACE).listFiles()
    if(files == null){
    	printColorErr "项目目录下没有文件"
        sh"""exit -1"""
    }
    for(int i=0;i<files.size();i++){
        if(files[i].isDirectory()){
        	//println files[i].getName()
            def contents = files[i].getName().split("[.]")
            if (contents[contents.length-1].equals("xcodeproj")) {
                projectName=contents[0]
                break
                //return path;
            }
        }
    }
    return projectName
}