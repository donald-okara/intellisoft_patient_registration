package ke.don.intellisoft_patient_registration.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests the shared `Redacted` behavior once, via one representative value class (`FirstName`),
 * rather than per PII type - all of them get this for free by declaring `: Redacted`.
 */
class RedactedTest {
    @Test
    fun toStringIsRedactedNotTheRealValue() {
        val name = FirstName("Jane")

        assertEquals("[REDACTED]", name.toString())
        assertFalse(name.toString().contains("Jane"))
    }

    @Test
    fun equalsAndHashCodeStillCompareTheRealValue() {
        assertEquals(FirstName("Jane"), FirstName("Jane"))
        assertEquals(FirstName("Jane").hashCode(), FirstName("Jane").hashCode())
    }
}
