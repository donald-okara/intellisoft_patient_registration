package ke.don.intellisoft_patient_registration.domain

/** BMI = weight(kg) / height(m)^2. */
fun calculateBmi(
    heightCm: Double,
    weightKg: Double,
): Double {
    val heightM = heightCm / 100.0
    return weightKg / (heightM * heightM)
}

enum class BmiStatus {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
}

/** Underweight &lt;18.5, Normal 18.5-&lt;25, Overweight &gt;=25. */
fun classifyBmi(bmi: Double): BmiStatus =
    when {
        bmi < 18.5 -> BmiStatus.UNDERWEIGHT
        bmi < 25.0 -> BmiStatus.NORMAL
        else -> BmiStatus.OVERWEIGHT
    }
