package jp.co.arsaga.extensions.gateway

import java.util.concurrent.ConcurrentHashMap

class SharedRepositoryContainer<BaseRepository> {
    private val _transitionTmpContainer: ConcurrentHashMap<() -> Boolean, BaseRepository> =
        ConcurrentHashMap()
    val transitionTmpContainer: Map<() -> Boolean, BaseRepository> = _transitionTmpContainer
    fun cleanUp() {
        _transitionTmpContainer.keys.removeAll { it() }
    }
    inline fun <reified E> searchTmpContainer(): List<E> {
        cleanUp()
        return transitionTmpContainer.values
            .filterIsInstance<E>()
    }
}