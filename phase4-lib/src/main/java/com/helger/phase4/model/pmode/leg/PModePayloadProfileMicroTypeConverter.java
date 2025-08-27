/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.model.pmode.leg;

import com.helger.base.state.EMandatory;
import com.helger.mime.IMimeType;
import com.helger.mime.parse.MimeTypeParser;
import com.helger.mime.parse.MimeTypeParserException;
import com.helger.phase4.model.pmode.AbstractPModeMicroTypeConverter;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.IMicroQName;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * XML converter for objects of class {@link PModePayloadProfile}.
 *
 * @author Philip Helger
 */
public class PModePayloadProfileMicroTypeConverter extends AbstractPModeMicroTypeConverter <PModePayloadProfile>
{
  private static final IMicroQName ATTR_NAME = new MicroQName ("Name");
  private static final IMicroQName ATTR_MIME_TYPE = new MicroQName ("MimeType");
  private static final IMicroQName ATTR_XSD_FILENAME = new MicroQName ("XSDFilename");
  private static final IMicroQName ATTR_MAX_SIZE_KB = new MicroQName ("MaxSizeKB");
  private static final IMicroQName ATTR_MANDATORY = new MicroQName ("Mandatory");

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final PModePayloadProfile aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull final String sTagName)
  {
    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);

    ret.setAttribute (ATTR_NAME, aValue.getName ());
    ret.setAttribute (ATTR_MIME_TYPE, aValue.getMimeType ().getAsString ());
    ret.setAttribute (ATTR_XSD_FILENAME, aValue.getXSDFilename ());
    if (aValue.hasMaxSizeKB ())
      ret.setAttribute (ATTR_MAX_SIZE_KB, aValue.getMaxSizeKB ().intValue ());
    ret.setAttribute (ATTR_MANDATORY, aValue.isMandatory ());
    return ret;
  }

  @Nonnull
  public PModePayloadProfile convertToNative (final IMicroElement aElement)
  {
    final String sName = aElement.getAttributeValue (ATTR_NAME);
    final String sMimeType = aElement.getAttributeValue (ATTR_MIME_TYPE);
    final IMimeType aMimeType;
    try
    {
      aMimeType = MimeTypeParser.parseMimeType (sMimeType);
    }
    catch (final MimeTypeParserException ex)
    {
      throw new IllegalArgumentException ("Failed to parse MIME Type '" + sMimeType + "'", ex);
    }
    final String sXSDFilename = aElement.getAttributeValue (ATTR_XSD_FILENAME);
    final Integer aMaxSizeKB = aElement.getAttributeValueWithConversion (ATTR_MAX_SIZE_KB, Integer.class);
    final EMandatory eMandatory = EMandatory.valueOf (aElement.getAttributeValueAsBool (ATTR_MANDATORY,
                                                                                        PModePayloadProfile.DEFAULT_MANDATORY));

    return new PModePayloadProfile (sName, aMimeType, sXSDFilename, aMaxSizeKB, eMandatory);
  }
}
