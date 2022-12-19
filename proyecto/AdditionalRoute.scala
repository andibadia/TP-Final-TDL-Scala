implicit val spawnSystem = ActorSystem(SpawnProtocol(), "spawn")

def messageRoute = 
        // pattern matching on the URL
        pathPrefix("message" / Segment) { trainerId =>
            // await on the webflow materialization pending session actor creation by the spawnSystem
            Await.ready(ChatSessionMap.findOrCreate(trainerId).webflow(), Duration.Inf).value.get match {
                case Success(value) => handleWebSocketMessages(value)
                case Failure(exception) => 
                    println(exception.getMessage())
                    failWith(exception)
            }
        }
