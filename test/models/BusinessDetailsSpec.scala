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

import models.agent.AgentDetails
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BusinessDetailsSpec extends AnyWordSpec with Matchers {

  "fromAgentDetails" should {
    "map the shared fields, renaming mobilePhoneNumber and email" in {
      val details = AgentDetails(
        businessName = Some("Agent 1"),
        addressLine1 = Some("1"),
        addressLine2 = Some("2"),
        addressLine3 = Some("3"),
        addressLine4 = Some("4"),
        postcode = Some("AB1 2CD"),
        country = Some("GB"),
        abroadSignal = Some("N"),
        phoneNumber = Some("0191 202 2500"),
        mobilePhoneNumber = Some("07890 123 456"),
        faxNumber = Some("0800 202 2500"),
        email = Some("a@example.com")
      )

      BusinessDetails.fromAgentDetails(details) shouldBe BusinessDetails(
        Some("Agent 1"),
        Some("1"),
        Some("2"),
        Some("3"),
        Some("0191 202 2500"),
        Some("07890 123 456"),
        Some("0800 202 2500"),
        Some("a@example.com")
      )
    }
  }
}
