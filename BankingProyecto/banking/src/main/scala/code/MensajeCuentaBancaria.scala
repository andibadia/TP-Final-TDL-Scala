package code
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}

sealed trait MensajeCuentaBancaria

case class Depositar(cantidad: Int) extends MensajeCuentaBancaria
case class Retirar(cantidad: Int)  extends MensajeCuentaBancaria
case class ObtenerBalance(replyTo: ActorRef[Int])        extends MensajeCuentaBancaria
