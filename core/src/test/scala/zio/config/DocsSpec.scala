package zio.config

import java.util.Properties

import ConfigDescriptor._
import zio.config.DocsSpecUtils._
import zio.test.Assertion._
import zio.test._

object DocsSpec
    extends BaseSpec(
      suite("partial products fail instead of returning none")(
        test(
          "Generate docs for simple config"
        ) {
          val caseClass2 =
            (string("region") |@| string("account_name"))(TestCase1.CaseClass2.apply, TestCase1.CaseClass2.unapply)

          val source1 =
            ConfigSource.fromProperties(new Properties(), "docker env")

          val source2 =
            ConfigSource.fromProperties(new Properties(), "system properties")

          val caseClass3 =
            (string("token_id").from(source1.orElse(source2)) |@| string(
              "username"
            ))(TestCase1.CaseClass3.apply, TestCase1.CaseClass3.unapply)

          val caseClass4 =
            nested("azure") {
              (string("a") |@| string("b"))(TestCase1.CaseClass4.apply, TestCase1.CaseClass4.unapply)
            }

          val either1 =
            (nested("aws")(caseClass2))
              .orElseEither(nested("credentials")(caseClass3))

          val config: ConfigDescriptor[TestCase1.CaseClass1] =
            (string("user") |@| either1 |@| caseClass4)(
              TestCase1.CaseClass1.apply,
              TestCase1.CaseClass1.unapply
            )

          val finalSource =
            ConfigSource.fromMap(Map.empty, source = "system environment")

          val result =
            generateDocs(config from finalSource).toTable.asMarkdownContent

          assert(result)(
            equalTo(DocsSpecUtils.expected)
          )
        }
      )
    )

object DocsSpecUtils {
  object TestCase1 {
    case class CaseClass1(
      a: String,
      b: Either[CaseClass2, CaseClass3],
      c: CaseClass4
    )
    case class CaseClass2(a: String, b: String)
    case class CaseClass3(c: String, d: String)
    case class CaseClass4(c: String, d: String)
  }

  val expected =
    s"""
       |## Configuration Details
       |
       |
       ||FieldName           |Format               |Description         |Sources           |
       ||---                 |---                  |---                 |---               |
       ||[azure](#root.azure)|[all-of](#root.azure)|                    |                  |
       ||                    |[any-one-of](#root)  |                    |                  |
       ||user                |primitive            |value of type string|system environment|
       |
       |### root.azure
       |
       ||FieldName|Format   |Description         |Sources           |
       ||---      |---      |---                 |---               |
       ||b        |primitive|value of type string|system environment|
       ||a        |primitive|value of type string|system environment|
       |
       |### root
       |
       ||FieldName                       |Format                     |Description|Sources|
       ||---                             |---                        |---        |---    |
       ||[credentials](#root.credentials)|[all-of](#root.credentials)|           |       |
       ||[aws](#root.aws)                |[all-of](#root.aws)        |           |       |
       |
       |### root.credentials
       |
       ||FieldName|Format   |Description         |Sources                                          |
       ||---      |---      |---                 |---                                              |
       ||username |primitive|value of type string|system environment                               |
       ||token_id |primitive|value of type string|docker env, system properties, system environment|
       |
       |### root.aws
       |
       ||FieldName   |Format   |Description         |Sources           |
       ||---         |---      |---                 |---               |
       ||account_name|primitive|value of type string|system environment|
       ||region      |primitive|value of type string|system environment|
       |""".stripMargin

}