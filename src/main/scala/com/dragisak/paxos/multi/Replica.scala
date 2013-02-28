package com.dragisak.paxos.multi

import akka.actor.{ActorLogging, Actor}

class Replica(val numLeaders: Int) extends Actor with ActorLogging{

  lazy val leaders = (0 until numLeaders).map(i => context.actorFor("../leader-" + i)).toSet

  var state = Seq[Operation]()

  var slotNum = 1L

  var proposals = Set[Proposal]()

  var decisions = Set[Decision]()

  private def propose(p: Command) {

    if (!(decisions contains Decision(slotNum, p))) {
      val s1 = (proposals.map(_.s) ++ decisions.map(_.s) + 0L).max + 1L
      val proposal = Proposal(s1, p)
      proposals = proposals + proposal

      leaders foreach (_ ! proposal)
    }
  }

  private def perform(p: Command) {
    decisions.find(d => d.p == p && d.s < slotNum) match {
      case Some(_) => slotNum = slotNum + 1L
      case None =>
        val result = p.op.execute
        state = state :+ p.op
        slotNum = slotNum + 1L

        send(p.k, Response(p.cid, result))
    }

  }

  private def send(dest: String, response: Response) {
    log.info("Got " + response)
    //context.actorFor(dest) ! response

  }

  def receive = {

    case Request(p) =>
      log.info("Got " + p)
      propose(p)

    case d @ Decision(_, p) => {
      log.info("Got " + d)

      decisions = decisions + d

      decisions.filter(_.s == slotNum) foreach {
        case Decision(_, p1) =>
          for (
            Proposal(_, p2) <- proposals.find(prop => prop.s == slotNum && prop.p != p1)
          ) yield propose(p2)


          perform(p1)
      }

    }

    case GetState =>
      sender ! state

  }

}
