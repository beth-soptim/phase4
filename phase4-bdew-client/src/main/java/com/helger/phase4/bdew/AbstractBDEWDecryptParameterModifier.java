/*
 * Copyright (C) 2023-2025 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
 *
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.bdew;

import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.dom.handler.RequestData;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.phase4.crypto.IAS4DecryptParameterModifier;

import jakarta.annotation.Nonnull;

/**
 * Abstract base implementation of {@link IAS4DecryptParameterModifier} show
 * casing the ignorance of a BSP rule.
 *
 * @author Philip Helger
 */
public abstract class AbstractBDEWDecryptParameterModifier implements IAS4DecryptParameterModifier
{
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  public void modifyRequestData (@Nonnull final RequestData aRequestData)
  {
    // Ignore this rule; see #170
    aRequestData.setIgnoredBSPRules (new CommonsArrayList <> (BSPRule.R3058));
  }
}
