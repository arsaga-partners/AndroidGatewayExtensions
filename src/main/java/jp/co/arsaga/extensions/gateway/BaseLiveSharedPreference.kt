package jp.co.arsaga.extensions.gateway

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData

class LiveIntPreference(
    sharedPreference: SharedPreferences,
    key: String,
) : BaseLiveSharedPreference<Int>(sharedPreference, key, 0) {
    override fun get(
        sharedPreferences: SharedPreferences,
        key: String, defaultValue: Int
    ): Int = sharedPreferences.getInt(key, defaultValue)

    override fun register(value: Int) {
        sharedPreference.edit().putInt(key, defaultValue).apply()
    }
}

class LiveStringPreference(
    sharedPreference: SharedPreferences,
    key: String,
) : BaseLiveSharedPreference<String>(sharedPreference, key, "") {
    override fun get(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: String
    ): String = sharedPreferences.getString(key, defaultValue) ?: defaultValue

    override fun register(value: String) {
        sharedPreference.edit().putString(key, defaultValue).apply()
    }
}

abstract class BaseLiveSharedPreference<T : Any>(
    protected val sharedPreference: SharedPreferences,
    protected val key: String,
    protected val defaultValue: T
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
