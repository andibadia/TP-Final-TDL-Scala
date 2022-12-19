import akka.actor.typed.ActorRef

object CoreChatEvents {
    import WebSocketsEvents._

    sealed trait CoreChatEvent
    final case class UserMessage(message: String, phoneNumber: String) extends CoreChatEvent
    final case class Connected(websocket: ActorRef[WebSocketsEvent]) extends CoreChatEvent
    final case object Disconnected extends CoreChatEvent
    final case class Failed(ex: Throwable) extends CoreChatEvent
}

object WebSocketsEvents {
    sealed trait WebSocketsEvent
    final case class MessageToUser(phoneNumber: String, message: String) extends WebSocketsEvent
    final case object Complete extends WebSocketsEvent
    final case class Failure(ex: Throwable) extends WebSocketsEvent
}
