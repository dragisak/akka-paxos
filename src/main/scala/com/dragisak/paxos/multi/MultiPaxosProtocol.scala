package com.dragisak.paxos.multi

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

case class Phase1b(l: ActorRef, b: Ballot, accepted: Set[PValue])

case class Phase2a(l: ActorRef, pValue: PValue)

case class Phase2b(l: ActorRef, b: Ballot)

case class Preempted(b: Ballot)

case class Adopted(b: Ballot, pValues: Set[PValue])


case class Ballot(leader: Int, ballot: Long) extends Ordered[Ballot] {

  def compare(that: Ballot) = (this.leader - that.leader) match {
    case x if x == 0 => (this.ballot - that.ballot).toInt
    case x => x
  }

  def increment = copy(ballot = ballot + 1)

}

object GetState