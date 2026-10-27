package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "airdrive_settings")

data class AppSettings(
    val autoStartOnWifi: Boolean = true,
    val onlyTrustedNetworks: Boolean = true,
    val trustedNetworks: Set<String> = emptySet(),
    val port: Int = 2121,
    val allowAnonymous: Boolean = true,
    val allowWrite: Boolean = true,
    val username: String = "admin",
    val password: String = "admin",
    val rootPath: String = "/storage/emulated/0",
    val enableWakeLock: Boolean = true,
    val advertiseMdns: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val AUTO_START_ON_WIFI = booleanPreferencesKey("auto_start_on_wifi")
        val ONLY_TRUSTED_NETWORKS = booleanPreferencesKey("only_trusted_networks")
        val TRUSTED_NETWORKS = stringSetPreferencesKey("trusted_networks")
        val PORT = intPreferencesKey("port")
        val ALLOW_ANONYMOUS = booleanPreferencesKey("allow_anonymous")
        val ALLOW_WRITE = booleanPreferencesKey("allow_write")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val ROOT_PATH = stringPreferencesKey("root_path")
        val ENABLE_WAKE_LOCK = booleanPreferencesKey("enable_wake_lock")
        val ADVERTISE_MDNS = booleanPreferencesKey("advertise_mdns")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            autoStartOnWifi = preferences[Keys.AUTO_START_ON_WIFI] ?: true,
            onlyTrustedNetworks = preferences[Keys.ONLY_TRUSTED_NETWORKS] ?: true,
            trustedNetworks = preferences[Keys.TRUSTED_NETWORKS] ?: emptySet(),
            port = preferences[Keys.PORT] ?: 2121,
            allowAnonymous = preferences[Keys.ALLOW_ANONYMOUS] ?: true,
            allowWrite = preferences[Keys.ALLOW_WRITE] ?: true,
            username = preferences[Keys.USERNAME] ?: "admin",
            password = preferences[Keys.PASSWORD] ?: "admin",
            rootPath = preferences[Keys.ROOT_PATH] ?: "/storage/emulated/0",
            enableWakeLock = preferences[Keys.ENABLE_WAKE_LOCK] ?: true,
            advertiseMdns = preferences[Keys.ADVERTISE_MDNS] ?: true
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun setAutoStartOnWifi(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START_ON_WIFI] = enabled }
    }

    suspend fun setOnlyTrustedNetworks(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ONLY_TRUSTED_NETWORKS] = enabled }
    }

    suspend fun addTrustedNetwork(ssid: String) {
        val cleanSsid = ssid.removeSurrounding("\"").trim()
        if (cleanSsid.isEmpty() || cleanSsid == "<unknown ssid>") return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TRUSTED_NETWORKS] ?: emptySet()
            prefs[Keys.TRUSTED_NETWORKS] = current + cleanSsid
        }
    }

    suspend fun removeTrustedNetwork(ssid: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.TRUSTED_NETWORKS] ?: emptySet()
            prefs[Keys.TRUSTED_NETWORKS] = current - ssid
        }
    }

    suspend fun setPort(port: Int) {
        context.dataStore.edit { it[Keys.PORT] = port }
    }

    suspend fun setAllowAnonymous(allow: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_ANONYMOUS] = allow }
    }

    suspend fun setAllowWrite(allow: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_WRITE] = allow }
    }

    suspend fun setCredentials(username: String, pass: String) {
        context.dataStore.edit {
            it[Keys.USERNAME] = username
            it[Keys.PASSWORD] = pass
        }
    }

    suspend fun setRootPath(path: String) {
        context.dataStore.edit { it[Keys.ROOT_PATH] = path }
    }

    suspend fun setEnableWakeLock(enable: Boolean) {
        context.dataStore.edit { it[Keys.ENABLE_WAKE_LOCK] = enable }
    }

    suspend fun setAdvertiseMdns(advertise: Boolean) {
        context.dataStore.edit { it[Keys.ADVERTISE_MDNS] = advertise }
    }
}
