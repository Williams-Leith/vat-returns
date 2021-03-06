/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import config.MicroserviceAppConfig
import connectors.httpParsers.SubmitVatReturnHttpParser._
import javax.inject.Inject
import models.{SuccessModel, VatReturnSubmission}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class SubmitVatReturnConnector @Inject()(val http: HttpClient, val appConfig: MicroserviceAppConfig) {

  private lazy val desVatReturnsUrl: String => String = vrn => appConfig.desServiceUrl + appConfig.desSubmitVatReturnPath + vrn

  def submitVatReturn(vrn: String, model: VatReturnSubmission, originatorID: String)
                     (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpGetResult[SuccessModel]] = {

    implicit val hc: HeaderCarrier = headerCarrier
      .withExtraHeaders(
        "Content-Type" -> "application/json",
        "Environment" -> appConfig.desEnvironment,
        "OriginatorID" -> originatorID
      )
      .copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))

    Logger.debug(s"[SubmitVatReturnConnector][submitVatReturn] Submitting VAT Return to URL: ${desVatReturnsUrl(vrn)}. Body: ${Json.toJson(model)}")
    Logger.debug(s"[SubmitVatReturnConnector][submitVatReturn] Headers: ${hc.headers}")
    http.POST[VatReturnSubmission, HttpGetResult[SuccessModel]](desVatReturnsUrl(vrn), model)(
      implicitly[Writes[VatReturnSubmission]],
      implicitly[HttpReads[HttpGetResult[SuccessModel]]],
      hc,
      implicitly[ExecutionContext]
    )
  }
}
