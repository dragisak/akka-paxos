package com.dragisak

import akka.actor._
import akka.pattern.ask
import paxos._
import scala.concurrent.duration._
import akka.util.Timeout
import concurrent.{Await, Future}

object HelloPaxos extends App {
  val system = ActorSystem("HelloPaxos")
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(30.seconds)

  try {
    val crashes = 10
    val cntLeaders = 3
    val messages = 1000

    val acceptors = (for (i <- 0 until crashes * 2 + 1) yield (system.actorOf(Props[Acceptor], name = s"acceptor-${i}"))).toSet
    val replicas = (for (i <- 0 until crashes + 1) yield (system.actorOf(Props(new Replica(cntLeaders)), name = s"replica-${i}"))).toSet
    val leaders = for (i <- 0 until cntLeaders) yield (system.actorOf(Props(new Leader(i, acceptors, replicas)), name = s"leader-${i}"))

    Thread.sleep(500)

    println(s"Sending ${messages} messages")
    for (i <- 0 until messages) {
      val req = Request(Command("yo", i, Operation(i.toString)))
      replicas.foreach(_ ! req)
    }


    val waitTime = messages.toLong * 50
    println(s"Waiting ${waitTime/1000} sec ...")
    Thread.sleep(waitTime)

    val res = Await.result(Future.sequence(replicas.toSeq.map(r => (r ? GetState).mapTo[Seq[Operation]])), 20.seconds)

    assert(res.size == replicas.size, s"Expected responses from ${replicas.size} but got ${res.size}")
    assert(res.head.size == messages, s"Expected ${messages} messages but received ${res.head.size}")
    val s = res.toSet
    assert(s.size == 1, s"All replicas must receive same sequence of events ${s}")

    println (s"Successfully received ${res.size} responses with ${res.head.size} messages each")
    println("Looking good")
  }
  finally {
    println("Bye.")
    system.shutdown()
  }

}
