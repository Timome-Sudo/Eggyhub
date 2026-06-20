package com.timome.eggyhub.data

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 人机验证全局状态管理器（单例）
 *
 * 功能：
 * 1. 管理验证取消后的 15 秒冷却时间
 * 2. 管理当前验证流程中"我不会"的点击次数（最多3次，下一次验证流程重置）
 * 3. 生成各种验证所需的随机数据
 * 4. 计数验证的连续失败次数统计（5次后重置题目）
 */
object CaptchaManager {

    // =================== 冷却时间 ===================
    private const val COOLDOWN_SECONDS = 15
    private var lastCancelTimestampMs: Long = 0

    fun isInCooldown(): Boolean {
        val elapsed = System.currentTimeMillis() - lastCancelTimestampMs
        return elapsed < COOLDOWN_SECONDS * 1000L
    }

    fun getRemainingCooldownSeconds(): Int {
        val elapsed = System.currentTimeMillis() - lastCancelTimestampMs
        val remaining = COOLDOWN_SECONDS * 1000L - elapsed
        return if (remaining > 0) ((remaining / 1000f).roundToInt()) else 0
    }

    fun markCancelled() {
        lastCancelTimestampMs = System.currentTimeMillis()
    }

    // =================== "我不会"点击次数 ===================
    // 每次开始一个新的验证流程（CaptchaDialog 首次显示）时重置为 0
    private var helpClickCount: Int = 0

    fun getHelpClickCount(): Int = helpClickCount

    fun incrementHelpClickCount(): Int {
        helpClickCount += 1
        return helpClickCount
    }

    fun resetHelpClickCount() {
        helpClickCount = 0
    }

    // =================== 验证类型 ===================
    enum class CaptchaType(val displayName: String) {
        SLIDER("滑条验证"),
        BLIND_SLIDER("盲猜滑条验证"),
        MATH("计算验证"),
        COUNT("计数验证")
    }

    /**
     * 随机选择验证类型（排除特定类型，用于"我不会"切换）
     */
    fun randomCaptchaType(exclude: CaptchaType? = null): CaptchaType {
        val types = CaptchaType.values().filter { it != exclude }
        return types[Random.nextInt(types.size)]
    }

    // =================== 滑条验证 ===================
    /**
     * 生成滑条验证目标值（0-100，包含两端）
     */
    fun generateSliderTarget(): Int {
        return Random.nextInt(0, 101)
    }

    // =================== 计算验证 ===================
    data class MathProblem(
        val num1: Int = 0,
        val num2: Int = 0,
        val num3: Int = 0,
        val operator: Char = '+', // '+', '-', 'x', '÷'
        val operator2: Char = '+', // 第二个运算符（用于三数运算）
        val answer: Int,
        val displayText: String // 例如 "25 + 13 = ?"
    )

    fun generateMathProblem(): MathProblem {
        val problemType = Random.nextInt(0, 5)
        return when (problemType) {
            0 -> {
                val a = Random.nextInt(1, 50)
                val b = Random.nextInt(1, 50)
                MathProblem(a, b, 0, '+', '+', a + b, "$a + $b = ?")
            }
            1 -> {
                val a = Random.nextInt(10, 100)
                val b = Random.nextInt(1, a)
                MathProblem(a, b, 0, '-', '-', a - b, "$a - $b = ?")
            }
            2 -> {
                val a = Random.nextInt(2, 12)
                val b = Random.nextInt(2, 10)
                MathProblem(a, b, 0, 'x', 'x', a * b, "$a × $b = ?")
            }
            3 -> {
                val b = Random.nextInt(2, 10)
                val result = Random.nextInt(2, 12)
                val a = b * result
                MathProblem(a, b, 0, '÷', '÷', result, "$a ÷ $b = ?")
            }
            else -> {
                val a = Random.nextInt(1, 30)
                val b = Random.nextInt(1, 30)
                val c = Random.nextInt(1, 30)
                MathProblem(a, b, c, '+', '+', a + b + c, "$a + $b + $c = ?")
            }
        }
    }

