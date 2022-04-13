package io.pleo.antaeus.core.services

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.joda.time.DateTime

class BillingServiceTest {
    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }

    @Test
    fun `should process payments successfully`() {
        // Setup
        val testInvoices = listOf(
            Invoice(
                1,
                2,
                Money(10.toBigDecimal(), Currency.EUR),
                InvoiceStatus.PENDING,
                DateTime.now()
            ),
            Invoice(
                5,
                10,
                Money(55.toBigDecimal(), Currency.USD),
                InvoiceStatus.PENDING,
                DateTime.now()
            )
        )

        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices(2) } returns testInvoices
            every { updateInvoicesStatus(any(), any()) } returns Unit
        }

        val invoiceService = InvoiceService(dal = dal)
        val billingService =
            BillingService(
                paymentProvider = paymentProvider,
                invoiceService = invoiceService,
                batchSize = 2,
                interval = 2000
            )

        // Execute
        billingService.runBillingBatch()

        // Verify test
        verify { paymentProvider.charge(testInvoices[0]) }
        verify { paymentProvider.charge(testInvoices[1]) }

        verify { dal.fetchUnpaidInvoices(2) }
        verify { dal.updateInvoicesStatus(testInvoices, InvoiceStatus.PAID) }

        confirmVerified()
    }


    @Test
    fun `should skip already paid invoice status`() {
        // Setup
        val testInvoices = listOf(
            Invoice(
                1,
                2,
                Money(10.toBigDecimal(), Currency.EUR),
                InvoiceStatus.PENDING,
                DateTime.now()
            ),
            Invoice(
                5,
                10,
                Money(55.toBigDecimal(), Currency.USD),
                InvoiceStatus.PAID,
                DateTime.now()
            )
        )

        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices(2) } returns testInvoices
            every { updateInvoicesStatus(any(), any()) } returns Unit
        }

        val invoiceService = InvoiceService(dal = dal)
        val billingService =
            BillingService(
                paymentProvider = paymentProvider,
                invoiceService = invoiceService,
                batchSize = 2,
                interval = 2000
            )

        // Execute
        billingService.runBillingBatch()

        // Verify test
        verify { paymentProvider.charge(testInvoices[0]) }
        verify(exactly = 0) { paymentProvider.charge(testInvoices[1]) }

        verify { dal.updateInvoicesStatus(listOf(testInvoices[0]), InvoiceStatus.PAID) }

        confirmVerified()
    }

    @Test
    fun `should handle failed charges`() {
        // Setup
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(match { it.id == 1 }) } returns false
            every { charge(match { it.id == 5 }) } throws NetworkException()
        }

        val testInvoices = listOf(
            Invoice(
                1,
                2,
                Money(10.toBigDecimal(), Currency.EUR),
                InvoiceStatus.PENDING,
                DateTime.now()
            ),
            Invoice(
                5,
                10,
                Money(55.toBigDecimal(), Currency.USD),
                InvoiceStatus.PENDING,
                DateTime.now()
            )
        )

        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices(2) } returns testInvoices
            every { updateInvoicesStatus(any(), any()) } returns Unit
        }

        val invoiceService = InvoiceService(dal = dal)
        val billingService =
            BillingService(
                paymentProvider = paymentProvider,
                invoiceService = invoiceService,
                batchSize = 2,
                interval = 2000
            )

        // Execute
        billingService.runBillingBatch()

        // Verify test
        verify { paymentProvider.charge(testInvoices[0]) }
        verify { paymentProvider.charge(testInvoices[1]) }

        verify(exactly = 0) { dal.updateInvoicesStatus(any(), any()) }

        confirmVerified()
    }
}