package com.visualalpha.service

import com.newmotion.akka.rabbitmq._
import akka.actor.{ Props, ActorRef, ActorSystem, PoisonPill }
import concurrent.duration._

object Connection {

  type ReceivedMessageBody = String
  type ResponseString = String 

  val system = ActorSystem("ConnectionSystem") 

  case class ConnParam(
    get: String,
    isDefault: Boolean,
    ) {
      def userOutput: String = this match {
        case ConnParam(p, false) => this.get
        case ConnParam(p, true) => s"${this.get} (default value)"
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
  val INPUT_ROUTING_KEY = ConnParam("INPUT_ROUTING_KEY", "input.key")
  val OUTPUT_ROUTING_KEY = ConnParam("OUTPUT_QUEUE", false)

  def printConnectionParams(): Unit = {
    println("Starting with RabbitMq Parameters:")
    println(s"Host is ${RMQ_HOST.userOutput}")
    println(s"Username is ${RMQ_USERNAME.userOutput}")
    println(s"Password is ${RMQ_PASSWORD.userOutput}")
    println(s"Input queue is ${INPUT_QUEUE.userOutput}")
    println(s"Output routing key is ${OUTPUT_ROUTING_KEY.userOutput}")
  }

  val defaultConnectionActor: ActorRef = 
    getConnectionActor(RMQ_HOST.get, RMQ_USERNAME.get, RMQ_PASSWORD.get)

  def getConnectionActor(
    host: String,
    username: String,
    password: String,
  ): ActorRef = 
    system.actorOf(
      ConnectionActor.props(new ConnectionFactory(), reconnectionDelay = 10.seconds)
    )

  def setupDefaultInputChannel(channel: Channel, self: ActorRef) = {
    channel.queueDeclare(INPUT_QUEUE.get, false, false, false, null)
    channel.exchangeDeclare(RMQ_EXCHANGE.get, "topic")
    channel.queueBind(INPUT_QUEUE.get, RMQ_EXCHANGE.get, INPUT_ROUTING_KEY.get)
  }

  def setupDefaultOutputChannel(channel: Channel, self: ActorRef) = {
    channel.exchangeDeclare(RMQ_EXCHANGE.get, "topic")
  }

  def startRespondingOnInputQueue(
    connectionActor: ActorRef,
    respondWith: ReceivedMessageBody => ResponseString,
    ): ActorRef = {
      val outputChannel = connectionActor.createChannel(
        ChannelActor.props(setupDefaultOutputChannel)
      )
      def respondFunc(receivedMsg: String)(c: Channel) = {
        val response = respondWith(receivedMsg)
        println("Responding on output channel with ")
        println(response.take(50))
        c.basicPublish(
          RMQ_EXCHANGE.get, OUTPUT_ROUTING_KEY.get, null, response.getBytes("UTF-8")
        )
      }
      def setupInputChannelWithConsumer(channel: Channel, self: ActorRef) = {
        setupDefaultInputChannel(channel, self)
        val consumer = new DefaultConsumer(channel) {
          override def handleDelivery(
            tag: String, env: Envelope, prop: BasicProperties, body: Array[Byte],
            ) = {
            val bodyString = new String(body, "UTF-8")
            println(s"Receiving on input channel:")
            println(s"${bodyString.take(50)}")
            val rF = respondFunc(bodyString)(_)
            outputChannel ! ChannelMessage(rF, dropIfNoChannel = false)
          }
        }
        channel.basicConsume(INPUT_QUEUE.get, true, consumer)
      }
      connectionActor.createChannel(
        ChannelActor.props(setupInputChannelWithConsumer)
      )
    }

  def sendToInputChannel(inputChannel: ActorRef, message: String): Unit = {
    def sendMessage(channel: Channel) = { 
      println(s"Sending to input channel")
      channel.basicPublish(
        RMQ_EXCHANGE.get, INPUT_ROUTING_KEY.get, null, message.getBytes("UTF-8")
      )
    }
    inputChannel ! ChannelMessage(sendMessage, dropIfNoChannel = false)
  }

  def startMonitoringOutputChannel(connectionActor: ActorRef): ActorRef = {
    def setupOutputQueueWithConsumer(channel: Channel, self: ActorRef) = {
      val monitorQueue = channel.queueDeclare().getQueue
      channel.queueBind(monitorQueue, RMQ_EXCHANGE.get, OUTPUT_ROUTING_KEY.get)
      val consumer = new DefaultConsumer(channel) {
        override def handleDelivery(
          tag: String, env: Envelope, prop: BasicProperties, body: Array[Byte],
          ) = {
          val bodyString = new String(body, "UTF-8")
          println(s"Found in output queue:")
          println(s"${bodyString.take(50)}")
        }
      }
      channel.basicConsume(monitorQueue, true, consumer)
    }
    connectionActor.createChannel(ChannelActor.props(setupOutputQueueWithConsumer))
  }

  def publishToDefaultOutput(connectionActor: ActorRef, toPublish: String) = {
    val outputChannel = connectionActor.createChannel(
      ChannelActor.props(setupDefaultOutputChannel)
    ) 
    def sendMessage(channel: Channel) = { 
      println(s"Sending to output channel")
      println(toPublish.take(50))
      channel.basicPublish(
        RMQ_EXCHANGE.get, OUTPUT_ROUTING_KEY.get, null, toPublish.getBytes("UTF-8")
      )
    }
    outputChannel ! ChannelMessage(sendMessage, dropIfNoChannel = false)
  }

  def shutdownSystem(connectionActor: ActorRef) = {
    system stop connectionActor
    system.terminate
  }
}
