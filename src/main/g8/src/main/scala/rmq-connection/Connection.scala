

import com.newmotion.akka.rabbitmq._

object Conn {

  case class ConnParam(
    param: String,
    isDefault: Boolean,
    ) {
      def userOutput: String = this match {
        case ConnParam(p, false) => this.param
        case ConnParam(p, true) => s"${this.param} (default value)"
      }
    }
  object ConnParam {
    def apply(envVarName: String, defaultVal: String): ConnParam = 
      sys.env.get(envVarName) match {
        case Some(v) => ConnParam(v, false)
        case None => ConnParam(defaultVal, true)
      }
    }

  val RMQ_HOST = ConnParam("RMQ_HOST", "rabbitmq-server")
  val RMQ_USERNAME = ConnParam("RMQ_USERNAME", "guest")
  val RMQ_PASSWORD = ConnParam("RMQ_PASSWORD", "guest")
  val RMQ_EXCHANGE = ConnParam("RMQ_EXCHANGE", false)
  val INPUT_QUEUE = ConnParam("INPUT_QUEUE", false)
  val OUTPUT_ROUTING_KEY = ConnParam("OUTPUT_QUEUE", false)

  def printConnectionParams(): Unit = {
    println("Starting with RabbitMq parameters:")
    println(s"Host is ${RMQ_HOST.userOutput}")
    println(s"Username is ${RMQ_USERNAME.userOutput}")
    println(s"Password is ${RMQ_PASSWORD.userOutput}")
    println(s"Input queue is ${INPUT_QUEUE.userOutput}")
    println(s"Output routing key is ${OUTPUT_ROUTING_KEY.userOutput}")
  }
}

