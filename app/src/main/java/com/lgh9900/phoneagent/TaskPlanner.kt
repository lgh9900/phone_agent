package com.lgh9900.phoneagent

enum class ActionType {
    LAUNCH,
    TAP,
    TYPE,
    SWIPE,
    BACK,
    HOME,
    LONG_PRESS,
    DOUBLE_TAP,
    WAIT,
    FINISH,
    TAKE_OVER,
    UNKNOWN
}

data class AgentAction(
    val type: ActionType,
    val x: Int? = null,
    val y: Int? = null,
    val x2: Int? = null,
    val y2: Int? = null,
    val text: String? = null,
    val app: String? = null,
    val durationMs: Long? = null,
    val message: String? = null
)

object TaskPlanner {


    fun parseAction(actionString: String): AgentAction {
        val cleaned = actionString.trim()

        return try {
            when {
                cleaned.startsWith("do(") -> parseDo(cleaned)
                cleaned.startsWith("finish(") -> parseFinish(cleaned)
                else -> AgentAction(ActionType.UNKNOWN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AgentAction(ActionType.UNKNOWN)
        }
    }


    private fun parseDo(text: String): AgentAction {
        val content = text.substringAfter("do(").substringBeforeLast(")")

        val params = parseParameters(content)
        val actionName = params["action"] ?: return AgentAction(ActionType.UNKNOWN)

        return when (actionName) {
            "Launch" -> {
                AgentAction(
                    type = ActionType.LAUNCH,
                    app = params["app"] ?: getPackageName(params["app"] ?: "")
                )
            }

            "Tap" -> {
                val element = parseArray(params["element"])
                AgentAction(
                    type = ActionType.TAP,
                    x = element?.getOrNull(0),
                    y = element?.getOrNull(1),
                    message = params["message"]
                )
            }

            "Type", "Type_Name" -> {
                AgentAction(
                    type = ActionType.TYPE,
                    text = params["text"]
                )
            }

            "Swipe" -> {
                val start = parseArray(params["start"])
                val end = parseArray(params["end"])
                AgentAction(
                    type = ActionType.SWIPE,
                    x = start?.getOrNull(0),
                    y = start?.getOrNull(1),
                    x2 = end?.getOrNull(0),
                    y2 = end?.getOrNull(1)
                )
            }

            "Back" -> AgentAction(ActionType.BACK)

            "Home" -> AgentAction(ActionType.HOME)

            "Long Press" -> {
                val element = parseArray(params["element"])
                AgentAction(
                    type = ActionType.LONG_PRESS,
                    x = element?.getOrNull(0),
                    y = element?.getOrNull(1)
                )
            }

            "Double Tap" -> {
                val element = parseArray(params["element"])
                AgentAction(
                    type = ActionType.DOUBLE_TAP,
                    x = element?.getOrNull(0),
                    y = element?.getOrNull(1)
                )
            }

            "Wait" -> {
                val durationStr = params["duration"] ?: "1 seconds"
                AgentAction(
                    type = ActionType.WAIT,
                    durationMs = parseDurationMs(durationStr)
                )
            }

            "Take_over" -> {
                AgentAction(
                    type = ActionType.TAKE_OVER,
                    message = params["message"]
                )
            }

            else -> AgentAction(ActionType.UNKNOWN)
        }
    }


    private fun parseFinish(text: String): AgentAction {
        val message = text
            .substringAfter("finish(message=")
            .substringBeforeLast(")")
            .trim('"', '\'')

        return AgentAction(
            type = ActionType.FINISH,
            message = message
        )
    }

    private fun parseParameters(content: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        var currentKey = ""
        var currentValue = StringBuilder()
        var inQuote = false
        var inArray = false

        var i = 0
        while (i < content.length) {
            val char = content[i]

            when {
                char == '=' && !inQuote && !inArray -> {
                    currentKey = currentValue.toString().trim()
                    currentValue.clear()
                }

                char == '"' || char == '\'' -> {
                    inQuote = !inQuote
                }

                char == '[' && !inQuote -> {
                    inArray = true
                    currentValue.append(char)
                }

                char == ']' && !inQuote -> {
                    inArray = false
                    currentValue.append(char)
                }

                char == ',' && !inQuote && !inArray -> {
                    if (currentKey.isNotEmpty()) {
                        params[currentKey] = currentValue.toString().trim()
                        currentKey = ""
                        currentValue.clear()
                    }
                }

                else -> {
                    currentValue.append(char)
                }
            }

            i++
        }

        if (currentKey.isNotEmpty()) {
            params[currentKey] = currentValue.toString().trim()
        }

        return params
    }


    private fun parseArray(arrayStr: String?): List<Int>? {
        if (arrayStr == null) return null

        return try {
            val cleaned = arrayStr.trim().removeSurrounding("[", "]")
            cleaned.split(",")
                .map { it.trim().toInt() }
        } catch (e: Exception) {
            null
        }
    }


    private fun parseDurationMs(durationStr: String): Long {
        return try {
            when {
                "second" in durationStr.lowercase() -> {
                    val num = durationStr.filter { it.isDigit() }.toLongOrNull() ?: 1
                    num * 1000
                }
                "ms" in durationStr.lowercase() -> {
                    durationStr.filter { it.isDigit() }.toLongOrNull() ?: 1000
                }
                else -> 1000L
            }
        } catch (e: Exception) {
            1000L
        }
    }

    private fun getPackageName(appName: String): String {
        return when (appName) {
            "淘宝" -> "com.taobao.taobao"
            "微信" -> "com.tencent.mm"
            "抖音" -> "com.ss.android.ugc.aweme"
            "京东" -> "com.jingdong.app.mall"
            "天猫" -> "com.tmall.wireless"
            "钉钉" -> "com.alibaba.android.rimet"
            "QQ" -> "com.tencent.mobileqq"
            "微博" -> "com.sina.weibo"
            "拼多多" -> "com.xunmeng.pinduoduo"
            "小红书" -> "com.xiaohongshu.app"
            "美团" -> "com.sankuai.meituan"
            "饿了么" -> "com.ele.me"
            "设置" -> "com.android.settings"
            else -> appName // 如果没有映射，假设输入的就是包名
        }
    }
}