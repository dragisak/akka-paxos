package akkapaxos

import akka.actor.ActorRef

case class Operation(id: String) {
  def execute = Result(id)
}

case class Result(id: String)

case class Command(
  k: String,
  cid: Long,
  op: Operation
)

case class Request(p: Command)

case class Proposal(s: Long, p: Command)

case class Decision(s: Long, p: Command)

case class Response(cid: Long, result: Result)

case class PValue(b: Ballot, s: Long, p: Command)

case class Phase1a(l: ActorRef, b: Ballot)

case class Phase1b(l: ActorRef, b: Ballot, accepted: Option[PValue])

case class Phase2a(l: ActorRef, pValue: PValue)

case class Phase2b(l: ActorRef, b: Ballot)

case class Preempted(b: Ballot)

case class Adopted(b: Ballot, pValues: Set[PValue]) {
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