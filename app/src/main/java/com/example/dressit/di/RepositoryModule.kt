package com.example.dressit.di

import android.content.Context
import com.example.dressit.data.repository.BookingRepository
import com.example.dressit.data.repository.NotificationRepository
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
object RepositoryModule {

    @Provides
    @Singleton
    fun providePostRepository(
        @ApplicationContext context: Context,
        notificationRepository: NotificationRepository
    ): PostRepository {
        return PostRepository(context, notificationRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository {
        return UserRepository()
    }
    
    @Provides
    @Singleton
    fun provideNotificationRepository(@ApplicationContext context: Context): NotificationRepository {
        return NotificationRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideBookingRepository(@ApplicationContext context: Context): BookingRepository {
        return BookingRepository(context)
    }
} 