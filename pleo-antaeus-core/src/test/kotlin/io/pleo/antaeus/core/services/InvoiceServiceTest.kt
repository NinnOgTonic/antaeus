package io.pleo.antaeus.core.services

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.random.Random

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
        every { fetchUnpaidInvoices(any()) } returns emptyList()
        every { createInvoice(any(), any(), any()) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will query with non-default limit`() {
        assert(invoiceService.fetchUnpaid(500).isEmpty())

        verify { dal.fetchUnpaidInvoices(500) }
        confirmVerified()
    }

    @Test
    fun `will create invoice`() {
        val testCustomer = Customer(
            id = 45,
            currency = Currency.EUR
        )

        val testAmount = Money(
            value = BigDecimal(Random.nextDouble(10.0, 500.0)),
            currency = Currency.EUR
        )

        invoiceService.createInvoice(
            amount = testAmount,
            customer = testCustomer,
            status = InvoiceStatus.PENDING
        )

        verify { dal.createInvoice(testAmount, testCustomer, InvoiceStatus.PENDING) }
        confirmVerified()
    }
}
