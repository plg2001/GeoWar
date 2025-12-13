package com.example.geowar.data

import com.example.geowar.data.auth.AuthApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://plg2001.pythonanywhere.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
}
