package akkapaxos

import akka.actor._

class Leader[E](
  val id: Int,
  val acceptors: Set[ActorRef],
  val replicas: Set[ActorRef]
) extends Actor with LoggingFSM[LeaderState, LeaderData[E]] {

  import Leader._

  override def preStart() {
    spawnScout(Ballot(BallotNumber(id, 0), self))
  }

  startWith(Waiting, LeaderData(Ballot(BallotNumber(id, 0), self), Map()))


  when(Waiting) {

    case Event(prop: Proposal[E], data) if !data.contains(prop.s) =>
      stay using (data + prop)


    case Event(adopted: Adopted[E], data) =>

      val props: Map[Long, Proposal[E]] = combine(data.proposals, adopted.pValues)

      props.values.foreach(p => spawnCommander(PValue(data.ballot, p.s, p.p)))

      goto(Active) using LeaderData(adopted.b, props)

    case Event(Preempted(b1), data) if b1 > data.ballot =>
      val ballotNum = data.ballot.increment
      spawnScout(ballotNum)
      stay using LeaderData(ballotNum, data.proposals)

    case Event(Decision(slot,_), data) =>
      stay using data.copy(proposals = data.proposals - slot)

  }

  when(Active) {

    case Event(prop: Proposal[E], data) if !data.contains(prop.s) =>
      spawnCommander(PValue(data.ballot, prop.s, prop.p))
      stay using (data + prop)


    case Event(Preempted(b1), data) if b1 > data.ballot =>
      val ballotNum = data.ballot.increment
      spawnScout(ballotNum)
      goto(Waiting) using LeaderData(ballotNum, data.proposals)

    case Event(Decision(slot,_), data) =>
      stay using data.copy(proposals = data.proposals - slot)

  }

  whenUnhandled {
    case Event(m, _) =>
      log.debug("Ignoring {}", m)
      stay()
  }


  private def spawnCommander(pVal: PValue[E]) {
    context.actorOf(Props(classOf[Commander[E]], acceptors, replicas, pVal), name = s"commander-$id-${pVal.b.ballotNumber}-${pVal.s}")
  }

  private def spawnScout(b: Ballot) {
    context.actorOf(Props(classOf[Scout[E]], acceptors, b), name = s"scout-$id-${b.ballotNumber}")
  }


}

object Leader {

  def combine[E](proposals: Map[Long, Proposal[E]], pVals: Set[PValue[E]]) = plus(proposals, pmax(pVals))

  def pmax[E](pVals: Set[PValue[E]]): Map[Long, Proposal[E]] = pVals
    .groupBy(_.s)
    .map(_._2.max(Ordering.by[PValue[E], Ballot](_.b)))
    .map(v => (v.s, Proposal[E](v.s, v.p)))
    .toMap

  /**
   * Elements of y as well as elements of x that are not in y
   */
  def plus[E](x: Map[Long, Proposal[E]], y: Map[Long, Proposal[E]]) = y ++ x.filterNot {
    case (key, _) => y.keySet.contains(key)
  }


}


sealed trait LeaderState

case object Active extends LeaderState

case object Waiting extends LeaderState

case class LeaderData[E](ballot: Ballot, proposals: Map[Long, Proposal[E]]) {

  def +(p: Proposal[E]) = copy(proposals = proposals + (p.s -> p))

  def contains(slot: Long): Boolean = proposals.contains(slot)

  override lazy val toString = s"Ballot: $ballot, proposal.length:${proposals.size}"
}



