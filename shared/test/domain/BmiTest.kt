package ke.don.intellisoft_patient_registration.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BmiTest {
    @Test
    fun calculatesBmiFromHeightAndWeight() {
        // 180cm, 70kg -> 70 / 1.8^2 = 21.6049...
        val bmi = calculateBmi(heightCm = 180.0, weightKg = 70.0)

        assertTrue(abs(bmi - 21.6049) < 0.001)
    }

    @Test
    fun classifiesBelow18point5AsUnderweight() {
        assertEquals(BmiStatus.UNDERWEIGHT, classifyBmi(18.49))
    }

    @Test
    fun classifiesExactly18point5AsNormal() {
        assertEquals(BmiStatus.NORMAL, classifyBmi(18.5))
    }

    @Test
    fun classifiesJustBelow25AsNormal() {
        assertEquals(BmiStatus.NORMAL, classifyBmi(24.99))
    }

    @Test
    fun classifiesExactly25AsOverweight() {
        assertEquals(BmiStatus.OVERWEIGHT, classifyBmi(25.0))
    }

    @Test
    fun classifiesAbove25AsOverweight() {
        assertEquals(BmiStatus.OVERWEIGHT, classifyBmi(30.0))
    }
}
