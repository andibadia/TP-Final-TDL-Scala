import akka.actor.typed.{ Behavior, SpawnProtocol, ActorRef }
import akka.actor.typed.scaladsl.Behaviors

object SessionActor {

    import CoreChatEvents._
    import WebSocketsEvents._

    def receive(websocket: Option[ActorRef[WebSocketsEvent]]): Behavior[CoreChatEvent] = Behaviors.receiveMessage {
        // received a message from the user, route to this phone
        case UserMessage(msg, phone) => 
            println(s"Sending message $msg to phone $phone")
            // interface with ClientMessageService here
            Behaviors.same
        
        // received connection request, update websocket
        case Connected(websocket) => 
            println("Se ha conectado un nuevo chat")
            receive(Some(websocket))
            
        case Disconnected => 
            println("Desconectado")
            Behaviors.stopped

        case Failed(ex) => 
            throw new RuntimeException(ex)
    }
}
