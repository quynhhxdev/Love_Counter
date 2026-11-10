package com.example.lovecounter.data.model

import com.example.lovecounter.utils.handle.DateUtils
import java.util.Calendar

data class PersonProfile(
    val name: String = "",
    val gender: String = "MALE",
    val dateOfBirth: String = ""
) {
    fun calculateAge(): String {
        if (dateOfBirth.isEmpty()) return "26"
        return try {
            val dob = DateUtils.parseDate(dateOfBirth) ?: return "26"
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age < 0) "0" else age.toString()
        } catch (_: Exception) {
            "26"
        }
    }
}

data class CoupleProfile(
    val maleProfile: PersonProfile = PersonProfile(name = "Sasuke", gender = "MALE", dateOfBirth = ""),
    val femaleProfile: PersonProfile = PersonProfile(name = "Naruto", gender = "FEMALE", dateOfBirth = "")
)