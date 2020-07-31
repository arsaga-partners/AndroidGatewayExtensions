package jp.co.arsaga.extensions.gateway

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class BaseLiveSharedPreference<T : Any>(
    private val key: String,
    private val defaultValue: T
) : LiveData<T>() {

    protected abstract val sharedPreference: SharedPreferences

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (this.key == key) get(sharedPreferences, key, defaultValue)
            .run { postValue(this) }
    }

    protected abstract fun get(sharedPreferences: SharedPreferences, key: String, defaultValue: T): T

    abstract fun put(value: T)

    fun delete() {
        sharedPreference.edit().remove(key).apply()
    }

    override fun onActive() {
        super.onActive()
        value = get(sharedPreference, key, defaultValue)
        sharedPreference.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    override fun onInactive() {
        sharedPreference.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        super.onInactive()
        value = null
    }
}
