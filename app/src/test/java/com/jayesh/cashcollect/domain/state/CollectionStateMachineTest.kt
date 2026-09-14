package com.jayesh.cashcollect.domain.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionStateMachineTest {

    @Test
    fun `pending can transition to receipt confirmed or voided`() {
        assertTrue(CollectionStateMachine.canTransition(CollectionStatus.PENDING, CollectionStatus.RECEIPT_CONFIRMED))
        assertTrue(CollectionStateMachine.canTransition(CollectionStatus.PENDING, CollectionStatus.VOIDED))
        assertFalse(CollectionStateMachine.canTransition(CollectionStatus.PENDING, CollectionStatus.CONFIRMED))
    }

    @Test
    fun `receipt confirmed can transition to confirmed or voided`() {
        assertTrue(CollectionStateMachine.canTransition(CollectionStatus.RECEIPT_CONFIRMED, CollectionStatus.CONFIRMED))
        assertTrue(CollectionStateMachine.canTransition(CollectionStatus.RECEIPT_CONFIRMED, CollectionStatus.VOIDED))
        assertFalse(CollectionStateMachine.canTransition(CollectionStatus.RECEIPT_CONFIRMED, CollectionStatus.PENDING))
    }

    @Test
    fun `confirmed can only transition to voided`() {
        assertTrue(CollectionStateMachine.canTransition(CollectionStatus.CONFIRMED, CollectionStatus.VOIDED))
        assertFalse(CollectionStateMachine.canTransition(CollectionStatus.CONFIRMED, CollectionStatus.PENDING))
        assertFalse(CollectionStateMachine.canTransition(CollectionStatus.CONFIRMED, CollectionStatus.RECEIPT_CONFIRMED))
    }

    @Test
    fun `voided is strictly terminal`() {
        assertFalse(CollectionStateMachine.canTransition(CollectionStatus.VOIDED, CollectionStatus.PENDING))
        assertFalse(CollectionStateMachine.canTransition(CollectionStatus.VOIDED, CollectionStatus.RECEIPT_CONFIRMED))
        assertFalse(CollectionStateMachine.canTransition(CollectionStatus.VOIDED, CollectionStatus.CONFIRMED))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `void reason requires non-blank string`() {
        CollectionStateMachine.validateVoidReason("   ")
    }

    @Test
    fun `valid void reason trims and passes`() {
        org.junit.Assert.assertEquals("Wrong amount", CollectionStateMachine.validateVoidReason("  Wrong amount  "))
    }
}
