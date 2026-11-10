package com.example.lovecounter.utils.ads

import android.app.Application
import android.util.Log
import com.ads.admob.admob.AdmobFactoryImpl
import com.ads.admob.config.AfAdConfig

/**
 * AdsInitializer — Đầu mối duy nhất chịu trách nhiệm khởi tạo Google Mobile Ads SDK.
 *
 * Trách nhiệm:
 * - Gọi AdmobFactoryImpl.INSTANCE.initAdmob() đúng **một lần** duy nhất.
 * - Phơi bày [SdkState] ra ngoài để các class khác kiểm tra SDK đã sẵn sàng chưa.
 * - Hỗ trợ nhiều nơi đăng ký callback qua [runWhenReady] mà không gọi initAdmob nhiều lần.
 * - Không tự load bất kỳ loại Ad nào.
 * - Không chặn Main Thread.
 *
 * Sử dụng:
 * ```
 * // Tại App.onCreate():
 * AdsInitializer.initialize(this)
 *
 * // Tại bất kỳ nơi nào cần biết SDK đã sẵn sàng:
 * AdsInitializer.runWhenReady { /* load ads */ }
 *
 * // Kiểm tra trạng thái:
 * if (AdsInitializer.state == AdsInitializer.SdkState.INITIALIZED) { ... }
 * ```
 */
object AdsInitializer {

    private const val TAG = "AdsInitializer"

    /**
     * Trạng thái vòng đời của Google Mobile Ads SDK.
     */
    enum class SdkState {
        /** Chưa gọi initialize() lần nào. */
        NOT_INITIALIZED,

        /** Đang trong quá trình khởi tạo (MobileAds.initialize chưa callback). */
        INITIALIZING,

        /** SDK đã khởi tạo thành công, sẵn sàng load ads. */
        INITIALIZED,

        /** Khởi tạo thất bại do exception. */
        FAILED
    }

    /** Trạng thái hiện tại của SDK. Thread-safe (đọc @Volatile). */
    @Volatile
    var state: SdkState = SdkState.NOT_INITIALIZED
        private set

    /** Danh sách callback chờ SDK sẵn sàng. Chỉ dùng khi state == INITIALIZING. */
    private val pendingReadyCallbacks = mutableListOf<() -> Unit>()

    /**
     * Khởi tạo Google Mobile Ads SDK.
     *
     * - Chỉ khởi tạo đúng **một lần** trong vòng đời app.
     * - Nếu đang [SdkState.INITIALIZING]: callback sẽ được xếp hàng, không gọi initialize lại.
     * - Nếu đã [SdkState.INITIALIZED]: callback được gọi ngay lập tức.
     * - Không ảnh hưởng UI thread.
     *
     * @param application Application context.
     * @param adConfig Cấu hình Admob (test devices, interval...). Mặc định dùng AfAdConfig.Builder().build().
     * @param onReady Callback được gọi khi SDK sẵn sàng (cả thành công lẫn thất bại đều invoke để tránh chặn flow).
     */
    fun initialize(
        application: Application,
        adConfig: AfAdConfig = AfAdConfig.Builder().build(),
        onReady: (() -> Unit)? = null
    ) {
        when (state) {
            SdkState.INITIALIZED -> {
                Log.d(TAG, "SDK already initialized — invoking onReady immediately.")
                onReady?.invoke()
                return
            }
            SdkState.INITIALIZING -> {
                Log.d(TAG, "SDK is initializing — queuing onReady callback.")
                onReady?.let {
                    synchronized(pendingReadyCallbacks) {
                        pendingReadyCallbacks.add(it)
                    }
                }
                return
            }
            SdkState.FAILED -> {
                // Cho phép thử lại nếu đã thất bại
                Log.w(TAG, "SDK previously failed — retrying initialization.")
            }
            SdkState.NOT_INITIALIZED -> {
                // Tiếp tục khởi tạo lần đầu
            }
        }

        state = SdkState.INITIALIZING
        Log.d(TAG, "Starting AdMob SDK initialization...")

        onReady?.let {
            synchronized(pendingReadyCallbacks) {
                pendingReadyCallbacks.add(it)
            }
        }

        try {
            AdmobFactoryImpl.INSTANCE.initAdmob(application, adConfig) {
                onSdkInitialized()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SDK initialization threw an exception: ${e.message}", e)
            onSdkFailed()
        }
    }

    /**
     * Đăng ký một callback để chạy khi SDK sẵn sàng.
     *
     * - Nếu SDK đã [SdkState.INITIALIZED]: gọi ngay lập tức trên calling thread.
     * - Nếu SDK đang [SdkState.INITIALIZING]: xếp hàng chờ.
     * - Nếu SDK chưa khởi tạo: log cảnh báo — caller nên gọi [initialize] trước.
     *
     * @param task Callback được gọi khi SDK sẵn sàng.
     */
    fun runWhenReady(task: () -> Unit) {
        when (state) {
            SdkState.INITIALIZED -> {
                task()
            }
            SdkState.INITIALIZING, SdkState.NOT_INITIALIZED -> {
                synchronized(pendingReadyCallbacks) {
                    pendingReadyCallbacks.add(task)
                }
                if (state == SdkState.NOT_INITIALIZED) {
                    Log.w(TAG, "runWhenReady() called before initialize(). Task is queued but initialize() must be called first.")
                }
            }
            SdkState.FAILED -> {
                Log.e(TAG, "runWhenReady() called after SDK FAILED. Task will not run.")
            }
        }
    }

    /** SDK đã khởi tạo thành công — cập nhật state và drain queue. */
    private fun onSdkInitialized() {
        state = SdkState.INITIALIZED
        Log.d(TAG, "AdMob SDK initialized successfully.")
        drainPendingCallbacks()
    }

    /** SDK khởi tạo thất bại — cập nhật state và drain queue (để không block các flow chờ). */
    private fun onSdkFailed() {
        state = SdkState.FAILED
        Log.e(TAG, "AdMob SDK initialization FAILED. Draining queued callbacks to unblock flows.")
        drainPendingCallbacks()
    }

    private fun drainPendingCallbacks() {
        val callbacks = synchronized(pendingReadyCallbacks) {
            val snapshot = pendingReadyCallbacks.toList()
            pendingReadyCallbacks.clear()
            snapshot
        }
        Log.d(TAG, "Draining ${callbacks.size} pending callback(s).")
        callbacks.forEach { runCatching { it() } }
    }
}
