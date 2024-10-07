package com.guillermonegrete.tts.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DefaultSettingsRepository @Inject constructor(
    private val preferences: SharedPreferences,
    @ApplicationContext private val context: Context
): SettingsRepository {


    override fun setLanguageTo(language: String) {
        preferences.edit { putString(LANGUAGE_TO_KEY, language) }
    }

    override fun setLanguageFrom(language: String) {
        preferences.edit { putString(LANGUAGE_FROM_KEY, language) }
    }

    override suspend fun setShowSavedWords(enabled: Boolean) {
        val key = booleanPreferencesKey(SHOW_WORDS_KEY)
        context.dataStore.edit { settings -> settings[key] = enabled }
    }

    override fun getLanguageTo(): String {
        return preferences.getString(LANGUAGE_TO_KEY, null) ?: "en"
    }

    override fun getLanguageFrom(): String {
        return preferences.getString(LANGUAGE_FROM_KEY, null) ?: "auto"
    }

    override fun showSavedWords(): Flow<Boolean> {
        val key = booleanPreferencesKey(SHOW_WORDS_KEY)
        return context.dataStore.data.map { preferences ->
            // No type safety.
            preferences[key] ?: true
        }
    }

    override fun getImportTabPosition(): Flow<Int> {
        val key = intPreferencesKey(IMPORT_TAB_KEY)
        return context.dataStore.data.map { preferences ->
            // No type safety.
            preferences[key] ?: 0
        }
    }

    override suspend fun setImportTabPosition(pos: Int) {
        val key = intPreferencesKey(IMPORT_TAB_KEY)
        context.dataStore.edit { settings -> settings[key] = pos }
    }

    companion object{
        const val LANGUAGE_TO_KEY = "translate_to_pref"
        const val LANGUAGE_FROM_KEY = "translate_from_pref_key"
        const val SHOW_WORDS_KEY = "show_words_pref"
        const val IMPORT_TAB_KEY = "import_tab"
    }
}
