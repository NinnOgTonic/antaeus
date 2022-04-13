package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.util.Timer
import kotlin.concurrent.timerTask

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val batchSize: Int = 100,
    private val intervalMs: Long = 2000
) {
    private val billingTask: Timer = Timer()

    fun run() {
        billingTask.scheduleAtFixedRate(timerTask { runBillingBatch() }, intervalMs, intervalMs)
    }

    internal fun runBillingBatch() {

        val unpaidInvoices = invoiceService.fetchUnpaid(batchSize)

        var successfullyCharged: MutableList<Invoice> = mutableListOf()

        for (invoice in unpaidInvoices) {
            // Ensure that the invoice status is really pending, as a sanity check in addition to that in our query
            if (invoice.status != InvoiceStatus.PENDING) {
                logger.error("Attempted to bill non-pending invoice: $invoice")
                continue
            }

            // TODO: Consider optimistic locking pattern to allow multiple processors?
            try {
                /** TODO: Consider adding a processing step for the toy project, such that if our service is stopped unexpectedly we are able to send those stuck in the processing state to manual handling.
                 * Realistically this would likely be handled by splitting the transaction into an authorizing stage and capturing payment stage?
                 */
                if (paymentProvider.charge(invoice)) {
                    successfullyCharged.add(invoice)
                } else {
                    // Leave as pending, to retry later
                    logger.trace { "Failed to charge: $invoice" }
                }
            } catch (e: NetworkException) {
                // Leave as pending, to retry later
                logger.trace("Network error when calling payment provider: $e")
            } catch (e: Exception) {
                logger.error("Unhandled error when calling payment provider: $e, Invoice: $invoice")
            }
        }

        // TODO: Consider adding a lastAttemptedAt timer to failed transactions, to allow for logic not to try before some delay has passed

        if (successfullyCharged.isNotEmpty()) {
            // TODO: Processing could be sped up by signaling that we should re-run the function right away if count(unpaidInvoices) == batchSize
            invoiceService.markAsPaid(successfullyCharged)
            logger.debug("Successfully charged: $successfullyCharged")
        }
    }
}
