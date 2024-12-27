package com.keyolla.bugclosure

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.LinkedBlockingQueue

/**
 * 로그 통합관리
 */
class BugClosure {

    companion object {
        // 화면 출력 여부
        private var mIsLogPrint = true
        // 로그 파일 생성 여부
        private var mIsLogFileSave = false

        // Lifecycle 로그 출력 여부
        private var mIsLifecycleLog = false

        // View  TouchEvent 로그 출력 여부
        private var mIsViewTouchEventLog = false

        // 로그 TAG
        private var LOG_TAG = "BugClosure"


        // const area
        private const val LOG_MAX_INDEX = 3000
        private const val LOG_INDENT = "    "

        // LOG Level
        private const val LOG_TYPE_DEBUG = 0
        private const val LOG_TYPE_VERBOSE = 1
        private const val LOG_TYPE_INFO = 2
        private const val LOG_TYPE_WARN = 3
        private const val LOG_TYPE_ERROR = 4



        // Application Context
        private var mApplicationContext : Context? = null

        // 로그 파일 정보
        private var mLogFilePath =  "BugClosure"
        private var mLogFileName =  "BugClosure"
        private var mLogFileSize : Long = (1024 * 1024) * 10


        // 로그 큐와 작성기
        private val logQueue = LinkedBlockingQueue<String>()


        // 로그 파일에 출력시 시간 정보 형식
        private var mLogPrintDateFormat: String = "yyyy-MM-dd HH:mm:ss.SSS"
        private val mApplicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var mCurrentWriter: OutputStreamWriter? = null
        private val mFileLock = Any()
        private var mCurrentFileUri: Uri? = null

        /**
         * initialize BugClosure
         * @param context   :   ApplicationContext
         */
        fun init(applicationContext: Context) = apply {
            mApplicationContext = applicationContext

            if (mApplicationContext is Application) {
                val app = mApplicationContext as Application
                app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(activity: Activity, p1: Bundle?) {

                        if(mIsViewTouchEventLog)
                        {
                            activity.window.callback = TouchEventLogger(activity.window.callback, activity)
                        }

                        if(mIsLifecycleLog)
                            i("${activity.localClassName} onActivityCreated ")
                    }

                    override fun onActivityStarted(activity: Activity) {
                        if(mIsLifecycleLog)
                            i("${activity.localClassName} onActivityStarted ")
                    }

                    override fun onActivityResumed(activity: Activity) {
                        if(mIsLifecycleLog)
                            i("${activity.localClassName} onActivityResumed ")
                    }

                    override fun onActivityPaused(activity: Activity) {
                        if(mIsLifecycleLog)
                            i("${activity.localClassName} onActivityPaused ")

                        mApplicationScope.launch { flushBuffer() }
                    }

                    override fun onActivityStopped(activity: Activity) {
                        if(mIsLifecycleLog)
                            i("${activity.localClassName} onActivityStopped ")
                        mApplicationScope.launch { flushBuffer() }
                    }

                    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {
                        if(mIsLifecycleLog)
                            i("${activity.localClassName} onActivitySaveInstanceState ")

                    }

                    override fun onActivityDestroyed(activity: Activity) {
                        if(mIsLifecycleLog)
                            i("${activity.localClassName} onActivityDestroyed ")

                        mApplicationScope.launch { flushBuffer() }
                    }

                })

            }
        }

        /**
         * 로그 출력 설정
         * @param isPrint  :   로그 출력 여부
         */
        fun setLogPrint(isPrint: Boolean) = apply {
            mIsLogPrint = isPrint
        }

        /**
         * 로그 파일 생성 설정
         * @param isLogFileSave  :   로그 파일 생성 여부
         */
        fun setSaveLogFile(isLogFileSave: Boolean) = apply {
            mIsLogFileSave = isLogFileSave
        }

        /**
         * Lifecycle 로그 출력 여부
         * @param isLifecycleLog  :   Lifecycle 로그 출력 여부
         */
        fun setLifecycleLog(isLifecycleLog: Boolean) = apply {
            mIsLifecycleLog = isLifecycleLog
        }


        /**
         * ViewTouchEvent 로그 출력 여부
         * @param isViewTouchEventLog  :   ViewTouchEvent 로그 출력 여부
         */
        fun setViewTouchEventLog(isViewTouchEventLog: Boolean) = apply {
            mIsViewTouchEventLog = isViewTouchEventLog
        }


        /**
         * 로그 파일 정보 설정
         * @param filePath   : 로그 파일 저장 위치
         * @param fileName   : 로그 파일 이름
         * @param fileSizeMB   : 로그 파일 사이즈
         */
        fun setLogFileInformation(filePath : String, fileName : String, fileSizeMB : Int) = apply {
            mLogFilePath = filePath
            mLogFileSize = ((1024 * 1024) * fileSizeMB).toLong()
            mLogFileName = fileName
            mApplicationContext?.let {
                if(mIsLogFileSave)
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        startLogProcessing()
                }
            }
        }





        /**
         * 로그 파일에 출력시 Time 정보
         * @param format    :   format  기본형 "yyyy-MM-dd HH:mm:ss.SSS"
         */
        fun setLogPrintDateFormat(format: String) {
            mLogPrintDateFormat = format;
        }


        fun d(msg: String) = logMessage(msg, LOG_TYPE_DEBUG)


        fun v(msg: String) = logMessage(msg, LOG_TYPE_VERBOSE)


        fun i(msg: String) = logMessage(msg, LOG_TYPE_INFO)


        fun w(msg: String) = logMessage(msg, LOG_TYPE_WARN)


        fun e(msg: String) = logMessage(msg, LOG_TYPE_ERROR)



        /**
         * Data 통신
         * @param jsonString    :   format  기본형 "yyyy-MM-dd HH:mm:ss"
         */
        fun getJsonStringPretty(jsonString: String?): String {
            if (jsonString.isNullOrEmpty()) {
                e("jsonString is null")
                return ""
            }

            val prettyJsonSb = StringBuilder()
            var indentDepth = 0
            jsonString.forEach { char ->
                when (char) {
                    '{', '[' -> {
                        prettyJsonSb.append(char).append("\n").append(LOG_INDENT.repeat(++indentDepth))
                    }
                    '}', ']' -> {
                        prettyJsonSb.append("\n").append(LOG_INDENT.repeat(--indentDepth)).append(char)
                    }
                    ',' -> {
                        prettyJsonSb.append(char).append("\n").append(LOG_INDENT.repeat(indentDepth))
                    }
                    else -> prettyJsonSb.append(char)
                }
            }
            return prettyJsonSb.toString()
        }

        private fun logMessage(msg: String, logType: Int) {
            if (mIsLogPrint) logLong(msg, logType)
            else
            {
                // 화면에 노출은 하지 않고 파일 로그만 남길경우
                if(mIsLogFileSave)
                    logLong(msg, logType)
            }
        }

        private fun getWithMethodName(log: String): String {
            return try {
                val ste = Thread.currentThread().stackTrace[7]
                "[${ste.fileName.replace(".java", "")}::${ste.methodName}] $log"
            } catch (e: Throwable) {
                log
            }
        }


        private fun logLong(msg: String, logType: Int) {
            if (msg.length > LOG_MAX_INDEX) {
                logChunk(msg.substring(0, LOG_MAX_INDEX), logType)
                logLong(msg.substring(LOG_MAX_INDEX), logType)
            } else {
                logChunk(msg, logType)
            }
        }

        private fun logChunk(msg: String, logType: Int) {
            val logMsg = getWithMethodName(msg)
            try {
                when (logType) {
                    LOG_TYPE_DEBUG -> {
                        Log.d(LOG_TAG, logMsg)
                        saveFileLog( msg, logType)

                    }

                    LOG_TYPE_VERBOSE -> {
                        Log.v(LOG_TAG, logMsg)
                        saveFileLog( msg, logType)
                    }

                    LOG_TYPE_INFO -> {
                        Log.i(LOG_TAG, logMsg)
                        saveFileLog( msg, logType)
                    }

                    LOG_TYPE_WARN -> {
                        Log.w(LOG_TAG, logMsg)
                        saveFileLog( msg, logType)
                    }

                    LOG_TYPE_ERROR -> {
                        Log.e(LOG_TAG, logMsg)
                        saveFileLog( msg, logType)
                    }
                }
            } catch (_: Exception) {
                // Silent fail
            }
        }


        private fun getNowDateString() : String {
            return SimpleDateFormat(mLogPrintDateFormat).format(Calendar.getInstance().time).toString()
        }

        private fun saveFileLog(msg: String, level: Int) {
            try {
                if(!mIsLogFileSave)
                    return

                val printMsg = buildString {
                    append(getNowDateString())
                    append(
                        when (level) {
                            LOG_TYPE_DEBUG -> " [DEBUG]"
                            LOG_TYPE_VERBOSE -> " [VERBOSE]"
                            LOG_TYPE_INFO -> " [INFO]"
                            LOG_TYPE_WARN -> " [WARN]"
                            LOG_TYPE_ERROR -> " [ERROR]"
                            else -> ""
                        }
                    )
                    append(" $msg")
                }
                mApplicationScope.launch {
                    logQueue.put(printMsg)
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun startLogProcessing() {
            mApplicationScope.launch {
                while (isActive) {
                    writeLogToFile(logQueue.take())
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private suspend fun writeLogToFile(message: String) {
            withContext(Dispatchers.IO) {
                synchronized(mFileLock) {
                    if (mCurrentFileUri == null || checkIfFileExceedsSize( )){
                        mCurrentWriter?.close()
                        mCurrentWriter = createNewLogFile()
                    }

                    mCurrentWriter?.apply {
                        write("$message\n")
                        flush()
                    }

                }
            }
        }

        private fun checkIfFileExceedsSize(): Boolean {
            try {
                mApplicationContext!!.contentResolver.openInputStream(mCurrentFileUri!!)?.use { inputStream ->
                    val fileSize = inputStream.available().toLong()
                    return fileSize > mLogFileSize
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }



        private suspend fun flushBuffer() {
            withContext(Dispatchers.IO) {
                synchronized(mFileLock) {
                    mCurrentWriter?.flush()
                }
            }
        }

        fun closeLogger() {
            mApplicationScope.cancel()
            runBlocking {
                flushBuffer()
            }
            mCurrentWriter?.close()
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun createNewLogFile(): OutputStreamWriter? {
            val fileName = BugClosureFileUtil.getNextFileName(
                mApplicationContext!!,
                mLogFileName,
                mLogFilePath
            )
            val uri = BugClosureFileUtil.createFile(mApplicationContext!!, fileName, mLogFilePath)
            return uri?.let {
                mCurrentFileUri = it
                mApplicationContext!!.contentResolver.openOutputStream(it)?.let { outputStream ->
                    OutputStreamWriter(outputStream)
                }
            }
        }
    }


}