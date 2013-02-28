package com.dragisak

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout

case object Tick
case object Get

class Counter extends Actor {
  var count = 0

  def receive = {
    case Tick => count += 1
    case Get  => sender ! count
  }
}

object HelloWorld extends App {
  val system = ActorSystem("HelloWorld")
  implicit val ec = system.dispatcher

  val counter = system.actorOf(Props[Counter])

  counter ! Tick
  counter ! Tick
  counter ! Tick

  implicit val timeout = Timeout(5.seconds)

  (counter ? Get) onSuccess {
    case count => println("Count is " + count)
  }

  system.shutdown()
}
