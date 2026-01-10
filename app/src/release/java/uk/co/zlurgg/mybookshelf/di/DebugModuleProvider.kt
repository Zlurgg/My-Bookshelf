package uk.co.zlurgg.mybookshelf.di

import org.koin.core.module.Module

/**
 * Release build version - provides empty list.
 * In debug builds, this is replaced by the debug source set version
 * which provides debug-only Koin modules.
 */
object DebugModuleProvider {
    fun getModules(): List<Module> = emptyList()
}
