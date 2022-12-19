import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.ActorSystem
import scala.io.StdIn
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Try,Success,Failure}

object WebServer extends App {

    implicit val spawnSystem = ActorSystem(SpawnProtocol(), "spawn")
    // implicit values required by the server machinery
    implicit val actorSystem = akka.actor.ActorSystem("Sistema-de-Mensajes")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = actorSystem.dispatcher
		
    // Define la ruta más básica; si se envía  a /, se devuelve un texto
    def helloRoute: Route = pathEndOrSingleSlash {
        complete("Bienvenido al sistema de mensajería")
    }
    

    def messageRoute = 
        // Defino que pasa si se envía al endpoint message/Algo
        pathPrefix("message" / Segment) { trainerId =>
            // Espera a que este listo el Chat, ya que esta definido a partir de Futures
            Await.ready(ChatSessionMap.findOrCreate(trainerId)(spawnSystem).webflow(), Duration.Inf).value.get match {
                // De estar listo, envía el chat y UserId recibidos a handleWebSocketMessages
                case Success(value) => handleWebSocketMessages(value)
                case Failure(exception) => 
                    println(exception.getMessage())
                    failWith(exception)
            }
        }
		
    // Enlaza la ruta al endpoint usando HTTP
    val binding = Http(spawnSystem).bindAndHandle(helloRoute ~ messageRoute, "localhost", 8080)
    println("Server running...")
		
    // Cuando lee una linea apaga el servidor
    StdIn.readLine()
    binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
    println("Server is shut down")
} 