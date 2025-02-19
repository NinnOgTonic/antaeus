/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchUnpaidInvoices(limit: Int): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .select { InvoiceTable.status eq InvoiceStatus.PENDING.toString() }
                .limit(limit)
                .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                    it[this.dueAt] = DateTime.now()
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun updateInvoicesStatus(invoices: List<Invoice>, status: InvoiceStatus) {
        val invoiceIds = invoices.map { it.id }

        transaction(db) {
            InvoiceTable
                .update({ InvoiceTable.id inList invoiceIds }) {
                    it[InvoiceTable.status] = status.toString()
                }
        }
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun fetchCustomersWithNoInvoiceInCurrentMonth(limit: Int = 0): List<Customer> {
        // TODO: This query would probably preform better if we had an index on dueAt in invoices
        return transaction(db) {
            Join(
                CustomerTable, InvoiceTable,
                onColumn = CustomerTable.id, otherColumn = InvoiceTable.customerId,
                joinType = JoinType.LEFT
            )
                .select { InvoiceTable.dueAt.isNull() or (InvoiceTable.dueAt.month() neq CurrentDateTime().month()) }
                .limit(limit)
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }
}
