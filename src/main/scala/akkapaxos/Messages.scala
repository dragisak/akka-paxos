package akkapaxos

import akka.actor.ActorRef

case class Result(id: String)

case class Command[O](
  k: String,
  cid: Long,
  op: O
)

case class Request[E](p: Command[E])

case class Proposal[E](s: Long, p: Command[E])

case class Decision[E](s: Long, p: Command[E])

case class Response(cid: Long, result: Result)

case class PValue[E](b: Ballot, s: Long, p: Command[E])

case class Phase1a(l: ActorRef, b: Ballot)

case class Phase1b[E](l: ActorRef, b: Ballot, accepted: Option[PValue[E]])

case class Phase2a[E](l: ActorRef, pValue: PValue[E])

case class Phase2b(l: ActorRef, b: Ballot)

case class Preempted(b: Ballot)

case class Adopted[E](b: Ballot, pValues: Set[PValue[E]]) {
  override def toString = s"b:$b, pValues.size:${pValues.size}"
}

case class BallotNumber(leader: Int, number: Long)  extends Ordered[BallotNumber] {

  override def compare(that: BallotNumber) = (this.number - that.number) match {
    case 0l   ⇒ this.leader - that.leader
    case x    ⇒ x.toInt
  }

  def increment = copy(number = number + 1)

  override def toString = s"$number-$leader"
}

case class Ballot(ballotNumber: BallotNumber, l: ActorRef) extends Ordered[Ballot] {

  override def compare(that: Ballot) = this.ballotNumber.compare(that.ballotNumber)

  def increment = copy(ballotNumber = ballotNumber.increment)

}

case object GetState

case class Ping(b: Ballot)

case class Pong(b: Ballot)

case class ProtocolTimedOut(b: Ballot)