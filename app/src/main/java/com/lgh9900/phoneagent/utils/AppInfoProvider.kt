package com.lgh9900.phoneagent.utils

import android.view.accessibility.AccessibilityEvent

object AppInfoProvider {

    @Volatile
    private var currentPackageName: String = "System Home"


    fun updateFromEvent(event: AccessibilityEvent?) {
        event?.packageName?.toString()?.let { pkgName ->
            if (pkgName.isNotBlank() && !pkgName.startsWith("android")) {
                currentPackageName = getAppName(pkgName) ?: pkgName
            }
        }
    }


    fun getCurrentApp(): String {
        return currentPackageName
    }


    fun resetToHome() {
        currentPackageName = "System Home"
    }


    fun getPackageName(appName: String?): String? {
        return when (appName) {
            // Social & Messaging
            "微信" -> "com.tencent.mm"
            "QQ" -> "com.tencent.mobileqq"
            "微博" -> "com.sina.weibo"

            // E-commerce
            "淘宝" -> "com.taobao.taobao"
            "京东" -> "com.jingdong.app.mall"
            "拼多多" -> "com.xunmeng.pinduoduo"
            "淘宝闪购" -> "com.taobao.taobao"
            "京东秒送" -> "com.jingdong.app.mall"

            // Lifestyle & Social
            "小红书" -> "com.xingin.xhs"
            "豆瓣" -> "com.douban.frodo"
            "知乎" -> "com.zhihu.android"

            // Maps & Navigation
            "高德地图" -> "com.autonavi.minimap"
            "百度地图" -> "com.baidu.BaiduMap"

            // Food & Services
            "美团" -> "com.sankuai.meituan"
            "大众点评" -> "com.dianping.v1"
            "饿了么" -> "me.ele"
            "肯德基" -> "com.yek.android.kfc.activitys"

            // Travel
            "携程" -> "ctrip.android.view"
            "铁路12306" -> "com.MobileTicket"
            "12306" -> "com.MobileTicket"
            "去哪儿" -> "com.Qunar"
            "去哪儿旅行" -> "com.Qunar"
            "滴滴出行" -> "com.sdu.did.psnger"

            // Video & Entertainment
            "bilibili" -> "tv.danmaku.bili"
            "抖音" -> "com.ss.android.ugc.aweme"
            "快手" -> "com.smile.gifmaker"
            "腾讯视频" -> "com.tencent.qqlive"
            "爱奇艺" -> "com.qiyi.video"
            "优酷视频" -> "com.youku.phone"
            "芒果TV" -> "com.hunantv.imgo.activity"
            "红果短剧" -> "com.phoenix.read"

            // Music & Audio
            "网易云音乐" -> "com.netease.cloudmusic"
            "QQ音乐" -> "com.tencent.qqmusic"
            "汽水音乐" -> "com.luna.music"
            "喜马拉雅" -> "com.ximalaya.ting.android"

            // Reading
            "番茄小说" -> "com.dragon.read"
            "番茄免费小说" -> "com.dragon.read"
            "七猫免费小说" -> "com.kmxs.reader"

            // Productivity
            "飞书" -> "com.ss.android.lark"
            "QQ邮箱" -> "com.tencent.androidqqmail"

            // AI & Tools
            "豆包" -> "com.larus.nova"

            // Health & Fitness
            "keep" -> "com.gotokeep.keep"
            "美柚" -> "com.lingan.seeyou"

            // News & Information
            "腾讯新闻" -> "com.tencent.news"
            "今日头条" -> "com.ss.android.article.news"

            // Real Estate
            "贝壳找房" -> "com.lianjia.beike"
            "安居客" -> "com.anjuke.android.app"

            // Finance
            "同花顺" -> "com.hexin.plat.android"

            // Games
            "星穹铁道" -> "com.miHoYo.hkrpg"
            "崩坏：星穹铁道" -> "com.miHoYo.hkrpg"
            "恋与深空" -> "com.papegames.lysk.cn"

            // System Apps
            "设置", "AndroidSystemSettings", "Android System Settings",
            "Android  System Settings", "Android-System-Settings",
            "Settings" -> "com.android.settings"

            "AudioRecorder", "audiorecorder" -> "com.android.soundrecorder"
            "Bluecoins", "bluecoins" -> "com.rammigsoftware.bluecoins"
            "Broccoli", "broccoli" -> "com.flauschcode.broccoli"

            "Booking.com", "Booking", "booking.com", "booking", "BOOKING.COM" -> "com.booking"

            "Chrome", "chrome", "Google Chrome" -> "com.android.chrome"
            "Clock", "clock" -> "com.android.deskclock"
            "Contacts", "contacts" -> "com.android.contacts"

            "Duolingo", "duolingo" -> "com.duolingo"
            "Expedia", "expedia" -> "com.expedia.bookings"

            "Files", "files", "File Manager", "file manager" -> "com.android.fileexplorer"
            "gmail", "Gmail", "GoogleMail", "Google Mail" -> "com.google.android.gm"

            "GoogleFiles", "googlefiles", "FilesbyGoogle" -> "com.google.android.apps.nbu.files"
            "GoogleCalendar", "Google-Calendar", "Google Calendar",
            "google-calendar", "google calendar" -> "com.google.android.calendar"

            "GoogleChat", "Google Chat", "Google-Chat" -> "com.google.android.apps.dynamite"
            "GoogleClock", "Google Clock", "Google-Clock" -> "com.google.android.deskclock"

            "GoogleContacts", "Google-Contacts", "Google Contacts",
            "google-contacts", "google contacts" -> "com.google.android.contacts"

            "GoogleDocs", "Google Docs", "googledocs", "google docs" -> "com.google.android.apps.docs.editors.docs"
            "Google Drive", "Google-Drive", "google drive",
            "google-drive", "GoogleDrive", "Googledrive", "googledrive" -> "com.google.android.apps.docs"

            "GoogleFit", "googlefit" -> "com.google.android.apps.fitness"
            "GoogleKeep", "googlekeep" -> "com.google.android.keep"

            "GoogleMaps", "Google Maps", "googlemaps", "google maps" -> "com.google.android.apps.maps"
            "Google Play Books", "Google-Play-Books", "google play books",
            "google-play-books", "GooglePlayBooks", "googleplaybooks" -> "com.google.android.apps.books"

            "GooglePlayStore", "Google Play Store", "Google-Play-Store" -> "com.android.vending"
            "GoogleSlides", "Google Slides", "Google-Slides" -> "com.google.android.apps.docs.editors.slides"
            "GoogleTasks", "Google Tasks", "Google-Tasks" -> "com.google.android.apps.tasks"

            "Joplin", "joplin" -> "net.cozic.joplin"
            "McDonald", "mcdonald" -> "com.mcdonalds.app"
            "Osmand", "osmand" -> "net.osmand"

            "PiMusicPlayer", "pimusicplayer" -> "com.Project100Pi.themusicplayer"
            "Quora", "quora" -> "com.quora.android"
            "Reddit", "reddit" -> "com.reddit.frontpage"

            "RetroMusic", "retromusic" -> "code.name.monkey.retromusic"
            "SimpleCalendarPro" -> "com.scientificcalculatorplus.simplecalculator.basiccalculator.mathcalc"
            "SimpleSMSMessenger" -> "com.simplemobiletools.smsmessenger"

            "Telegram" -> "org.telegram.messenger"
            "temu", "Temu" -> "com.einnovation.temu"
            "Tiktok", "tiktok" -> "com.zhiliaoapp.musically"
            "Twitter", "twitter", "X" -> "com.twitter.android"
            "VLC" -> "org.videolan.vlc"
            "WeChat", "wechat" -> "com.tencent.mm"
            "Whatsapp", "WhatsApp" -> "com.whatsapp"

            else -> null
        }
    }


    private fun getAppName(packageName: String): String? {
        return when (packageName) {
            "com.taobao.taobao" -> "淘宝"
            "com.tencent.mm" -> "微信"
            "com.ss.android.ugc.aweme" -> "抖音"
            "com.jingdong.app.mall" -> "京东"
            "com.tmall.wireless" -> "天猫"
            "com.alibaba.android.rimet" -> "钉钉"
            "com.tencent.mobileqq" -> "QQ"
            "com.sina.weibo" -> "微博"
            "com.xunmeng.pinduoduo" -> "拼多多"
            "com.xiaohongshu.app" -> "小红书"
            "com.sankuai.meituan" -> "美团"
            "com.ele.me" -> "饿了么"
            "com.android.settings" -> "设置"
            "com.android.chrome" -> "Chrome"
            else -> null
        }
    }


}