import scala.concurrent.{ Future, ExecutionContext }

import akka.actor.typed.{ ActorRef, ActorSystem, SpawnProtocol, Props, DispatcherSelector }
import akka.actor.typed.SpawnProtocol.Spawn
import akka.http.scaladsl.model.ws.{ TextMessage, Message }
import akka.stream.{ FlowShape, OverflowStrategy }
import akka.stream.scaladsl._
import akka.stream.typed.scaladsl.{ ActorSink, ActorSource }
import akka.util.Timeout

import CoreChatEvents._
import WebSocketsEvents._

// Dentro de las sesiones de chats se manejan los Streams de Akka que crean los flujos con Websockets

class ChatSession(userId: String)(implicit system: ActorSystem[SpawnProtocol.Command]) {
    println("Spawning new chat session")

    import akka.actor.typed.scaladsl.AskPattern._
    import scala.concurrent.duration._
    import ChatSession._
    implicit val timeout: Timeout = Timeout(3.seconds)
    implicit val ec = system.dispatchers.lookup(DispatcherSelector.default())

    // Crea un actor por fuera del ActorSystem, que usa el método Spawn para crear un Actor hijo.  Este chatsActor es el que routea los mensajes
    // Esta definido como Future ya que no se cuando estará disponible el CoreChatEvent (va a depender de cuando el usuario mande el mensaje)
    private[this] val chatsActor: Future[ActorRef[CoreChatEvent]] = 
        system.ask[ActorRef[CoreChatEvent]] { ref =>
            Spawn[CoreChatEvent](
                // El actor creado no tiene conexión a ningún WebSocket al principio
                behavior = chatsActor.receive(None),
                name = s"$userId-chat-session",
                props = Props.empty,
                replyTo = ref
            )
        }
    
    // Defino un flujo que pasa información hacia y desde el websocket. 
    def webflow(): Future[Flow[Message, Message, _]]  = chatsActor.map { session =>
        Flow.fromGraph(
            GraphDSL.create(webSocketsActor) { implicit builder => socket =>
                
                import GraphDSL.Implicits._

                //  Transforma mensajes desde los websockets al protocolo del Actor (UserMessage)
                val webSocketSource = builder.add(
                    Flow[Message].collect {
                        case TextMessage.Strict(txt) => 
                            UserMessage(txt, "111-111-1111")
                    }
                )
                
                //  Transforma mensajes del Protocolo WebSocket a un mensaje de texto websocket
                val webSocketSink = builder.add(
                    Flow[WebSocketsEvent].collect {
                        case MessageToUser(p, t) => 
                            TextMessage(p + t)
                    }
                )
                
                // Rutea los mensajes al actor de los chats
                val routeToSession = builder.add(ActorSink.actorRef[CoreChatEvent](
                    ref = session,
                    onCompleteMessage = Disconnected,
                    onFailureMessage = Failed.apply
                ))
                
                val materializedActorSource = builder.materializedValue.map(ref => Connected(ref))
                
                val merge = builder.add(Merge[CoreChatEvent](2))
                webSocketSource ~> merge.in(0)
                materializedActorSource ~> merge.in(1)
                merge ~> routeToSession
                socket ~> webSocketSink
                FlowShape(webSocketSource.in, webSocketSink.out)
            }
        )
    }
}

// Objeto ChatSession crea un elemento de clase ChatSession
object ChatSession {
    def apply(userId: String)(implicit system: ActorSystem[SpawnProtocol.Command]): ChatSession = new ChatSession(userId)

    val webSocketsActor = ActorSource.actorRef[WebSocketsEvent](completionMatcher = {
        case Complete => 
    }, failureMatcher = {
        case WebSocketsEvents.Failure(ex) => throw ex
    }, bufferSize = 5, OverflowStrategy.fail)
}
