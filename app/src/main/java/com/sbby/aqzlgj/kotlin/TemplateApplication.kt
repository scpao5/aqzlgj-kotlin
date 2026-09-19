package com.sbby.aqzlgj.kotlin

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.UserManager
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import okhttp3.Cache
import okhttp3.OkHttpClient
import com.sbby.aqzlgj.kotlin.ui.util.CrashHandler
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.util.Locale

lateinit var templateApp: TemplateApplication

class TemplateApplication : Application(), ViewModelStoreOwner {

    companion object {
        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val applicationInfoClass = ApplicationInfo::class.java
                val method = applicationInfoClass.getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }

    /** 延迟初始化：即使锁屏期(direct boot) onCreate 提前返回，首次访问时也会完成初始化 */
    val okhttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().cache(Cache(File(cacheDir, "okhttp"), 10 * 1024 * 1024))
            .addInterceptor { block ->
                block.proceed(
                    block.request().newBuilder()
                        .header("User-Agent", "aqzlgj-kotlin/${BuildConfig.VERSION_CODE}")
                        .header("Accept-Language", Locale.getDefault().toLanguageTag()).build()
                )
            }.build()
    }
    private val appViewModelStore by lazy { ViewModelStore() }

    private fun isUserUnlocked(): Boolean =
        getSystemService(UserManager::class.java)?.isUserUnlocked == true

    override fun onCreate() {
        super.onCreate()
        templateApp = this

        // 崩溃捕获:尽量在最早注册(锁屏期也能注册,写入失败由内部 try 兜底)
        CrashHandler.register(this)

        if (!isUserUnlocked()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val prefs = this.getSharedPreferences("settings", MODE_PRIVATE)
            val enable = prefs.getBoolean("enable_predictive_back", false)
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback")
            setEnableOnBackInvokedCallback(applicationInfo, enable)
        }

    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
