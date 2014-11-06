package main

import java.util.concurrent.CountDownLatch

import akka.actor._

case object Run
case object Msg

class Destination extends Actor {

  def receive = {
    case Msg => sender ! Msg
  }
}

class Client(id: Int,
             actor: ActorRef,
             latch: CountDownLatch,
             repeat: Long) extends Actor {

  val initalMessages = repeat

  var sent = 0L
  var received = 0L

  var startTime = 0L
  var endTime = 0L

  override def preStart: Unit = {
    startTime = System.currentTimeMillis()
  }

  override def postStop: Unit = {
    endTime = System.currentTimeMillis()
    println("actor %d sent %d received %d with in %f seconds".
      format(id, sent, received, (endTime - startTime) / 1000.0))
  }

  def receive = {
    case Msg =>
      received += 1
      if (sent < repeat) {
        actor ! Msg
        sent += 1
      } else if (received >= repeat) {
        latch.countDown()
        actor ! PoisonPill
        self ! PoisonPill
      }
    case Run =>
      for (i <- 0L until initalMessages) {
        actor ! Msg
        sent += 1
      }
  }
}

object Benchmark {

  def main(args: Array[String]): Unit = {
    val actorPairNum = args(0).toInt
    val repeat = args(1).toInt
    val actorSystem = ActorSystem("system")
    for (i <- 0 until actorPairNum) {
      val destinationActor = actorSystem.actorOf(Props(new Destination))
      val clientActor = actorSystem.actorOf(Props(new Client(i, destinationActor,
        new CountDownLatch(2), repeat)))
      clientActor ! Run
    }
  }
}