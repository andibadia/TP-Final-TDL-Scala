package code

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props,  Scheduler, SpawnProtocol}
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import scala.concurrent.duration.DurationInt
import akka.util.Timeout
import scala.concurrent.{ Future, ExecutionContext }




object Main extends App {
  def behavior(balance: Int): Behavior[MensajeCuentaBancaria] = Behaviors.receiveMessage  {
      case Depositar(cantidad) => behavior(balance + cantidad)
      case Retirar(cantidad) => behavior(balance - cantidad)
      case ObtenerBalance(replyTo) =>
        replyTo ! balance
        Behaviors.same
  }
  val actorSystem = ActorSystem(SpawnProtocol(), name="BankActorSystem")

  implicit val timeout: Timeout=Timeout(2.seconds)
  implicit val scheduler: Scheduler = actorSystem.scheduler
  import actorSystem.executionContext

  val cuentaBancariaFuture: Future[ActorRef[MensajeCuentaBancaria]] =
    actorSystem.ask[ActorRef[MensajeCuentaBancaria]] { ref =>
      Spawn[MensajeCuentaBancaria](
        behavior=behavior(balance=0),
        name="cuenta1",
        props = Props.empty,
        replyTo=ref
      )
    }
  val future = for{
    cuentaBancaria <- cuentaBancariaFuture
    balance1 <- cuentaBancaria ? ObtenerBalance
    _         = println("Su Balance actual es de " + balance1)
    _         = cuentaBancaria ! Depositar(200)
    _         = cuentaBancaria ! Retirar(50)
    balance2  <- cuentaBancaria ? ObtenerBalance
    _         = println("Su balance actual es de " + balance2)
  } yield()
  actorSystem.terminate()

}
