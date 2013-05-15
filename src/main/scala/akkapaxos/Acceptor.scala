package akkapaxos

import akka.actor._

class Acceptor[E] extends Actor with LoggingFSM[AcceptorState, AcceptorData[E]] {

  startWith(AcceptorRunning, AcceptorData(Ballot(BallotNumber(-1, -1), self), Map()))

  when(AcceptorRunning) {

    case Event(Phase1a(l, b), data) =>
      val newData :AcceptorData[E] = if (b > data.ballot) {
        data.copy(ballot = b)
      } else {
        data
      }
      l ! Phase1b(self, newData.ballot, newData.accepted.get(newData.ballot.ballotNumber))
      stay using newData

    case Event(phase2a: Phase2a[E], data) =>
      val newData = if (phase2a.pValue.b >= data.ballot) {
        AcceptorData(phase2a.pValue.b, data.accepted + (phase2a.pValue.b.ballotNumber -> phase2a.pValue))
      } else {
        data
      }
      phase2a.l ! Phase2b(self, newData.ballot)
      stay using newData
  }

  whenUnhandled {
    case msg =>
      log.warning("Unhandled message {}", msg)
      stay()
  }
}

case class AcceptorData[E](ballot: Ballot, accepted: Map[BallotNumber, PValue[E]]) {
  override lazy val toString = s"ballot:$ballot, accepted.size:${accepted.size}"
}

sealed trait AcceptorState

case object AcceptorRunning extends AcceptorState
