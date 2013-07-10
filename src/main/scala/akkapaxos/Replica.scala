package akkapaxos

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import scala.annotation.tailrec
import scala.collection.SortedMap

class Replica[S, E](val numLeaders: Int) extends Actor with ActorLogging {

  this: DistributedState[S, E] =>

  lazy val leaders = (0 until numLeaders).map(i => context.actorSelection("../leader-" + i)).toSet

  var state: S = startState

  var slotNum = 1L

  var proposals = Map[Long, Proposal[E]]()

  var decisionMap = SortedMap[Long, Command[E]]()
  var decisionSet = Map[Command[E], Set[Long]]()

  var maxSeenSoFar = 0L

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


  def alreadyDecided(command: Command[E], slotNo: Long) = decisionMap.contains(slotNo)


  def receive = LoggingReceive {

    case request: Request[E] => propose(request.p)

    case d: Decision[E] => {

      addDecision(d)
      if (maxSeenSoFar < d.s) maxSeenSoFar = d.s

      log.debug("Got decision {} current slot {}", d, slotNum)

      val (newSlotNum, newState) = updateState(slotNum, decisionMap.get(slotNum), state)


      slotNum = newSlotNum
      state = newState

    }

    case GetState => sender ! state

  }


  @tailrec private def updateState(slot: Long, cmd: Option[Command[E]], st: S): (Long, S) = cmd match {
    case None => (slot, st)
    case Some(c) =>

      replayProposals(slot, c, proposals)

      val (newSlotNum, newState) = decisionSet.get(c) match {
        case Some(slots) if !slots.exists(_ < slot) =>
          log.debug("Applying event {}", c.op)
          (slot + 1L, append(st, c.op))
        case _ =>
          (slot + 1L, st)
      }

      updateState(newSlotNum, decisionMap.get(newSlotNum), newState)
  }


  def replayProposals(slot: Long, c: Command[E], props: Map[Long, Proposal[E]]) {
    props.get(slot) match {
      case Some(Proposal(_, c2)) if c2 != c => propose(c2)
      case _ => // do nothing
    }
  }
}
