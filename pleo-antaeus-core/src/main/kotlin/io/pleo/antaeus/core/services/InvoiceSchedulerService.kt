package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import mu.KotlinLogging
import java.math.BigDecimal
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class InvoiceSchedulerService(
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService,
    private val batchSize: Int = 50,
    private val intervalMs: Long = 5000
) {
    private val invoiceSchedulerTask: Timer = Timer()

    fun run() {
        invoiceSchedulerTask.scheduleAtFixedRate(timerTask { runInvoiceSchedulingBatch() }, intervalMs, intervalMs)
    }

    internal fun runInvoiceSchedulingBatch() {
        val outstandingCustomers = customerService.fetchCustomersWithNoInvoiceInCurrentMonth(batchSize);
        logger.debug("Running invoice creation for outstanding customers: $outstandingCustomers")

        for (customer in outstandingCustomers) {
            // TODO: Could be optimised further with a bulk invoice creation to reduce DB roundtrips
            invoiceService.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = InvoiceStatus.PENDING
            )
            logger.debug("Created invoice for Customer: ${customer.id}")
        }
    }
}
