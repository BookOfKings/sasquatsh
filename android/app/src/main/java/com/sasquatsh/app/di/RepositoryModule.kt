package com.sasquatsh.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for repository dependencies.
 *
 * Repositories with @Inject constructor and @Singleton (like EventsRepository,
 * GroupsRepository, etc.) are automatically provided by Hilt. This module exists
 * for any repositories that need custom construction or @Binds interface mappings
 * as the project grows.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // EventsRepository, GroupsRepository, and other repositories that use
    // @Singleton @Inject constructor are auto-provided by Hilt.
    //
    // Add @Provides or @Binds methods here when you need:
    // - Interface-to-implementation bindings (e.g., for testing)
    // - Repositories with custom construction logic
}
