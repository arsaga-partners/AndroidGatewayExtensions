package jp.co.arsaga.extensions.gateway

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Model層で画面遷移のハンドリングをするためのシングルトン。
 * このクラスを使うことでMVVM系アーキテクチャでModel層に起因する画面遷移処理を作成する際の、
 * ViewModelからViewに画面遷移イベントを通知する煩雑さを無くすことで、
 * IO処理完了後画面遷移のフローを一方通行にして可読性を上げるためのクラス。
 *
 * ViewModelでIO処理をする時に、Activityを引数に受け取り画面遷移するラムダを定義してRepositoryに渡し、
 * Repository層でIO処理の完了コールバック内でこのシングルトンにラムダを渡すことで動作する。
 * ガベージコレクションで消えないようにApplicationクラスのサブクラス内から参照し、
 * ApplicationのonCreate時にこのクラスを登録することで最新のActivityがこのクラスに挿入されるようになる。
 */

interface TransitionCallbackHandler {
    /**
     * @param callback
     * 画面遷移を実行するラムダ。
     * 第二引数を実行すると直近最大${maxSize}件分の画面遷移ラムダ式のメソッド名リストを取得できる
     */
    fun post(callback: (Activity, successCallbackNameList: () -> Map<String, Long>) -> Unit)

    /**
     * @param callback 画面遷移を実行するラムダ
     */
    fun post(callback: (Activity) -> Unit)

    abstract class Impl : TransitionCallbackHandler, Application.ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
            execute()
        }

        override fun onActivityPaused(activity: Activity) {
            currentActivity = null
        }

        private var currentActivity: Activity? = null

        private val handler = Handler(Looper.getMainLooper())

        private val isHandling = AtomicBoolean(false)

        private val callbackDeque = ConcurrentLinkedDeque<(Activity) -> Unit>()

        protected open val maxSize: Int = 10

        private val successCallbackNameQueue =
            object : ConcurrentLinkedQueue<Pair<String, Long>>() {
                override fun add(element: Pair<String, Long>?): Boolean {
                    (size - maxSize)
                        .takeIf { it > 0 }
                        ?.run { repeat(this) { poll() } }
                    return super.add(element)
                }
            }

        override fun post(
            callback: (Activity, successCallbackNameList: () -> Map<String, Long>) -> Unit
        ) {
            post { activity ->
                callback(activity) { successCallbackNameQueue.toMap() }
            }
        }

        override fun post(callback: (Activity) -> Unit) {
            callbackDeque.addLast(callback)
            execute()
        }

        private fun execute() {
            fun start() {
                callbackDeque.pollFirst()?.run {
                    handler.post { transition(this, ::start) }
                } ?: run {
                    isHandling.set(false)
                }
            }
            if (isHandling.getAndSet(true)) return
            start()
        }

        private fun transition(
            callback: (Activity) -> Unit,
            onNext: () -> Unit
        ) {
            currentActivity?.let {
                runCatching {
                    callback(it)
                }.onSuccess {
                    Pair(
                        callback.javaClass.name,
                        System.currentTimeMillis()
                    ).run(successCallbackNameQueue::add)
                    onNext()
                }.onFailure {
                    Timber.e(it)
                    when (it) {
                        ::isSuspend -> rollback(callback)
                        else -> onNext()
                    }
                }
            } ?: run { rollback(callback) }
        }

        open fun isSuspend(throwable: Throwable): Boolean = false

        private fun rollback(callback: (Activity) -> Unit) {
            callbackDeque.addFirst(callback)
            isHandling.set(false)
        }
    }
}