import akka.actor.typed.{ ActorSystem, SpawnProtocol }

object ChatSessionMap {
    
    // mutable data structure holding all the chat sessions. Definition to follow
    private var sessions: Map[String, ChatSession] = Map.empty[String, ChatSession]
    

    def findOrCreate(userId: String)(implicit system: ActorSystem[SpawnProtocol.Command]): ChatSession = sessions.getOrElse(userId, create(userId))
    
    // creates chat sessions. Role of the typed ActorSystem will be explained soon
    private def create(userId: String)(implicit system: ActorSystem[SpawnProtocol.Command]) = {
        val session = ChatSession(userId)
        sessions += userId -> session
        session
    }
}