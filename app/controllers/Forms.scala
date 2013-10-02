package controllers

import play.api.data.Forms._

/**
 * Form helpers.
 */
object Forms {

  private val ValidURLSchemes = Set("http", "https")

  // Regular expression that matches a domain name, e.g. facebook.com or nl.linkedin.com
  // which starts with a group of letters and numbers, followed by one or more dot-separated groups that may contain hyphens,
  // followed by a top-level domain name (2-6 letters).
  private val DomainNameRegex = """^[a-z0-9]+([\-\.][a-z0-9]+)*\.[a-z]{2,6}$""".r

  /**
   * Validates that the given URL is a valid web site URL, which means that the scheme must be HTTP or HTTPS, and the
   * host name must include a domain name.
   */
  private def validateWebUrl(url: String): Boolean = {
    try {
      val uri = new java.net.URI(url)
      val validScheme = ValidURLSchemes.contains(Option(uri.getScheme).getOrElse("").toLowerCase)
      val host = Option(uri.getHost).getOrElse("")
      val validDomain = DomainNameRegex.findFirstIn(host.toLowerCase).isDefined
      validScheme && validDomain
    } catch {
      case _: Throwable ⇒ false
    }
  }

  /**
   * Web site URL form mapping.
   */
  val webUrl = text verifying ("error.url.web", validateWebUrl(_))

}