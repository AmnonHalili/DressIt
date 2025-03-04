package com.example.dressit.di

import android.content.Context
import com.example.dressit.data.repository.PostRepository
import com.example.dressit.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository {
        return UserRepository()
    }

    @Provides
    @Singleton
    fun providePostRepository(@ApplicationContext context: Context): PostRepository {
        return PostRepository(context)
    }
} 