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

case class PValue(b: String, s: Long, p: Command)

case class Phase1a(l: ActorRef, b: String)

case class Phase1b(l: ActorRef, b: String, accepted: Set[PValue])

case class Phase2a(l: ActorRef, pValue: PValue)

case class Phase2b(l: ActorRef, b: String)

case class Preempted(b: String)

case class Adopted(b: String, pValues: Set[PValue])


case class Ballot(leader: Int, ballot: Long) extends Ordered[Ballot] {

  def compare(that: Ballot) = (this.leader - that.leader) match {
    case x if x == 0 => (this.ballot - that.ballot).toInt
    case x => x
  }

}

object GetState