/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    /**
     * Returns a list of [limit] unpaid invoices.
     * To fetch all unpaid invoices set the [limit] = 0
     */
    fun fetchUnpaid(limit: Int = 100): List<Invoice> {
        return dal.fetchUnpaidInvoices(limit)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun markAsPaid(invoices: List<Invoice>) {
        return dal.updateInvoicesStatus(invoices, InvoiceStatus.PAID)
    }


}
