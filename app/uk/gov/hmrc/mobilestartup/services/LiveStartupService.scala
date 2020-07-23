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

package uk.gov.hmrc.mobilestartup.services
import cats.implicits._
import com.google.inject.name.Named
import javax.inject.Inject
import uk.gov.hmrc.mobilestartup.connectors.GenericConnector

import scala.concurrent.{ExecutionContext, Future}

class LiveStartupService @Inject() (
  connector:                                                                                         GenericConnector[Future],
  @Named("feature.userPanelSignUp") userPanelSignUp:                                                 Boolean,
  @Named("feature.helpToSave.enableBadge") helpToSaveEnableBadge:                                    Boolean,
  @Named("feature.enablePushNotificationTokenRegistration") enablePushNotificationTokenRegistration: Boolean,
  @Named("feature.paperlessAlertDialogues") enablePaperlessAlertDialogues:                           Boolean,
  @Named("feature.paperlessAdverts") enablePaperlessAdverts:                                         Boolean
)(implicit ec:                                                                                       ExecutionContext)
    extends StartupServiceImpl[Future](connector,
                                       userPanelSignUp,
                                       helpToSaveEnableBadge,
                                       enablePushNotificationTokenRegistration,
                                       enablePaperlessAlertDialogues,
                                       enablePaperlessAdverts)
