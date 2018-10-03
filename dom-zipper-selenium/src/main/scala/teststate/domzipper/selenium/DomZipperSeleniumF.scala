package teststate.domzipper.selenium

import org.openqa.selenium.{By, WebDriver, WebElement}
import scala.collection.JavaConverters._
import teststate.domzipper._
import teststate.selenium.util.SeleniumExt._
import DomZipper.{CssSelEngine, CssSelResult, DomCollection, Layer}
import ErrorHandler.{ErrorHandlerOptionOps, ErrorHandlerResultOps}

object DomZipperSeleniumF {

  type Dom = WebElement

  type DomCollection[F[_], C[_]] = DomZipper.DomCollection[DomZipperSeleniumF, F, C, Dom]

  type CssSelEngine = DomZipper.CssSelEngine[Dom, Dom]

  private implicit val cssSelSelenium: CssSelEngine =
    CssSelEngine((css, parent) => parent.findElements(By.cssSelector(css)).asScala.toVector)

  final class Constructors[F[_]](implicit F: ErrorHandler[F]) {

    def apply(name: String, webElement: WebElement)(implicit scrub: HtmlScrub, driver: WebDriver): DomZipperSeleniumF[F] =
      new DomZipperSeleniumF(Vector.empty, Layer(name, "", webElement))

    def apply(webElement: WebElement)(implicit scrub: HtmlScrub, driver: WebDriver): DomZipperSeleniumF[F] =
      apply("<provided>", webElement)

    def apply(name: String, webElement: WebElement, driver: WebDriver)(implicit scrub: HtmlScrub): DomZipperSeleniumF[F] =
      apply(name, webElement)(scrub, driver)

    def apply(webElement: WebElement, driver: WebDriver)(implicit scrub: HtmlScrub): DomZipperSeleniumF[F] =
      apply(webElement)(scrub, driver)

    def tag(tag: String, driver: WebDriver)(implicit scrub: HtmlScrub): DomZipperSeleniumF[F] =
      apply(tag, driver.findElement(By.tagName(tag)))(scrub, driver)

    def html(driver: WebDriver)(implicit scrub: HtmlScrub): DomZipperSeleniumF[F] =
      tag("html", driver)

    def body(driver: WebDriver)(implicit scrub: HtmlScrub): DomZipperSeleniumF[F] =
      tag("body", driver)
  }
}

import DomZipperSeleniumF.Dom

final class DomZipperSeleniumF[F[_]](override protected val prevLayers: Vector[Layer[Dom]],
                              override protected val curLayer: Layer[Dom]
                             )(implicit
                               override protected val $: CssSelEngine[Dom, Dom],
                               override protected val htmlScrub: HtmlScrub,
                               override protected val F: ErrorHandler[F],
                               driver: WebDriver
                             ) extends DomZipperBase[F, Dom, DomZipperSeleniumF] {

  override protected def self = this

  override protected def copySelf[G[_]](h: HtmlScrub, g: ErrorHandler[G]) =
    new DomZipperSeleniumF(prevLayers, curLayer)($, h, g, driver)

  override protected[domzipper] def addLayer(nextLayer: Layer[Dom]) =
    new DomZipperSeleniumF(prevLayers :+ curLayer, nextLayer)

  override protected def _parent: F[Dom] =
    F.attempt(dom.parent()(driver))

  override protected def _outerHTML: String =
    getAttribute("outerHTML").getOrElse("null")

  override protected def _innerHTML: String =
    getAttribute("innerHTML").getOrElse("null")

  private def newDomCollection[C[_]](desc: String, result: CssSelResult[Dom], C: DomCollection.Container[F, C]): DomCollection[DomZipperSeleniumF, F, C, Dom] =
    new DomCollection[DomZipperSeleniumF, F, C, Dom](this, _.addLayer(_), desc, result, None, C)

  override protected def collect[C[_]](sel: String, C: DomCollection.Container[F, C]): DomCollection[DomZipperSeleniumF, F, C, Dom] =
    newDomCollection(sel, runCssQuery(sel), C)

  override protected def collectChildren[C[_]](desc: String, C: DomCollection.Container[F, C]): DomCollection[DomZipperSeleniumF, F, C, Dom] =
    newDomCollection(desc, dom.children(), C)

  override protected def collectChildren[C[_]](desc: String, sel: String, C: DomCollection.Container[F, C]): DomCollection[DomZipperSeleniumF, F, C, Dom] = {
    // WebElement implements hashCode and equals sensibly
    val all: Set[WebElement] = runCssQuery(sel).toSet
    val children = dom.children().filter(all.contains)
    newDomCollection(desc, children, C)
  }

  override def matches(css: String): F[Boolean] = {
    val all = driver.findElements(By.cssSelector(css)).asScala
    val dom = this.dom
    F pass all.contains(dom) // WebElement implements hashCode and equals sensibly
  }

  override def getAttribute(name: String): Option[String] =
    Option(dom.getAttribute(name))

  override def tagName: String =
    dom.getTagName()

  override def innerText: String =
    dom.getText()

  override def checked: F[Boolean] =
    F pass dom.isSelected()

  override def classes: Set[String] =
    dom.classes()

  override def value: F[String] =
    getAttribute("value") orFail s".value failed on <${dom.getTagName}>."

  /** The currently selected option in a &lt;select&gt; dropdown. */
  def selectedOption: F[DomCollection[DomZipperSeleniumF, F, Option, Dom]] =
    dom.getTagName.toUpperCase match {
      case "SELECT" => F pass collect01("option[selected]")
      case x        => F fail s"<$x> is not a <SELECT>"
    }

  /** The text value of the currently selected option in a &lt;select&gt; dropdown. */
  def selectedOptionText: F[Option[String]] =
    selectedOption.flatMap(_.mapDoms(_.getText))
}