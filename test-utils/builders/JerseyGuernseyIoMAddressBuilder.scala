/*
 * Copyright 2025 HM Revenue & Customs
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

package builders

import models.Country.Jersey
import models.JerseyGuernseyIoMAddress

object JerseyGuernseyIoMAddressBuilder {

  val aJerseyGuernseyIoMAddress: JerseyGuernseyIoMAddress = JerseyGuernseyIoMAddress(
    line1 = "default-line-1",
    line2 = None,
    town = "default-town",
    county = None,
    postCode = "default-postcode",
    country = Jersey
  )
}
