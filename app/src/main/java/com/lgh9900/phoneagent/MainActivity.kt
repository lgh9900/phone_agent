package com.lgh9900.phoneagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lgh9900.phoneagent.service.ScreenCaptureService

class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AgentViewModel = viewModel()) {
    val context = LocalContext.current
    var taskText by remember { mutableStateOf("") }
    val logs by viewModel.logs.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.addLog("✓ 录音权限已授予")
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                taskText = matches[0]
                viewModel.addLog("语音识别: $taskText")
            }
        }
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            result.data?.let { data ->
                try {
                    ScreenCaptureService.startService(context, result.resultCode, data)
                    viewModel.setScreenCaptureReady(true)
                    viewModel.addLog("✓ 截屏权限已授予")
                } catch (e: Exception) {
                    viewModel.addLog("✗ 启动截屏服务失败: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            viewModel.addLog("✗ 截屏权限被拒绝")
        }
    }

    LaunchedEffect(Unit) {
        if (!isAccessibilityServiceEnabled(context)) {
            viewModel.addLog("⚠ 请启用无障碍服务")
        }
        else if (ScreenCaptureService.getInstance() != null) {
            viewModel.setScreenCaptureReady(true)
            viewModel.addLog("✓ 截屏服务已就绪")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Agent") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            /*Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("API 配置", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    var apiUrl by remember { mutableStateOf(viewModel.apiUrl.value) }
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it; viewModel.updateApiUrl(it) },
                        label = { Text("API URL") },
                        placeholder = { Text("http://your-server:8000/v1/chat/completions") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }*/

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = taskText,
                onValueChange = { taskText = it },
                label = { Text("任务指令") },
                placeholder = { Text("例如: 打开淘宝搜索牛奶") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning,
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!isAccessibilityServiceEnabled(context)) {
                            openAccessibilitySettings(context)
                            viewModel.addLog("请在设置中启用 Phone Agent 无障碍服务，启用后无需再次设置")
                            return@Button
                        }

                        if (!viewModel.isScreenCaptureReady.value) {
                            val projectionManager = context.getSystemService(
                                MediaProjectionManager::class.java
                            )
                            screenCaptureLauncher.launch(
                                projectionManager.createScreenCaptureIntent()
                            )
                            return@Button
                        }

                        if (taskText.isNotBlank()) {
                            viewModel.startTask(taskText)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning
                ) {
                    Text(if (isRunning) "执行中..." else "执行任务")
                }

                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@Button
                        }

                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出任务指令")
                        }
                        speechLauncher.launch(intent)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning
                ) {
                    Text("语音输入")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isRunning) {
                Button(
                    onClick = { viewModel.stopTask() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("停止任务")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "运行日志",
                            style = MaterialTheme.typography.titleSmall
                        )
                        TextButton(onClick = { viewModel.clearLogs() }) {
                            Text("清空")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = logs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

           /* Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { openAccessibilitySettings(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("无障碍设置", style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = { openOverlaySettings(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("悬浮窗权限", style = MaterialTheme.typography.bodySmall)
                }
            }*/
        }
    }
}

fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val service = "${context.packageName}/.service.MyAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    
    val service1 = "${context.packageName}/.service.MyAccessibilityService"
    val service2 = "${context.packageName}.service.MyAccessibilityService"
    
    return enabledServices?.contains(service1) == true || 
           enabledServices?.contains(service2) == true ||
           enabledServices?.contains(context.packageName) == true
}

fun openAccessibilitySettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}

fun openOverlaySettings(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}