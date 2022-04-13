# The solution proposal

N.B.: I must warn if unusual Kotlin patterns which are not best practice are used throughout the submission. This
codebase is the first Kotlin codebase I have touched.

My initial thinking when it comes problems of this kind is to split the problem into two:

1. The generation of invoices which is due to be paid, owning the first of month business logic.
2. The automatic payment processing of pending invoices, including handling of errors and retries.

By making separating services responsible for scheduling invoices, and executing the billing service at all times makes
it easy to handle errors be repeat attempts at charging, which could be extended with logic to limit the number of
retries and also warn the operations/tech that something is not properly handled. Or alternatively advise the customer
that there is a problem with payment.

In terms of scheduling the invoices I thought it might be a good idea to extend the first of month logic to run at all
times, and then check if an invoice has been generated for the current month. Doing so would implicitly generate an
invariant where, if the systems are functional, would generate invoices for existing customers on the first of the month
-- but also, that if the systems are down, they will be generated once functional again. And in addition it would also
allow us to generate invoices for clients onboarded during the month.

While simple this approach ensures that:

* The solution can easily segment and scale using concurrency with some additional work.
* Handling of rare events, such as a total breakdown of the network, the payment provider, or our own hosting starting
  at the first of the month and spanning more than 24hrs, without "skipping" the charging or invoicing that given month.
* Customers who may not have funds available via the payment method, can be captured past the first of the month, once
  the issues are sorted out.

To further scale in a distributed way this approach dividing the workload into batches the system would have to be
extended with a number of concurrent consumers, which might use a coordination approach such as an optimistic locking
mechanism prior to processing the work in the unprocessed batches.

## Testing

Using the templates in this codebase we only have access to unit testing. The unit testing approach will limit our
capabilities to properly test our solution, without a proper database layer. The testing of the InvoiceSchedulerService
will make this especially apparent, as much of the core scheduling logic is placed in AntaeusDal, which is mocked out in
unit tests.

Given the scope this toy task and my previous experience in Kotlin I felt it was a little over the top to go ahead and
add additional End-to-End tests.
