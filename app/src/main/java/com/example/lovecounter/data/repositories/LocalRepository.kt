package com.example.lovecounter.data.repositories

import com.example.lovecounter.data.database.table.TableTestEntity
import com.example.lovecounter.data.model.Language
import com.example.lovecounter.data.model.OnBoarding
import com.example.lovecounter.data.sources.LocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor(
    private val localDataSource: LocalDataSource
) {
    fun getListLanguage(): List<Language> = localDataSource.getListLanguage()

    fun getListOnBoarding(): List<OnBoarding> = localDataSource.getListOnBoarding()

    suspend fun getListTableTest(): List<TableTestEntity> = localDataSource.getListTableTest()
}
