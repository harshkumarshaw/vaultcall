package com.vaultcall.di

import com.vaultcall.service.GreetingPlayer
import com.vaultcall.service.NotificationHelper
import com.vaultcall.service.SpamDetector
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for service-layer dependencies.
 *
 * Most service classes are @Singleton with @Inject constructors,
 * so Hilt handles them automatically. This module exists for any
 * future manual bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    // Currently all service classes use @Inject constructors with @Singleton.
    // Add manual @Provides methods here if needed in the future.
}
