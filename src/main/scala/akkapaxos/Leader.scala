package akkapaxos

import akka.actor._

class Leader(
  val id: Int,
  val acceptors: Set[ActorRef],
  val replicas: Set[ActorRef]
) extends Actor with LoggingFSM[LeaderState, LeaderData] {

  import Leader._

  override def preStart() {
    spawnScout(Ballot(BallotNumber(id, 0), self))
  }

  startWith(Waiting, LeaderData(Ballot(BallotNumber(id, 0), self), Map()))


  when(Waiting) {

    case Event(prop: Proposal, data) if !data.contains(prop.s) =>
      stay using (data + prop)


    case Event(Adopted(b, pVals), data) =>

      val props = combine(data.proposals, pVals)

      props.values.foreach(p => spawnCommander(PValue(data.ballot, p.s, p.p)))

      goto(Active) using LeaderData(b, props)

    case Event(Preempted(b1), data) if b1 > data.ballot =>
      val ballotNum = data.ballot.increment
      spawnScout(ballotNum)
      stay using LeaderData(ballotNum, data.proposals)

  }

  when(Active) {

    case Event(prop: Proposal, data) if !data.contains(prop.s) =>
      spawnCommander(PValue(data.ballot, prop.s, prop.p))
      stay using (data + prop)


    case Event(Preempted(b1), data) if b1 > data.ballot =>
      val ballotNum = data.ballot.increment
      spawnScout(ballotNum)
      goto(Waiting) using LeaderData(ballotNum, data.proposals)

  }

  whenUnhandled {
    case m =>
      log.debug("Ignoring {}", m)
      stay()
  }


  private def spawnCommander(pVal: PValue) {
    context.actorOf(Props(new Commander(acceptors, replicas, pVal)), name = s"commander-${pVal.b.ballotNumber}-${pVal.s}")
  }

  private def spawnScout(b: Ballot) {
    context.actorOf(Props(new Scout(acceptors, b)), name = s"scout-${b.ballotNumber}")
  }


}

object Leader {

  def combine(proposals: Map[Long, Proposal], pVals: Set[PValue]) = plus(proposals, pmax(pVals))

  def pmax(pVals: Set[PValue]): Map[Long, Proposal] = pVals
    .groupBy(_.s)
    .map(_._2.max(Ordering.by[PValue, Ballot](_.b)))
    .map(v => (v.s, Proposal(v.s, v.p)))
    .toMap

  /**
   * Elements of y as well as elements of x that are not in y
   */
  def plus(x: Map[Long, Proposal], y: Map[Long, Proposal]) = y ++ x.filterNot {
    case (key, _) => y.keySet.contains(key)
  }


}


sealed trait LeaderState

case object Active extends LeaderState

case object Waiting extends LeaderState

case class LeaderData(ballot: Ballot, proposals: Map[Long, Proposal]) {

  def +(p: Proposal) = copy(proposals = proposals + (p.s -> p))

  def contains(slot: Long): Boolean = proposals.contains(slot)

  override def toString = s"Ballot: $ballot, proposal.length:${proposals.size}"
}



