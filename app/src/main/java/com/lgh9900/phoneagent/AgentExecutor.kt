package com.lgh9900.phoneagent

import android.graphics.Bitmap
import android.util.Base64
import com.lgh9900.phoneagent.config.SystemPrompt
import com.lgh9900.phoneagent.model.ModelConfig
import com.lgh9900.phoneagent.utils.AppInfoProvider
import com.lgh9900.phoneagent.utils.AppInfoProvider.getPackageName
import com.lgh9900.phoneagent.utils.MessageBuilder
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream

data class StepResult(
    val success: Boolean,
    val finished: Boolean,
    val action: AgentAction?,
    val thinking: String,
    val message: String? = null
)

class AgentExecutor(
    private val screenshotProvider: ScreenshotProvider,
    private val accessibilityController: AccessibilityController,
    private val logger: (String) -> Unit
) {

    companion object {
        const val MAX_STEPS = 100
        const val STEP_DELAY_MS = 1500L
    }

    private val modelConfig = ModelConfig()
    private val llmApi = LLMApi(modelConfig)

    private val context = mutableListOf<Map<String, Any>>()
    private var stepCount = 0


    suspend fun runTask(instruction: String) {
        logger("━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        logger("🎯 任务目标: $instruction")
        logger("━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        context.clear()
        stepCount = 0

        val firstResult = executeStep(instruction, isFirst = true)

        if (firstResult.finished) {
            logger("✅ 任务完成: ${firstResult.message ?: "完成"}")
            return
        }

        while (stepCount < MAX_STEPS) {
            val result = executeStep(isFirst = false)

            if (result.finished) {
                logger("\n🎉 ═══════════════════════════")
                logger("✅ 任务完成: ${result.message ?: "完成"}")
                logger("═══════════════════════════\n")
                return
            }

            delay(STEP_DELAY_MS)
        }

        logger("⏱️ 达到最大步数限制 ($MAX_STEPS)")
    }

    private suspend fun executeStep(
        userPrompt: String? = null,
        isFirst: Boolean = false
    ): StepResult {
        stepCount++

        logger("\n━━━ 第 $stepCount 步 ━━━")

        try {
            logger("📸 截取屏幕...")
            val screenshot = screenshotProvider.captureScreen()

            if (screenshot == null) {
                logger("⚠ 截屏失败")
                logger("💡 请检查以下事项:")
                logger("   1. 截屏权限是否已授予")
                logger("   2. 截屏服务是否正常运行")
                logger("   3. 应用是否在后台被系统杀死")
                return StepResult(
                    success = false,
                    finished = false,
                    action = null,
                    thinking = "",
                    message = "截屏失败"
                )
            }

            val currentApp = AppInfoProvider.getCurrentApp()
            logger("📱 当前应用: $currentApp")
            logger("📐 屏幕尺寸: ${screenshot.width}x${screenshot.height}")

            if (isFirst) {
                context.add(MessageBuilder.createSystemMessage(SystemPrompt.getChinese()))

                val screenInfo = MessageBuilder.buildScreenInfo(currentApp)
                val textContent = "$userPrompt\n\n$screenInfo"
                val base64Image = bitmapToBase64(screenshot)

                context.add(
                    MessageBuilder.createUserMessage(
                        text = textContent,
                        imageBase64 = base64Image
                    )
                )
            } else {
                val screenInfo = MessageBuilder.buildScreenInfo(currentApp)
                val textContent = "** Screen Info **\n\n$screenInfo"
                val base64Image = bitmapToBase64(screenshot)

                context.add(
                    MessageBuilder.createUserMessage(
                        text = textContent,
                        imageBase64 = base64Image
                    )
                )
            }

            logger("🤖 请求 LLM 分析...")
            logger("💭 思考过程:")
            logger("─".repeat(30))

            val response = llmApi.request(context) { thinkingChunk ->
                logger(thinkingChunk)
            }

            logger("─".repeat(30))
            logger("🎯 决策动作:")
            logger(response.action)
            logger("─".repeat(30))

            val action = try {
                TaskPlanner.parseAction(response.action)
            } catch (e: Exception) {
                logger("❌ 动作解析失败: ${e.message}")
                AgentAction(ActionType.FINISH)
            }

            logger("📌 动作类型: ${action.type}")

            val lastIndex = context.size - 1
            context[lastIndex] = MessageBuilder.removeImagesFromMessage(context[lastIndex])

            context.add(
                MessageBuilder.createAssistantMessage(
                    "<think>${response.thinking}</think><answer>${response.action}</answer>"
                )
            )

            val actionResult = executeAction(action, screenshot.width, screenshot.height)

            val finished = action.type == ActionType.FINISH ||
                    action.type == ActionType.TAKE_OVER

            return StepResult(
                success = actionResult,
                finished = finished,
                action = action,
                thinking = response.thinking,
                message = when (action.type) {
                    ActionType.FINISH -> "任务完成"
                    ActionType.TAKE_OVER -> "需要人工介入"
                    else -> null
                }
            )

        } catch (e: Exception) {
            logger("❌ 步骤执行失败: ${e.message}")
            e.printStackTrace()
            return StepResult(
                success = false,
                finished = false,
                action = null,
                thinking = "",
                message = "执行失败: ${e.message}"
            )
        }
    }


    private suspend fun executeAction(
        action: AgentAction,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        return try {
            when (action.type) {
                ActionType.TAP -> {
                    val absX = convertToAbsolute(action.x!!, screenWidth)
                    val absY = convertToAbsolute(action.y!!, screenHeight)
                    logger("👆 点击 ($absX, $absY)")
                    accessibilityController.click(absX, absY)
                    true
                }

                ActionType.TYPE -> {
                    logger("⌨️ 输入: ${action.text}")
                    accessibilityController.inputText(action.text!!)
                    true
                }

                ActionType.SWIPE -> {
                    val absX1 = convertToAbsolute(action.x!!, screenWidth)
                    val absY1 = convertToAbsolute(action.y!!, screenHeight)
                    val absX2 = convertToAbsolute(action.x2!!, screenWidth)
                    val absY2 = convertToAbsolute(action.y2!!, screenHeight)
                    logger("👆 滑动 ($absX1,$absY1) → ($absX2,$absY2)")
                    accessibilityController.swipe(absX1, absY1, absX2, absY2)
                    true
                }

                ActionType.BACK -> {
                    logger("◀️ 返回")
                    accessibilityController.performBack()
                    true
                }

                ActionType.HOME -> {
                    logger("🏠 回到主屏幕")
                    accessibilityController.performHome()
                    AppInfoProvider.resetToHome()
                    true
                }

                ActionType.LAUNCH -> {
                    val packageName = getPackageName(action.app)
                    logger("🚀 启动应用: ${packageName}")
                    accessibilityController.launchApp(packageName!!)
                    true
                }

                ActionType.LONG_PRESS -> {
                    val absX = convertToAbsolute(action.x!!, screenWidth)
                    val absY = convertToAbsolute(action.y!!, screenHeight)
                    logger("👆 长按 ($absX, $absY)")
                    accessibilityController.longPress(absX, absY)
                    true
                }

                ActionType.DOUBLE_TAP -> {
                    val absX = convertToAbsolute(action.x!!, screenWidth)
                    val absY = convertToAbsolute(action.y!!, screenHeight)
                    logger("👆👆 双击 ($absX, $absY)")
                    accessibilityController.doubleTap(absX, absY)
                    true
                }

                ActionType.WAIT -> {
                    logger("⏱️ 等待 ${action.durationMs}ms")
                    delay(action.durationMs!!)
                    true
                }

                ActionType.FINISH -> {
                    logger("🎉 完成")
                    true
                }

                ActionType.TAKE_OVER -> {
                    logger("🤚 请求人工介入")
                    true
                }

                ActionType.UNKNOWN -> {
                    logger("⚠ 未知动作")
                    false
                }
            }
        } catch (e: Exception) {
            logger("❌ 动作执行失败: ${e.message}")
            false
        }
    }


    private fun convertToAbsolute(relative: Int, dimension: Int): Int {
        return (relative / 1000.0 * dimension).toInt()
    }


    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}