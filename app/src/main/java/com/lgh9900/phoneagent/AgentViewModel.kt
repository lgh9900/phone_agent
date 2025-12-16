package com.lgh9900.phoneagent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lgh9900.phoneagent.service.ScreenCaptureService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val _logs = MutableStateFlow("")
    val logs: StateFlow<String> = _logs

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _apiUrl = MutableStateFlow("http://10.0.2.2:8000/v1/chat/completions")
    val apiUrl: StateFlow<String> = _apiUrl

    private val _isScreenCaptureReady = MutableStateFlow(false)
    val isScreenCaptureReady: StateFlow<Boolean> = _isScreenCaptureReady

    private var currentJob: Job? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        addLog("📱 Phone Agent 已启动")
        addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        addLog("")
        addLog("⚙️ 初始化检查:")
        addLog("   1. 请启用无障碍服务")
        addLog("   2. 请授予截屏权限")
        addLog("   3. 配置LLM API地址")
        addLog("")
    }

    fun updateApiUrl(url: String) {
        _apiUrl.value = url
        addLog("✓ API URL 已更新")
    }

    fun setScreenCaptureReady(ready: Boolean) {
        _isScreenCaptureReady.value = ready
        if (ready) {
            addLog("✓ 截屏权限已就绪")
        }
    }

    fun startTask(instruction: String) {
        if (_isRunning.value) {
            addLog("⚠ 任务正在执行中...")
            return
        }

        if (instruction.isBlank()) {
            addLog("⚠ 请输入任务指令")
            return
        }

        currentJob = viewModelScope.launch {
            try {
                _isRunning.value = true
                addLog("")
                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                addLog("▶️ 开始执行新任务")
                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                addLog("")

                val context = getApplication<Application>()
                val screenshotProvider = ScreenshotProvider(context)
                val accessibilityController = AccessibilityController.getInstance()

                if (accessibilityController == null) {
                    addLog("❌ 无障碍服务未启用")
                    addLog("   请在设置中启用 Phone Agent 无障碍服务")
                    return@launch
                }

                val screenCaptureService = ScreenCaptureService.getInstance()
                if (screenCaptureService == null || !screenCaptureService.isReady()) {
                    addLog("❌ 截屏服务未就绪")
                    addLog("   请点击按钮重新授权截屏权限")
                    addLog("   ScreenCaptureService 实例: $screenCaptureService")
                    return@launch
                }

                val executor = AgentExecutor(
                    screenshotProvider = screenshotProvider,
                    accessibilityController = accessibilityController,
                    logger = { log -> addLog(log) }
                )

                executor.runTask(instruction)

            } catch (e: Exception) {
                addLog("")
                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                addLog("❌ 任务执行失败")
                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                addLog("错误: ${e.message}")
                addLog("")
                e.printStackTrace()
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun stopTask() {
        currentJob?.cancel()
        _isRunning.value = false
        addLog("")
        addLog("⏹ 任务已停止")
        addLog("")
    }

    fun addLog(message: String) {
        if (message.isEmpty()) {
            _logs.value = "\n" + _logs.value
            return
        }

        val timestamp = dateFormat.format(Date())
        val newLog = "[$timestamp] $message\n"
        _logs.value = newLog + _logs.value
    }

    fun clearLogs() {
        _logs.value = ""
        addLog("✓ 日志已清空")
    }
}