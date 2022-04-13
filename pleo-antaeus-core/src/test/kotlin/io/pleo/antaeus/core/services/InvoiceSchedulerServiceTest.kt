package io.pleo.antaeus.core.services

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test

class InvoiceSchedulerServiceTest {
    private val testCustomers = listOf(
        Customer(
            3,
            Currency.DKK
        ),
        Customer(
            5,
            Currency.EUR
        )
    )

    private val dal = mockk<AntaeusDal> {
        every { fetchCustomersWithNoInvoiceInCurrentMonth(any()) } returns testCustomers
        every { createInvoice(any(), any(), any()) } returns null
    }

    private val testBatchSize = 10
    private val invoiceService = InvoiceService(dal = dal)
    private val customerService = CustomerService(dal = dal)
    private val invoiceSchedulerService = InvoiceSchedulerService(
        invoiceService = invoiceService,
        customerService = customerService,
        batchSize = testBatchSize
    )

    @Test
    fun `generates invoices for pending customers`() {
        invoiceSchedulerService.runInvoiceSchedulingBatch()

        verify(exactly = 1) {
            dal.createInvoice(
                any(),
                testCustomers[0],
                InvoiceStatus.PENDING
            )
        }
        verify(exactly = 1) {
            dal.createInvoice(
                any(),
                testCustomers[1],
                InvoiceStatus.PENDING
            )
        }
        confirmVerified()
    }
}

