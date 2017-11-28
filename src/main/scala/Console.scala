import java.io.{File, IOException}
import java.util

import scala.tools.jline.console.ConsoleReader
import scala.tools.jline.console.completer.Completer
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.tools.jline.TerminalFactory
import scala.tools.jline.console.history.FileHistory

object App {
  def main(args: Array[String]): Unit = {
    new Console().repl()
  }
}

class Console {
  import monix.execution.Scheduler.Implicits.global
  private val history = new FileHistory(new File(".history").getAbsoluteFile)
  private val reader  = new ConsoleReader()

  reader.setHistory(history)
  reader.setPrompt("g> ")

  def complete(s: String, pos: Int) =
    Await.result(GoogleGrammar
                   .completeAsync(GoogleGrammar.search, s)
                   .map(c => {
                     (c.completionStrings, c.position.column)
                   })
                   .runAsync,
                 Duration.Inf)
  reader.addCompleter(new SearchCompleter(complete))

  def repl(): Unit = {
    try {
      for (line <- Iterator
             .continually(reader.readLine())
             .takeWhile(_ != null)) { // scalastyle:ignore null
        interpret(line)
      }
    } catch {
      case io: IOException => io.printStackTrace()
    } finally {
      try {
        TerminalFactory.get().restore()
      } catch {
        case ex: Exception => ex.printStackTrace()
      }
      reader.getHistory.asInstanceOf[FileHistory].flush()
    }
  }

  private def interpret(st: String): Unit = {
    if (st.nonEmpty) {
      println(GoogleGrammar.parse(GoogleGrammar.search, st + " ")) // finish with whitespace since terms need to be delimited with whitespace
    }
  }
}

class SearchCompleter(completeFct: (String, Int) => (Seq[String], Int)) extends Completer {
  override def complete(buffer: String, cursor: Int, candidates: util.List[CharSequence]): Int = {
    val (completions, pos) = completeFct(buffer, cursor)
    val completionStart    = (pos - 1).max(0)
    val prefix             = buffer.substring(completionStart, buffer.length)
    val stringsMatchingEntry =
      completions.map(_ + " ").filter(_.startsWith(prefix.trim()))
    candidates.addAll(stringsMatchingEntry.asJava)
    completionStart
  }
}
