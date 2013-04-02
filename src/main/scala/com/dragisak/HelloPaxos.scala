package com.dragisak

import akka.actor._
import akka.pattern.ask
import paxos.multi._
import scala.concurrent.duration._
import akka.util.Timeout
import concurrent.Future

object HelloPaxos extends App {
  val system = ActorSystem("HelloPaxos")
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val crashes = 20
  val cntLeaders = 10
  val messages = 2000

  val acceptors = (for (i <- 0 until crashes * 2 + 1) yield (system.actorOf(Props[Acceptor], name = "acceptor-" + i))).toSet
  val replicas = (for (i <- 0 until crashes + 1) yield (system.actorOf(Props(new Replica(cntLeaders)), name = "replica-" + i))).toSet
  val leaders = for (i <- 0 until cntLeaders) yield (system.actorOf(Props(new Leader(i, acceptors, replicas)), name = "leader-" + i))


  for (i <- 0 until messages) {
    val req = Request(Command("yo", i, Operation(i.toString)))
    replicas.foreach(_ ! req)
  }

  Thread.sleep(10000)

  val res = Future.sequence(replicas.toSeq.map(r => (r ? GetState).mapTo[Seq[Operation]])).onSuccess {
    case s => {
     // s.foreach(o => println(o.map(_.id).mkString(",")))
      assert(s.toSet.size == 1, "All replicas must receive same sequence of events")
      assert(s.head.size == messages, "Expected %d messages but received %d".format (messages, s.head.size))
      println("Bye.")
      system.shutdown()
    }
  }


}
