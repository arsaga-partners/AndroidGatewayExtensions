package jp.co.arsaga.extensions.gateway

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData

abstract class BaseLiveSharedPreference<T : Any>(
    protected val sharedPreference: SharedPreferences,
    private val key: String,
    private val defaultValue: T
) : MutableLiveData<T>() {

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (this.key == key) get(sharedPreferences, key, defaultValue)
            .run { postValue(this) }
    }

    protected abstract fun get(sharedPreferences: SharedPreferences, key: String, defaultValue: T): T

    protected abstract fun register(value: T)

    override fun setValue(value: T?) {
        super.setValue(value)
        if (value == null) delete()
        else register(value)
    }

    private fun delete() {
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
    }
}
