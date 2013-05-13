package akkapaxos

import akka.actor._

class Acceptor extends Actor with LoggingFSM[AcceptorState, AcceptorData] {

  startWith(AcceptorRunning, AcceptorData(Ballot(BallotNumber(-1, -1), self), Map()))

  when(AcceptorRunning) {

    case Event(Phase1a(l, b), data) =>
      val newData = if (b > data.ballot) {
        data.copy(ballot = b)
      } else {
        data
      }
      l ! Phase1b(self, newData.ballot, newData.accepted.get(newData.ballot.ballotNumber))
      stay using newData

    case Event(Phase2a(l, pValue), data) =>
      val newData = if (pValue.b >= data.ballot) {
        AcceptorData(pValue.b, data.accepted + (pValue.b.ballotNumber -> pValue))
      } else {
        data
      }
      l ! Phase2b(self, newData.ballot)
      stay using newData
  }

  whenUnhandled {
    case msg =>
      log.warning("Unhandled message {}", msg)
      stay()
  }
}

case class AcceptorData(ballot: Ballot, accepted: Map[BallotNumber, PValue])

sealed trait AcceptorState

case object AcceptorRunning extends AcceptorState
