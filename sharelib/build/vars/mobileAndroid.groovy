import hudson.model.*;
import java.io.File;

/**
 * 修改Android的打包版本号
 **/
def modifyVersion(String version) {
	//分割版本号,获取版本号实际数字
	def appCode="";
	def codes = version.split("\\.");
	codes.each {
		appCode += it
	}

	//读取文件信息，并替换指定内容
	def filecontent = new File(WORKSPACE + "/code/gradle.properties").text
	def contents  = filecontent.split("\n")
	for(int i=0;i<contents.size();i++) {
		if(contents[i].contains("APP_VERSION=")){
			contents[i] = "APP_VERSION=" + version + "\n"
		}else if (contents[i].contains("APP_CODE=")) {
			contents[i] = "APP_CODE=" + appCode + "\n"
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
	def file = new File(WORKSPACE + "/code/gradle.properties")
	if(file.exists()) {
		file.delete()
	}
	def printWriter = file.newPrintWriter()
	printWriter.write(content)
	printWriter.flush()
    printWriter.close()

    println "打包版本号替换完成"
    
}

/**
 * 修改Android打渠道包
 **/
def modifyChannel() {

	//读取文件信息，并替换指定内容
	def filecontent = new File(WORKSPACE + "/app/src/main/AndroidManifest.xml").text
	def contents  = filecontent.split("\n")
	for(int i=0;i<contents.size();i++) {
		if(i<(contents.size()-1) && contents[i].contains("<meta-data--") && contents[i+1].contains("android:name=\"UMENG_CHANNEL\"")){
			contents[i] = "        <meta-data\n"
		}else if (i<(contents.size()-1) && contents[i].contains("android:name=\"UMENG_CHANNEL\"") && contents[i+1].contains("android:value=\"\${UMENG_CHANNEL_VALUE}\" />")) {
			contents[i] = "            android:name=\"UMENG_CHANNEL\"\n"
		}else if (i!=0 && contents[i].contains("android:value=\"\${UMENG_CHANNEL_VALUE}\" />") && contents[i-1].contains("android:name=\"UMENG_CHANNEL\"")) {
			contents[i] = "            android:value=\"\${UMENG_CHANNEL_VALUE}\" />\n"
		}
	}

	//获取替换完成的内容
	def content = ""
	for(int i=0;i<contents.size();i++) {
		//println contents[i]
		content += (contents[i] + "\n")
	}

	//根据内容替换源文件内容
	def file = new File(WORKSPACE + "/app/src/main/AndroidManifest.xml")
	if(file.exists()) {
		file.delete()
	}
	def printWriter = file.newPrintWriter()
	printWriter.write(content)
	printWriter.flush()
    printWriter.close()

    //读取文件信息，并替换指定内容
	def filecontent = new File(WORKSPACE + "/app/build.gradle").text
	def contents  = filecontent.split("\n")
	for(int i=0;i<contents.size();i++) {
		if(contents[i].contains("productFlavors {") && contents[i].contains("//")){
			contents[i] = "    productFlavors {\n"
		}else if (contents[i].contains("xiaomi {}") && contents[i].contains("//")) {
			contents[i] = "        xiaomi {}\n"
		}else if (contents[i].contains("_360 {}") && contents[i].contains("//")) {
			contents[i] = "        _360 {}\n"
		}else if (contents[i].contains("baidu {}") && contents[i].contains("//")) {
			contents[i] = "        baidu {}\n"
		}else if (contents[i].contains("wandoujia {}") && contents[i].contains("//")) {
			contents[i] = "        wandoujia {}\n"
		}else if (contents[i].contains("oppo {}") && contents[i].contains("//")) {
			contents[i] = "        oppo {}\n"
		}else if (contents[i].contains("vivo {}") && contents[i].contains("//")) {
			contents[i] = "        vivo {}\n"
		}else if (contents[i].contains("tencent {}") && contents[i].contains("//")) {
			contents[i] = "        tencent {}\n"
		}else if (contents[i].contains("huawei {}") && contents[i].contains("//")) {
			contents[i] = "        huawei {}\n"
		}else if ( i!=0 && contents[i-1].contains("huawei {}") && contents[i].contains("//") && contents[i].contains("}")) {
			contents[i] = "    }\n"
		}else if (contents[i].contains("productFlavors.all {") && contents[i].contains("//")) {
			contents[i] = "    productFlavors.all {\n"
		}else if (contents[i].contains("flavor -> flavor.manifestPlaceholders = [UMENG_CHANNEL_VALUE: name]") && contents[i].contains("//")) {
			contents[i] = "        flavor -> flavor.manifestPlaceholders = [UMENG_CHANNEL_VALUE: name]\n"
		}else if ( i!=0 && contents[i-1].contains("flavor -> flavor.manifestPlaceholders = [UMENG_CHANNEL_VALUE: name]") && contents[i].contains("//") && contents[i].contains("}")) {
			contents[i] = "    }\n"
		}
	}

	//获取替换完成的内容
	def content = ""
	for(int i=0;i<contents.size();i++) {
		//println contents[i]
		content += (contents[i] + "\n")
	}

	//根据内容替换源文件内容
	def file = new File(WORKSPACE + "/app/build.gradle")
	if(file.exists()) {
		file.delete()
	}
	def printWriter = file.newPrintWriter()
	printWriter.write(content)
	printWriter.flush()
    printWriter.close()

    println "打包版本号替换完成"
    
}

def modifyEnv() {
	
}