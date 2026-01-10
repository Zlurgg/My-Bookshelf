package uk.co.zlurgg.mybookshelf.di

import org.koin.core.module.Module

/**
 * Debug build override that provides debug-only Koin modules.
 * This file shadows the release version and adds debug dependencies.
 */
object DebugModuleProvider {
    fun getModules(): List<Module> = listOf(debugModule)
}