    // =================== 计数验证 ===================
    // 200 个常用汉字字库
    private val CHINESE_CHARS = listOf(
        "的", "一", "是", "在", "不", "了", "有", "和", "人", "这",
        "中", "大", "为", "上", "个", "我", "们", "以", "到", "他",
        "时", "说", "国", "地", "也", "子", "而", "你", "要", "那",
        "会", "着", "没", "看", "好", "自", "己", "里", "天", "年",
        "用", "事", "能", "就", "向", "进", "出", "得", "来", "去",
        "多", "少", "可", "爱", "美", "丽", "高", "低", "小", "长",
        "前", "后", "左", "右", "东", "西", "南", "北", "内", "外",
        "上", "下", "中", "间", "边", "角", "面", "方", "圆", "点",
        "起", "坐", "走", "跑", "跳", "飞", "游", "爬", "站", "睡",
        "吃", "喝", "看", "听", "说", "读", "写", "学", "习", "做",
        "工", "作", "生", "活", "家", "房", "楼", "屋", "门", "窗",
        "桌", "椅", "床", "灯", "书", "笔", "纸", "本", "包", "盒",
        "水", "火", "山", "石", "土", "田", "日", "月", "星", "云",
        "风", "雨", "雪", "雷", "电", "春", "夏", "秋", "冬", "季",
        "花", "草", "树", "叶", "果", "林", "森", "鸟", "鱼", "虫",
        "牛", "马", "羊", "鸡", "狗", "猫", "猪", "兔", "虎", "龙",
        "红", "黄", "蓝", "绿", "白", "黑", "紫", "橙", "粉", "灰",
        "金", "银", "铜", "铁", "钱", "元", "角", "分", "市", "斤",
        "爸", "妈", "哥", "姐", "弟", "妹", "爷", "奶", "叔", "舅",
        "男", "女", "老", "幼", "师", "生", "友", "情", "爱", "心"
    ).distinct()

    // 完整字库（汉字 + 大小写英文字母 + 数字1-9）
    private val FULL_CHAR_POOL: List<String> by lazy {
        val pool = mutableListOf<String>()
        pool.addAll(CHINESE_CHARS)
        for (c in 'a'..'z') pool.add(c.toString())
        for (c in 'A'..'Z') pool.add(c.toString())
        for (n in '1'..'9') pool.add(n.toString())
        pool
    }

    data class CountProblem(
        val text: String,           // 完整文本（包含插入的字符）
        val targetChar: String,     // 需要计数的字符
        val expectedCount: Int      // 实际出现次数（期望用户输入的值）
    )

    /**
     * 生成计数验证题目
     */
    fun generateCountProblem(): CountProblem {
        val baseLength = Random.nextInt(20, 51)
        val insertCount = Random.nextInt(5, 31)
        val targetChar = FULL_CHAR_POOL[Random.nextInt(FULL_CHAR_POOL.size)]

        // 生成基础文本（不含目标字符）
        val baseText = StringBuilder()
        repeat(baseLength) {
            var c: String
            do {
                c = FULL_CHAR_POOL[Random.nextInt(FULL_CHAR_POOL.size)]
            } while (c == targetChar) // 确保不含目标字符
            baseText.append(c)
        }

        // 在随机位置插入 targetChar（共 insertCount 次）
        repeat(insertCount) {
            val pos = Random.nextInt(baseText.length + 1)
            baseText.insert(pos, targetChar)
        }

        return CountProblem(
            text = baseText.toString(),
            targetChar = targetChar,
            expectedCount = insertCount
        )
    }

    /**
     * 计算字符在文本中出现的实际次数（用于比对用户输入）
     */
    fun countCharOccurrences(text: String, targetChar: String): Int {
        var count = 0
        var index = 0
        while (index < text.length) {
            // 汉字和英文字符长度不同
            if (index + targetChar.length <= text.length &&
                text.substring(index, index + targetChar.length) == targetChar
            ) {
                count++
                index += targetChar.length
            } else {
                index++
            }
        }
        return count
    }

    // =================== 计数验证连续失败统计 ===================
    private var countFailCount: Int = 0

    fun incrementCountFail(): Int {
        countFailCount += 1
        return countFailCount
    }

    fun resetCountFail() {
        countFailCount = 0
    }

    fun shouldRegenerateCountProblem(): Boolean = countFailCount > 5

    // =================== 数值判断辅助函数 ===================

    /**
     * 判断值是否在目标的容差范围内
     */
    fun isValueInRange(value: Float, target: Int, tolerance: Int = 0): Boolean {
        val diff = abs(value - target)
        return diff <= tolerance
    }
}
