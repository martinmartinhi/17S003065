#define SERVER_NAME "napd"
#define INFO_MSG  "[信息] >>"
#define OK_MSG    "\033[1;32m[成功] >>\033[0m"
#define ERROR_MSG "\033[1;31m[错误] >>\033[0m"
#define WARN_MSG  "\033[1;33m[警告] >>\033[0m"
#define USER_MSG ">>"

#define DB_FILE "p2pd.sqlite"
#define DEFAULT_PORT "7777"
#define NUM_THREADS 64
#define QUEUE_LENGTH 32

#define MAX_PORT 65535//自定义端口的最大�?
#define PRIVILEGED_PORT 1024
#define TP_UTIL 0.80//线程池的警告阈�?