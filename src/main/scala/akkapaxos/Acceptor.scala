package akkapaxos

import akka.actor._
import akka.event.LoggingReceive

class Acceptor extends Actor with ActorLogging {

  var ballotNumber = Ballot(-1, -1, self)
  var accepted = Map[Long, PValue]()

  def receive = LoggingReceive {


    case Phase1a(l, b) =>
      if (b > ballotNumber) {
        ballotNumber = b
      }
      l ! Phase1b(self, ballotNumber, accepted.get(ballotNumber.ballot))

    case Phase2a(l, pValue) =>
      if (pValue.b >= ballotNumber) {
        ballotNumber = pValue.b
        accepted = accepted + (ballotNumber.ballot -> pValue)
      }
      l ! Phase2b(self, ballotNumber)
  }
}
