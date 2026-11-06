package com.example.lovecounter.domain.usecase

import com.example.lovecounter.data.model.Language
import com.example.lovecounter.data.repositories.LocalRepository
import javax.inject.Inject

class GetListLanguageUseCase @Inject constructor(
    private val localRepository: LocalRepository
) : UseCase<UseCase.Param, List<Language>>() {
    override suspend fun execute(param: Param): List<Language> {
        return localRepository.getListLanguage()
    }
}
