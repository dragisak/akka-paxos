package akkapaxos

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import scala.annotation.tailrec
import scala.collection.SortedMap

class Replica[S,E](val numLeaders: Int) extends Actor with ActorLogging {

  this: DistributedState[S, E] =>

  lazy val leaders = (0 until numLeaders).map(i => context.actorFor("../leader-" + i)).toSet

  var slotNum = 1L

  var state :S = startState

  var proposals = Map[Long, Proposal[E]]()

  var decisionMap = SortedMap[Long, Command[E]]()
  var decisionSet = Map[Command[E], Set[Long]]()

  def addDecision(d: Decision[E]) {
    decisionMap = decisionMap + (d.s -> d.p)
    val slots = decisionSet.getOrElse(d.p, Set())

    decisionSet = decisionSet + (d.p -> (slots + d.s))
  }

  def propose(p: Command[E]) {

    if (!alreadyDecided(p, slotNum)) {
      maxSeenSoFar = maxSeenSoFar + 1L
      val proposal = Proposal(maxSeenSoFar, p)

      proposals = proposals + (proposal.s -> proposal)

      leaders foreach (_ ! proposal)
    }
  }

  var maxSeenSoFar = 0L

  def alreadyDecided(command: Command[E], slotNo: Long) = decisionMap.contains(slotNo)

  private def performAndIncrement(slot: Long, p: Command[E]): Long = {
    log.debug("Performing {} on {}", p, slot)
    decisionSet.get(p) match {
      case Some(slots) if !slots.exists(_ < slot) =>
        log.debug("Add state {}", p.op)
        state = append(state, p.op)
        slot + 1L
      case _ =>
        slot + 1L
    }
  }


  def receive = LoggingReceive {

    case request :Request[E] => propose(request.p)

    case d: Decision[E] => {

      addDecision(d)
      if (maxSeenSoFar < d.s) maxSeenSoFar = d.s

      log.debug("Got decision {} current slot {}", d, slotNum)

      @tailrec def loop(slot: Long, cmd: Option[Command[E]]): Long = cmd match {
        case None => slot
        case Some(c) =>
          for (
            Proposal(_, p2) <- proposals.get(slot) if p2 != c
          ) yield propose(p2)

          val newSlotNum = performAndIncrement(slot, c)
          loop(newSlotNum, decisionMap.get(newSlotNum))
      }


      slotNum = loop(slotNum, decisionMap.get(slotNum))

    }

    case GetState => sender ! state

  }

}
