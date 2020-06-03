package com.visualalpha.service

import scala.io.StdIn.readLine
import scala.io.Source
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.Using
import java.io.{BufferedReader, FileReader}
import akka.actor.{ PoisonPill, ActorRef, Actor }

object CLI {

  import Connection.system

  def startCli(connectionActor: ActorRef, inputQueue: ActorRef): Unit = {
    val cmd = readLine("> ")
    cmd match {
      case "help" => printHelp
      case s if List("q", "quit").contains(s) => Main.quit
      case s if s.split("\\s+")(0) == "send" => send( s.split("\\s+").toList.tail, inputQueue)
    }
    startCli(connectionActor, inputQueue)
  }

  def send(cmdWords: List[String], inputQueue: ActorRef) = {
    def helper(fileNames: List[String], sendFunc: (String, ActorRef) => Unit): Unit = 
      fileNames match {
        case f :: fs => {
          sendFunc(f, inputQueue)
          helper(fs, sendFunc)
        }
        case Nil => ()
      }
    cmdWords match {    
      case "-resources" :: filenames => helper(filenames, resourcesFileSend)
      case _ => println("Unable to recognize command")
    }
  }

  def resourcesFileSend(name: String, inputQueue: ActorRef): Unit = {
    val fileText = Source.fromResource(name).mkString
    Connection.sendToInputChannel(inputQueue, fileText)
  }

  def printHelp = println("""
    Help is here!
    """)
}
