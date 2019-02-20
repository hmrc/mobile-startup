package uk.gov.hmrc.mobilestartup
import cats.Semigroup
import play.api.libs.json.JsObject

package object services {
  implicit val jsonObjectSemigroup: Semigroup[JsObject] = new Semigroup[JsObject] {
    override def combine(x: JsObject, y: JsObject): JsObject = x ++ y
  }
}
