/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsNumber, Json}

import java.time.LocalDate

class MgdCertificateSpec extends AnyWordSpec with Matchers {

  "BusinessType format" should {

    "write business type as numeric code" in {

      Json.toJson[BusinessType](BusinessType.Soleproprietor)              shouldBe JsNumber(1)
      Json.toJson[BusinessType](BusinessType.Corporatebody)               shouldBe JsNumber(2)
      Json.toJson[BusinessType](BusinessType.Unincorporatedbody)          shouldBe JsNumber(3)
      Json.toJson[BusinessType](BusinessType.Partnership)                 shouldBe JsNumber(4)
      Json.toJson[BusinessType](BusinessType.LimitedLiabilityPartnership) shouldBe JsNumber(5)
    }

    "read business type from numeric code" in {

      JsNumber(1).as[BusinessType] shouldBe BusinessType.Soleproprietor
      JsNumber(2).as[BusinessType] shouldBe BusinessType.Corporatebody
      JsNumber(3).as[BusinessType] shouldBe BusinessType.Unincorporatedbody
      JsNumber(4).as[BusinessType] shouldBe BusinessType.Partnership
      JsNumber(5).as[BusinessType] shouldBe BusinessType.LimitedLiabilityPartnership
    }

    "return JsError for invalid business type code" in {

      JsNumber(999).validate[BusinessType] shouldBe
        JsError("Invalid business type")
    }
  }

  "PartnerMember format" should {

    "serialize and deserialize correctly" in {

      val model = PartnerMember(
        namesOfPartMems = Some("John Partner"),
        solePropTitle = Some("Mr"),
        solePropFirstName = Some("John"),
        solePropMiddleName = Some("A"),
        solePropLastName = Some("Doe"),
        typeOfBusiness = BusinessType.Corporatebody
      )

      val json = Json.toJson(model)

      (json \ "typeOfBusiness").as[Int] shouldBe 2

      json.as[PartnerMember] shouldBe model
    }
  }

  "GroupMember format" should {

    "serialize and deserialize correctly" in {

      val model = GroupMember(
        namesOfGroupMems = "Group Member Ltd"
      )

      val json = Json.toJson(model)

      json.as[GroupMember] shouldBe model
    }
  }

  "ReturnPeriodEndDate format" should {

    "serialize LocalDate in ISO format" in {

      val model = ReturnPeriodEndDate(
        LocalDate.of(2026, 5, 19)
      )

      val json = Json.toJson(model)

      (json \ "returnPeriodEndDate").as[String] shouldBe "2026-05-19"

      json.as[ReturnPeriodEndDate] shouldBe model
    }
  }

  "MgdCertificate format" should {

    "serialize and deserialize correctly" in {

      val model = MgdCertificate(
        mgdRegNumber = "MGD123456",
        registrationDate = Some(LocalDate.of(2025, 1, 1)),
        individualName = Some("John Smith"),
        businessName = Some("Test Business"),
        tradingName = Some("Trading Name"),
        repMemName = Some("Representative Member"),
        busAddrLine1 = Some("Line 1"),
        busAddrLine2 = Some("Line 2"),
        busAddrLine3 = Some("Line 3"),
        busAddrLine4 = Some("Line 4"),
        busPostcode = Some("AA1 1AA"),
        busCountry = Some("UK"),
        busAdi = Some("ADI123"),
        repMemLine1 = Some("Rep Line 1"),
        repMemLine2 = Some("Rep Line 2"),
        repMemLine3 = Some("Rep Line 3"),
        repMemLine4 = Some("Rep Line 4"),
        repMemPostcode = Some("BB1 1BB"),
        repMemAdi = Some("REPADI"),
        typeOfBusiness = Some("Corporate Body"),
        businessTradeClass = Some(1),
        noOfPartners = Some(2),
        groupReg = "Y",
        noOfGroupMems = Some(3),
        dateCertIssued = Some(LocalDate.of(2026, 5, 19)),
        partMembers = Seq(
          PartnerMember(
            namesOfPartMems = Some("Partner 1"),
            solePropTitle = Some("Mrs"),
            solePropFirstName = Some("Jane"),
            solePropMiddleName = None,
            solePropLastName = Some("Doe"),
            typeOfBusiness = BusinessType.Soleproprietor
          )
        ),
        groupMembers = Seq(
          GroupMember("Group Member 1")
        ),
        returnPeriodEndDates = Seq(
          ReturnPeriodEndDate(LocalDate.of(2026, 12, 31))
        )
      )

      val json = Json.toJson(model)

      (json \ "registrationDate").as[String] shouldBe "2025-01-01"
      (json \ "dateCertIssued").as[String]   shouldBe "2026-05-19"

      val partMemberJson = (json \ "partMembers")(0)
      (partMemberJson \ "typeOfBusiness").as[Int] shouldBe 1

      json.as[MgdCertificate] shouldBe model
    }

    "serialize correctly when optional fields are empty" in {

      val model = MgdCertificate(
        mgdRegNumber = "MGD999999",
        registrationDate = None,
        individualName = None,
        businessName = None,
        tradingName = None,
        repMemName = None,
        busAddrLine1 = None,
        busAddrLine2 = None,
        busAddrLine3 = None,
        busAddrLine4 = None,
        busPostcode = None,
        busCountry = None,
        busAdi = None,
        repMemLine1 = None,
        repMemLine2 = None,
        repMemLine3 = None,
        repMemLine4 = None,
        repMemPostcode = None,
        repMemAdi = None,
        typeOfBusiness = None,
        businessTradeClass = None,
        noOfPartners = None,
        groupReg = "N",
        noOfGroupMems = None,
        dateCertIssued = None,
        partMembers = Seq.empty,
        groupMembers = Seq.empty,
        returnPeriodEndDates = Seq.empty
      )

      val json = Json.toJson(model)

      json.as[MgdCertificate] shouldBe model
    }
  }
}
