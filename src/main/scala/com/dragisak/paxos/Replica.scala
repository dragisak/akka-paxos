package com.dragisak.paxos

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import scala.annotation.tailrec

class Replica(val numLeaders: Int) extends Actor with ActorLogging {

  lazy val leaders = (0 until numLeaders).map(i => context.actorFor("../leader-" + i)).toSet

  var state = Seq[Operation]()

  var slotNum = 1L

  var proposals = Map[Long, Proposal]()

  var decisions = Map[Long, Command]()

  def propose(p: Command) {

    if (!alreadyDecided(p, slotNum)) {
      maxSeenSoFar = maxSeenSoFar + 1L
      val proposal = Proposal(maxSeenSoFar, p)

      proposals = proposals + (proposal.s -> proposal)

      leaders foreach (_ ! proposal)
    }
  }

  var maxSeenSoFar = 0L

  def alreadyDecided(command: Command, slotNo: Long) = decisions.contains(slotNo)

  private def performAndIncrement(slot: Long, p: Command): Long = {
    log.debug("Performing {} on {}", p, slot)
    decisions.find {
      case (s: Long, c: Command) => c == p && s < slot
    } match {
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

      decisions = decisions + (d.s -> d.p)
      if (maxSeenSoFar < d.s) maxSeenSoFar = d.s

      log.debug("Got decision {} current slot {}", d, slotNum)


      @tailrec def loop(slot: Long, cmd: Option[Command]): Long = cmd match {
        case None => slot
        case Some(c) =>
          for (
            Proposal(_, p2) <- proposals.get(slot) if p2 != c
          ) yield propose(p2)

          val newSlotNum = performAndIncrement(slot, c)
          loop(newSlotNum, decisions.get(newSlotNum))
      }


      slotNum = loop(slotNum, decisions.get(slotNum))

    }

    case GetState => sender ! state

  }

}
