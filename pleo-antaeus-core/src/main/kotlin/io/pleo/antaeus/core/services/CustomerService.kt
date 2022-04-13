/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice

class CustomerService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Customer> {
        return dal.fetchCustomers()
    }

    /**
     * Returns a list of [limit] customers who has no invoice for the current month.
     * To fetch all customers without an invoice, set the [limit] = 0
     */
    fun fetchCustomersWithNoInvoiceInCurrentMonth(limit: Int = 100): List<Customer> {
        return dal.fetchCustomersWithNoInvoiceInCurrentMonth(limit)
    }

    fun fetch(id: Int): Customer {
        return dal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
    }
}
