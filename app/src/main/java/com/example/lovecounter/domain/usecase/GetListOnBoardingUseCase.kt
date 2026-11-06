package com.example.lovecounter.domain.usecase

import com.example.lovecounter.data.model.OnBoarding
import com.example.lovecounter.data.repositories.LocalRepository
import javax.inject.Inject

class GetListOnBoardingUseCase @Inject constructor(
    private val localRepository: LocalRepository
) : UseCase<UseCase.Param, List<OnBoarding>>() {
    override suspend fun execute(param: Param): List<OnBoarding> {
        return localRepository.getListOnBoarding()
    }
}
