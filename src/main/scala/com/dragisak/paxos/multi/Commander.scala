package com.dragisak.paxos.multi

import akka.actor._

class Commander(
  val l: ActorRef,
  val acceptors: Set[ActorRef],
  val replicas: Set[ActorRef],
  val pValue: PValue
  ) extends Actor with ActorLogging{

  var waitFor = acceptors


  override def preStart = acceptors.foreach(_ ! Phase2a(self, pValue))


  def receive = {

    case Phase2b(a, b1) =>
      if (b1 == pValue.b) {
        waitFor = waitFor - a
        if (waitFor.size < acceptors.size/2) {
          replicas.foreach(_ ! Decision(pValue.s, pValue.p))
        }
        self ! PoisonPill
      } else {
        l !  Preempted(b1)
        self ! PoisonPill

      }

  }

}
