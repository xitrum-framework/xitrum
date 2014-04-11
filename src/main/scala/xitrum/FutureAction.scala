package xitrum

/**
 * Actions extending FutureAction will be run asynchronously in a future. The
 * execution context is xitrum.Config.actorSystem.dispatcher.
 *
 * See also Action and ActorAction.
 */
trait FutureAction extends Action
