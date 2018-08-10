/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.domain

import org.joda.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Json, Reads}

case class TaxCodeHistory(previous: TaxCodeRecord, current: TaxCodeRecord){
//  def mostRecentTaxCodeChangeDate: LocalDate = {
//    implicit val dateTimeOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)
//    taxCodeRecords.map(_.startDate).max
//  }
}

object TaxCodeHistory {

//  implicit val format = Json.format[TaxCodeHistory]

    implicit val reads: Reads[TaxCodeHistory] = (
      (JsPath \ "taxCodeHistory" \ "previous").read[TaxCodeRecord] and
      (JsPath \ "taxCodeHistory" \ "current").read[TaxCodeRecord]
    )(TaxCodeHistory.apply _)
}


