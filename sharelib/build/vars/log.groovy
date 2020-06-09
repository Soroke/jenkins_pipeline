def info(message) {
	def now_date=new Date().format('yyyy年MM月dd日');
    echo "${now_date}INFO: ${message}"
}

def warning(message) {
	def now_date=new Date().format('yyyy年MM月dd日');
    echo "${now_date}WARNING: ${message}"
}