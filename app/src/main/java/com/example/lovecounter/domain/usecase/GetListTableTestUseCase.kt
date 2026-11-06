package com.example.lovecounter.domain.usecase

import com.example.lovecounter.data.database.table.TableTestEntity
import com.example.lovecounter.data.repositories.LocalRepository
import javax.inject.Inject

class GetListTableTestUseCase @Inject constructor(
    private val localRepository: LocalRepository
) : UseCase<UseCase.Param, List<TableTestEntity>>() {
    override suspend fun execute(param: Param): List<TableTestEntity> {
        return localRepository.getListTableTest()
    }
}
