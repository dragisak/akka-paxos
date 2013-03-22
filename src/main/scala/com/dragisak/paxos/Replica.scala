package com.dragisak.paxos

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import scala.annotation.tailrec

class Replica(val numLeaders: Int) extends Actor with ActorLogging {

  lazy val leaders = (0 until numLeaders).map(i => context.actorFor("../leader-" + i)).toSet

  var state = Seq[Operation]()

  var slotNum = 1L

  var proposals = Set[Proposal]()

  var decisions = Set[Decision]()

  def propose(p: Command) {

    if (!alreadyDecided(p, slotNum)) {
      val s1 = maxSeenSoFar  + 1L
      val proposal = Proposal(s1, p)
      proposals = proposals + proposal

      leaders foreach (_ ! proposal)
    }
  }

  def maxSeenSoFar = (proposals.map(_.s) ++ decisions.map(_.s) + 0L).max

  def alreadyDecided(command:Command, slotNo:Long) = decisions contains Decision(slotNo, command)

  private def performAndIncrement(slot: Long, p: Command): Long = {
    log.debug("Performing {} on {}", p, slot)
    decisions.find(d => d.p == p && d.s < slot) match {
      case Some(_) => slot + 1L
      case None =>
        log.debug("Add state {}", p.op)
        val result = p.op.execute
        state = state :+ p.op
        sendReply(p.k, Response(p.cid, result))
        slot + 1L

    }
  }

  private def sendReply(dest: String, response: Response) {
    //context.actorFor(dest) ! response

  }

  def receive = LoggingReceive {

    case Request(p) => propose(p)

    case d: Decision => {

      decisions = decisions + d

      log.debug("Got decision {} current slot {}", d, slotNum)


      @tailrec def loop(slot: Long, cmds: Set[Command]): Long = {
        if (cmds.isEmpty) slot
        else {
          val c = cmds.head // there should be no two commands with same slot

          for (
            Proposal(_, p2) <- proposals.find(prop => prop.s == slot && prop.p != c)
          ) yield propose(p2)

          val newSlotNum = performAndIncrement(slot, c)
          loop(newSlotNum, decisions.filter(_.s == newSlotNum).map(_.p))
        }
      }


      slotNum = loop(slotNum, decisions.filter(_.s == slotNum).map(_.p))

      /*
      decisions.filter(_.s == slotNum) foreach {
        case Decision(_, p1) =>
          for (
            Proposal(_, p2) <- proposals.find(prop => prop.s == slotNum && prop.p != p1)
          ) yield propose(p2)

          perform(p1)
      }
       */
    }

    case GetState => sender ! state

  }

}
