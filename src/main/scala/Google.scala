
import monix.eval.Task
import org.http4s.blaze.http.client.HttpClient
import org.json4s._
import org.json4s.native.JsonMethods._
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global


object Google {
  val logger = Logger("google")
  var cache  = Map[String, List[String]]()

  def querySuggestions(in: String): Task[List[String]] =
    cache.get(in).map(Task.eval(_)).getOrElse(queryAndParse(in))

  private def queryAndParse(in: String) = {
    val jsonSuggestions: Task[JValue] = Task.fromFuture(query(in))
    jsonSuggestions
      .map((json: JValue) => {
        logger.debug(s"Got JSON: ${json.toString()}")
        val results = for {
          JArray(results) <- json
          JArray(terms)   <- results
          JString(s)      <- terms
          word            <- s.split("\\s+")
        } yield word
        logger.debug(s"Got results: ${results.toString()}")
        cache += in -> results
        results
      })
      .map(_.filter(_.nonEmpty))
  }

  private def query(query: String) =
    HttpClient.GET(s"http://suggestqueries.google.com/complete/search?client=firefox&q=$query")(r => r.stringBody().map(parse(_)))
}

