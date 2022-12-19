import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import scala.io.StdIn
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.{ Message, TextMessage }

object WebServer extends App {
		
    // implicit values required by the server machinery
    implicit val actorSystem = akka.actor.ActorSystem("messaging-actorsystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = actorSystem.dispatcher
		
    // define a basic route ("/") that returns a welcoming message
    def helloRoute: Route = pathEndOrSingleSlash {
        complete("Welcome to messaging service")
    }
    
    // create WebSocket route
   def affirmRoute = path("affirm") {
        handleWebSocketMessages(
            Flow[Message].collect {
                case TextMessage.Strict(text) => TextMessage("You said " + text)
            }   
        )
   }
		
    // bind the route using HTTP to the server address and port
    val binding = Http().bindAndHandle(helloRoute ~ affirmRoute, "localhost", 8080)
    println("Server running...")
		
    // kill the server with input
    StdIn.readLine()
    binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
    println("Server is shut down")
} 