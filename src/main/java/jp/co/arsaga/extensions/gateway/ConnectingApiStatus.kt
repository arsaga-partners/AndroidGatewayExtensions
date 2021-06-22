package jp.co.arsaga.extensions.gateway

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * APIの接続状態を管理するクラス
 * ApiDispatchCommandを用いたAPI接続カウント数を保持している
 * プログレスバーの表示状態の管理の他、接続中の他のAPIとの待ち合わせなどに使う
 */

interface ConnectingApiStatus {

    val connectingCount: LiveData<Int>

    fun startApi(apiDispatchCommand: ApiDispatchCommand<*, *>)

    fun finishApi(apiDispatchCommand: ApiDispatchCommand<*, *>)

    abstract class Impl : ConnectingApiStatus {

        protected val container = mutableListOf<String>()

        protected val _connectingCount = MutableLiveData(0)
        override val connectingCount: LiveData<Int> = _connectingCount

        override fun startApi(apiDispatchCommand: ApiDispatchCommand<*, *>) {
            container.add(apiDispatchCommand::class.java.name)
            _connectingCount.postValue(container.size)
        }

        override fun finishApi(apiDispatchCommand: ApiDispatchCommand<*, *>) {
            container.remove(apiDispatchCommand::class.java.name)
            _connectingCount.postValue(container.size)
        }
    }

    object Default : Impl()
}