package com.example.geowar.repository

import android.content.Context
import androidx.core.content.edit
import com.example.geowar.PREF_AVATAR_SEED
import com.example.geowar.PREF_USER_ID
import com.example.geowar.PREFS_NAME
import com.example.geowar.data.auth.AuthApi
import com.example.geowar.models.UserDetails

class UserRepository(private val apiService: AuthApi, private val context: Context) {

    // Resa pubblica per utilizzo nel ViewModel
    fun getUserId(): Int {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getInt(PREF_USER_ID, -1)
    }

    fun saveAvatarSeed(avatarSeed: String?) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit {
            putString(PREF_AVATAR_SEED, avatarSeed)
        }
    }

    suspend fun getCurrentUserDetails(): Result<UserDetails> {
        val userId = getUserId()
        if (userId == -1) {
            return Result.failure(Exception("ID utente non trovato"))
        }

        return try {
            val response = apiService.getUserDetails(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Errore nel recupero dei dati utente: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserDetails(username: String, email: String, avatarSeed: String?): Result<Unit> {
        val userId = getUserId()
        if (userId == -1) {
            return Result.failure(Exception("ID utente non trovato"))
        }

        return try {
            val userDetails = UserDetails(username = username, email = email, avatar_seed = avatarSeed)
            val response = apiService.updateUserDetails(userId, userDetails)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Errore durante l'aggiornamento dei dati: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
