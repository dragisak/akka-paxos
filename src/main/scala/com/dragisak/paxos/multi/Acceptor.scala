package com.dragisak.paxos.multi

import akka.actor._

class Acceptor extends Actor with ActorLogging {

  var ballotNumber = Ballot(-1, -1, self)
  var accepted = Set[PValue]()

  def receive = {

    case Phase1a(l, b) =>
      if (b > ballotNumber) {
        ballotNumber = b
      }
      l ! Phase1b(self, ballotNumber, accepted)

    case Phase2a(l, pValue@PValue(b, s, p)) =>
      if (b >= ballotNumber) {
        ballotNumber = b
        accepted = accepted + pValue
      }
      l ! Phase2b(self, ballotNumber)
  }
}
