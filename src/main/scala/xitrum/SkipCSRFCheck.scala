package xitrum

/**
 * By default all non-GET requests are checked for anti-CSRF token.
 * Make your action (normally APIs for machines, e.g. smartphones) extend this
 * trait if you want to skip the check. Subclasses of the action will also not
 * be checked.
 */
trait SkipCSRFCheck
