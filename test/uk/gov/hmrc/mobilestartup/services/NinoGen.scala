package uk.gov.hmrc.mobilestartup.services
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.domain.Nino

trait NinoGen {
  val invalidPrefixes = List("BG", "GB", "NK", "KN", "TN", "NT", "ZZ")
  val validFirstCharacters:  List[Char] = ('A' to 'Z').filterNot(List('D', 'F', 'I', 'Q', 'U', 'V').contains).toList
  val validSecondCharacters: List[Char] = ('A' to 'Z').filterNot(List('D', 'F', 'I', 'O', 'Q', 'U', 'V').contains).toList

  val validPrefixes: List[String] = {
    for {
      c1 <- validFirstCharacters
      c2 <- validSecondCharacters
    } yield s"$c1$c2"
  }.filterNot(invalidPrefixes.contains)

  val genNumPart: Gen[String] = Gen.listOfN(6, Gen.oneOf('0' to '9')).map(_.mkString)

  val ninoGen: Gen[Nino] = {
    for {
      prefix  <- Gen.oneOf(validPrefixes)
      numPart <- genNumPart
      suffix  <- Gen.oneOf('A' to 'D')
    } yield s"$prefix$numPart$suffix"
  }.map(Nino(_))

  implicit val arbNino: Arbitrary[Nino] = Arbitrary(ninoGen)
}
