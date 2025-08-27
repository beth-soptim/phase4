/*
 * Copyright (C) 2024-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.dbnalliance;

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.rt.StackTraceHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.IJsonWriterSettings;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.marshaller.Ebms3SignalMessageMarshaller;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.IXMLWriterSettings;
import com.helger.xml.serialize.write.XMLWriterSettings;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class contains the structured information about what happens on DBNAlliance sending.
 *
 * @author Philip Helger
 * @since 3.1.0
 */
@NotThreadSafe
public class Phase4DBNAllianceSendingReport
{
  // State params
  private final OffsetDateTime m_aCurrentDateTimeUTC;
  private final String m_sSMLDNSZone;

  // Special issues only available if an XHE is provided
  private Exception m_aXHEParseException;

  // Input params
  private IParticipantIdentifier m_aSenderID;
  private IParticipantIdentifier m_aReceiverID;
  private IDocumentTypeIdentifier m_aDocTypeID;
  private IProcessIdentifier m_aProcessID;
  private String m_sSenderPartyID;

  // XHE details
  private String m_sXHEHeaderID;

  // SMP lookup results
  private String m_sC3EndpointURL;
  private X509Certificate m_aC3Cert;
  private String m_sC3CertSubjectCN;
  private OffsetDateTime m_aC3CertCheckDT;
  private ECertificateCheckResult m_eC3CertCheckResult;

  // AS4 params
  private String m_sAS4MessageID;
  private String m_sAS4ConversationID;

  // AS4 response details
  private EAS4UserMessageSendResult m_eAS4SendingResult;
  private Exception m_aAS4SendingException;
  private Ebms3SignalMessage m_aAS4ReceivedSignalMsg;
  private boolean m_bAS4ResponseError = false;
  private ICommonsList <Ebms3Error> m_aAS4ResponseErrors;

  private long m_nOverallDurationMillis = -1;
  private boolean m_bSendingSuccess = false;
  private boolean m_bOverallSuccess = false;

  public Phase4DBNAllianceSendingReport (@Nonnull final String sDNSZone)
  {
    m_aCurrentDateTimeUTC = PDTFactory.getCurrentOffsetDateTimeUTC ();
    m_sSMLDNSZone = sDNSZone;
  }

  public boolean hasXHEParseException ()
  {
    return m_aXHEParseException != null;
  }

  /**
   * Remember any specific exception that occurred during parsing of a provided XHE.
   *
   * @param e
   *        The exception that was caught. May be <code>null</code>.
   */
  public void setXHEParseException (@Nullable final Exception e)
  {
    m_aXHEParseException = e;
  }

  public boolean hasSenderID ()
  {
    return m_aSenderID != null;
  }

  /**
   * Remember the senders DBNAlliance Participant ID (C1 ID).
   *
   * @param a
   *        DBNAlliance Participant ID. May be <code>null</code>.
   */
  public void setSenderID (@Nullable final IParticipantIdentifier a)
  {
    m_aSenderID = a;
  }

  public boolean hasReceiverID ()
  {
    return m_aReceiverID != null;
  }

  /**
   * Remember the receivers DBNAlliance Participant ID (C4 ID).
   *
   * @param a
   *        DBNAlliance Participant ID. May be <code>null</code>.
   */
  public void setReceiverID (@Nullable final IParticipantIdentifier a)
  {
    m_aReceiverID = a;
  }

  public boolean hasDocTypeID ()
  {
    return m_aDocTypeID != null;
  }

  /**
   * Remember the DBNAlliance Document Type ID that was exchanged.
   *
   * @param a
   *        Document Type ID. May be <code>null</code>.
   */
  public void setDocTypeID (@Nullable final IDocumentTypeIdentifier a)
  {
    m_aDocTypeID = a;
  }

  public boolean hasProcessID ()
  {
    return m_aProcessID != null;
  }

  /**
   * Remember the DBNAlliance Process ID that was exchanged.
   *
   * @param a
   *        Process ID. May be <code>null</code>.
   */
  public void setProcessID (@Nullable final IProcessIdentifier a)
  {
    m_aProcessID = a;
  }

  public boolean hasSenderPartyID ()
  {
    return StringHelper.isNotEmpty (m_sSenderPartyID);
  }

  /**
   * Remember the sender party ID (the ID of C3).
   *
   * @param s
   *        Sender party ID. May be <code>null</code>.
   */
  public void setSenderPartyID (@Nullable final String s)
  {
    m_sSenderPartyID = s;
  }

  public boolean hasXHEHeaderID ()
  {
    return StringHelper.isNotEmpty (m_sXHEHeaderID);
  }

  /**
   * Remember the XHE Header ID.
   *
   * @param s
   *        XHE instance identifier. May be <code>null</code>.
   */
  public void setXHEHeaderID (@Nullable final String s)
  {
    m_sXHEHeaderID = s;
  }

  public boolean hasC3EndpointURL ()
  {
    return StringHelper.isNotEmpty (m_sC3EndpointURL);
  }

  /**
   * Remember the AP endpoint URL of C3 determined by the SMP lookup.
   *
   * @param s
   *        C3 endpoint URL. May be <code>null</code>.
   */
  public void setC3EndpointURL (@Nullable final String s)
  {
    m_sC3EndpointURL = s;
  }

  public boolean hasC3Cert ()
  {
    return m_aC3Cert != null;
  }

  public boolean hasC3CertSubjectCN ()
  {
    return StringHelper.isNotEmpty (m_sC3CertSubjectCN);
  }

  /**
   * Remember the public DBNAlliance AP certificate of C3 determined by the SMP lookup.
   *
   * @param a
   *        C3 public DBNAlliance AP certificate. May be <code>null</code>.
   */
  public void setC3Cert (@Nullable final X509Certificate a)
  {
    m_aC3Cert = a;
    m_sC3CertSubjectCN = CertificateHelper.getSubjectCN (a);
  }

  public boolean hasC3CertCheckDT ()
  {
    return m_aC3CertCheckDT != null;
  }

  /**
   * Remember the date and time, when the DBNAlliance AP certificate of C3, as retrieved from the
   * SMP, was checked for revocation.
   *
   * @param a
   *        The DBNAlliance AP Certificate check date time. May be <code>null</code>.
   */
  public void setC3CertCheckDT (@Nullable final OffsetDateTime a)
  {
    m_aC3CertCheckDT = a;
  }

  public boolean hasC3CertCheckResult ()
  {
    return m_eC3CertCheckResult != null;
  }

  /**
   * Remember the result of checking the DBNAlliance AP certificate of C3, as retrieved from the
   * SMP, for validity.
   *
   * @param e
   *        The DBNAlliance AP Certificate check result. May be <code>null</code>.
   */
  public void setC3CertCheckResult (@Nullable final ECertificateCheckResult e)
  {
    m_eC3CertCheckResult = e;
  }

  public boolean hasAS4MessageID ()
  {
    return StringHelper.isNotEmpty (m_sAS4MessageID);
  }

  /**
   * Remember the AS4 Message ID used to send out the message.
   *
   * @param s
   *        The AS4 Message ID. May be <code>null</code>.
   */
  public void setAS4MessageID (@Nullable final String s)
  {
    m_sAS4MessageID = s;
  }

  public boolean hasAS4ConversationID ()
  {
    return StringHelper.isNotEmpty (m_sAS4ConversationID);
  }

  /**
   * Remember the AS4 Conversation ID used to send out the message.
   *
   * @param s
   *        The AS4 Conversation ID. May be <code>null</code>.
   */
  public void setAS4ConversationID (@Nullable final String s)
  {
    m_sAS4ConversationID = s;
  }

  public boolean hasAS4ReceivedSignalMsg ()
  {
    return m_aAS4ReceivedSignalMsg != null;
  }

  public boolean hasAS4ResponseErrors ()
  {
    return m_aAS4ResponseErrors != null && m_aAS4ResponseErrors.isNotEmpty ();
  }

  /**
   * Remember the synchronously received AS4 Signal Message from C3.
   *
   * @param a
   *        The parsed AS4 Signal Message. May be <code>null</code>.
   */
  public void setAS4ReceivedSignalMsg (@Nullable final Ebms3SignalMessage a)
  {
    m_aAS4ReceivedSignalMsg = a;
    if (a != null)
    {
      if (a.hasErrorEntries ())
      {
        m_aAS4ResponseErrors = new CommonsArrayList <> (a.getError ());
        m_bAS4ResponseError = true;
      }
      else
        m_bAS4ResponseError = false;
    }
    else
    {
      m_aAS4ResponseErrors = null;
      m_bAS4ResponseError = false;
    }
  }

  public boolean hasAS4SendingResult ()
  {
    return m_eAS4SendingResult != null;
  }

  /**
   * Remember the overall AS4 sending result.
   *
   * @param e
   *        The AS4 sending result. May be <code>null</code>.
   */
  public void setAS4SendingResult (@Nullable final EAS4UserMessageSendResult e)
  {
    m_eAS4SendingResult = e;
  }

  public boolean hasAS4SendingException ()
  {
    return m_aAS4SendingException != null;
  }

  /**
   * Remember any exception that eventually occurred on AS4 sending.
   *
   * @param e
   *        The exception from AS4 sending. May be <code>null</code>.
   */
  public void setAS4SendingException (@Nullable final Exception e)
  {
    m_aAS4SendingException = e;
  }

  /**
   * Remember the overall duration it took to perform the lookup and sending process.
   *
   * @param n
   *        The overall milliseconds needed. Must be &ge; 0.
   */
  public void setOverallDurationMillis (@Nonnegative final long n)
  {
    m_nOverallDurationMillis = n;
  }

  /**
   * Remember the overall sending success.
   *
   * @param b
   *        <code>true</code> on success, <code>false</code> on failure.
   */
  public void setSendingSuccess (final boolean b)
  {
    m_bSendingSuccess = b;
  }

  /**
   * Remember the overall success. This may differ from the sending success, if e.g. sending
   * succeeded but storing the record for DBNAlliance reporting failed.
   *
   * @param b
   *        <code>true</code> on success, <code>false</code> on failure.
   */
  public void setOverallSuccess (final boolean b)
  {
    m_bOverallSuccess = b;
  }

  /**
   * Get the whole report as one big JSON structure. Only elements that were provided, are contained
   * in the report.
   *
   * @return The sending report as a JSON object. May not be <code>null</code>.
   */
  @Nonnull
  public IJsonObject getAsJsonObject ()
  {
    // Function to convert Exception to JSON
    final Function <Exception, IJsonObject> fEx = ex -> new JsonObject ().add ("class", ex.getClass ().getName ())
                                                                         .add ("message", ex.getMessage ())
                                                                         .add ("stackTrace",
                                                                               StackTraceHelper.getStackAsString (ex));

    final IJsonObject aJson = new JsonObject ();
    aJson.add ("currentDateTimeUTC", PDTWebDateHelper.getAsStringXSD (m_aCurrentDateTimeUTC));
    aJson.add ("smlDnsZone", m_sSMLDNSZone);

    if (hasXHEParseException ())
      aJson.add ("XHEParsingException", fEx.apply (m_aXHEParseException));

    if (hasSenderID ())
      aJson.add ("senderId", m_aSenderID.getURIEncoded ());
    if (hasReceiverID ())
      aJson.add ("receiverId", m_aReceiverID.getURIEncoded ());
    if (hasDocTypeID ())
      aJson.add ("docTypeId", m_aDocTypeID.getURIEncoded ());
    if (hasProcessID ())
      aJson.add ("processId", m_aProcessID.getURIEncoded ());
    if (hasSenderPartyID ())
      aJson.add ("senderPartyId", m_sSenderPartyID);

    if (hasXHEHeaderID ())
      aJson.add ("XHEHeaderID", m_sXHEHeaderID);

    if (hasC3EndpointURL ())
      aJson.add ("c3EndpointUrl", m_sC3EndpointURL);
    if (hasC3Cert ())
      aJson.add ("c3Cert", CertificateHelper.getPEMEncodedCertificate (m_aC3Cert));
    if (hasC3CertSubjectCN ())
      aJson.add ("c3CertSubjectCN", m_sC3CertSubjectCN);
    if (hasC3CertCheckDT ())
      aJson.add ("c3CertCheckDT", PDTWebDateHelper.getAsStringXSD (m_aC3CertCheckDT));
    if (hasC3CertCheckResult ())
      aJson.add ("c3CertCheckResult", m_eC3CertCheckResult.name ());

    if (hasAS4MessageID ())
      aJson.add ("as4MessageId", m_sAS4MessageID);
    if (hasAS4ConversationID ())
      aJson.add ("as4ConversationId", m_sAS4ConversationID);

    if (hasAS4SendingResult ())
      aJson.add ("sendingResult", m_eAS4SendingResult.name ());
    if (hasAS4SendingException ())
      aJson.add ("sendingException", fEx.apply (m_aAS4SendingException));
    if (hasAS4ReceivedSignalMsg ())
      aJson.add ("as4ReceivedSignalMsg", new Ebms3SignalMessageMarshaller ().getAsString (m_aAS4ReceivedSignalMsg));
    aJson.add ("as4ResponseError", m_bAS4ResponseError);
    if (hasAS4ResponseErrors ())
    {
      final IJsonArray aErrors = new JsonArray ();
      for (final Ebms3Error aError : m_aAS4ResponseErrors)
      {
        final IJsonObject aErrorDetails = new JsonObject ();
        if (aError.getDescription () != null)
          aErrorDetails.add ("description", aError.getDescriptionValue ());
        if (aError.getErrorDetail () != null)
          aErrorDetails.add ("errorDetails", aError.getErrorDetail ());
        if (aError.getCategory () != null)
          aErrorDetails.add ("category", aError.getCategory ());
        if (aError.getRefToMessageInError () != null)
          aErrorDetails.add ("refToMessageInError", aError.getRefToMessageInError ());
        if (aError.getErrorCode () != null)
          aErrorDetails.add ("errorCode", aError.getErrorCode ());
        if (aError.getOrigin () != null)
          aErrorDetails.add ("origin", aError.getOrigin ());
        if (aError.getSeverity () != null)
          aErrorDetails.add ("severity", aError.getSeverity ());
        if (aError.getShortDescription () != null)
          aErrorDetails.add ("shortDescription", aError.getShortDescription ());
        aErrors.add (aErrorDetails);
      }
      aJson.add ("as4ResponseErrors", aErrors);
    }

    aJson.add ("overallDurationMillis", m_nOverallDurationMillis);
    aJson.add ("sendingSuccess", m_bSendingSuccess);
    aJson.add ("overallSuccess", m_bOverallSuccess);
    return aJson;
  }

  /**
   * @return The JSON representation of the sending report, as a formatted string. Never
   *         <code>null</code>.
   */
  @Nonnull
  public String getAsJsonString ()
  {
    return getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
  }

  /**
   * @param aJWS
   *        The JSON writer settings to use. May not be <code>null</code>.
   * @return The JSON representation of the sending report, as a string. Never <code>null</code>.
   */
  @Nonnull
  public String getAsJsonString (@Nonnull final IJsonWriterSettings aJWS)
  {
    return getAsJsonObject ().getAsJsonString (aJWS);
  }

  /**
   * Get the sending report as a MicroDOM element. Only elements that were provided, are contained
   * in the report.
   *
   * @param sNamespaceURI
   *        The namespace URI to be used. May be <code>null</code>.
   * @param sTagName
   *        The tag name to use for the root element. May neither be <code>null</code> nor empty.
   * @return The created micro element and never <code>null</code>.
   */
  @Nonnull
  public IMicroElement getAsMicroElement (@Nullable final String sNamespaceURI,
                                          @Nonnull @Nonempty final String sTagName)
  {
    final BiFunction <Exception, String, IMicroElement> fEx = (ex, tag) -> {
      final IMicroElement ret = new MicroElement (sNamespaceURI, tag);
      ret.addElementNS (sNamespaceURI, "Class").addText (ex.getClass ().getName ());
      ret.addElementNS (sNamespaceURI, "Message").addText (ex.getMessage ());
      ret.addElementNS (sNamespaceURI, "StackTrace").addText (StackTraceHelper.getStackAsString (ex));
      return ret;
    };

    final IMicroElement ret = new MicroElement (sNamespaceURI, sTagName);
    ret.addElementNS (sNamespaceURI, "CurrentDateTimeUTC")
       .addText (PDTWebDateHelper.getAsStringXSD (m_aCurrentDateTimeUTC));
    ret.addElementNS (sNamespaceURI, "SMLDNSZone").addText (m_sSMLDNSZone);

    if (hasXHEParseException ())
      ret.addChild (fEx.apply (m_aXHEParseException, "XHEParsingException"));

    if (hasSenderID ())
      ret.addElementNS (sNamespaceURI, "SenderID").addText (m_aSenderID.getURIEncoded ());
    if (hasReceiverID ())
      ret.addElementNS (sNamespaceURI, "ReceiverID").addText (m_aReceiverID.getURIEncoded ());
    if (hasDocTypeID ())
      ret.addElementNS (sNamespaceURI, "DocTypeID").addText (m_aDocTypeID.getURIEncoded ());
    if (hasProcessID ())
      ret.addElementNS (sNamespaceURI, "ProcessID").addText (m_aProcessID.getURIEncoded ());
    if (hasSenderPartyID ())
      ret.addElementNS (sNamespaceURI, "SenderPartyID").addText (m_sSenderPartyID);

    if (hasXHEHeaderID ())
      ret.addElementNS (sNamespaceURI, "XHEHeaderID").addText (m_sXHEHeaderID);

    if (hasC3EndpointURL ())
      ret.addElementNS (sNamespaceURI, "C3EndpointUrl").addText (m_sC3EndpointURL);
    if (hasC3Cert ())
      ret.addElementNS (sNamespaceURI, "C3Cert").addText (CertificateHelper.getPEMEncodedCertificate (m_aC3Cert));
    if (hasC3CertSubjectCN ())
      ret.addElementNS (sNamespaceURI, "C3CertSubjectCN").addText (m_sC3CertSubjectCN);
    if (hasC3CertCheckDT ())
      ret.addElementNS (sNamespaceURI, "C3CertCheckDT").addText (PDTWebDateHelper.getAsStringXSD (m_aC3CertCheckDT));
    if (hasC3CertCheckResult ())
      ret.addElementNS (sNamespaceURI, "C3CertCheckResult").addText (m_eC3CertCheckResult.name ());

    if (hasAS4MessageID ())
      ret.addElementNS (sNamespaceURI, "AS4MessageId").addText (m_sAS4MessageID);
    if (hasAS4ConversationID ())
      ret.addElementNS (sNamespaceURI, "AS4ConversationId").addText (m_sAS4ConversationID);

    if (hasAS4SendingResult ())
      ret.addElementNS (sNamespaceURI, "AS4SendingResult").addText (m_eAS4SendingResult.name ());
    if (hasAS4SendingException ())
      ret.addChild (fEx.apply (m_aAS4SendingException, "AS4SendingException"));
    if (hasAS4ReceivedSignalMsg ())
      ret.addElementNS (sNamespaceURI, "AS4ReceivedSignalMsg")
         .addChild (new Ebms3SignalMessageMarshaller ().getAsMicroElement (m_aAS4ReceivedSignalMsg));
    ret.addElementNS (sNamespaceURI, "AS4ResponseError").addText (m_bAS4ResponseError);
    if (hasAS4ResponseErrors ())
    {
      final IMicroElement aErrors = ret.addElementNS (sNamespaceURI, "AS4ResponseErrors");

      for (final Ebms3Error aError : m_aAS4ResponseErrors)
      {
        final IMicroElement aItem = aErrors.addElementNS (sNamespaceURI, "Item");
        if (aError.getDescription () != null)
          aItem.addElementNS (sNamespaceURI, "Description").addText (aError.getDescriptionValue ());
        if (aError.getErrorDetail () != null)
          aItem.addElementNS (sNamespaceURI, "ErrorDetails").addText (aError.getErrorDetail ());
        if (aError.getCategory () != null)
          aItem.addElementNS (sNamespaceURI, "Category").addText (aError.getCategory ());
        if (aError.getRefToMessageInError () != null)
          aItem.addElementNS (sNamespaceURI, "RefToMessageInError").addText (aError.getRefToMessageInError ());
        if (aError.getErrorCode () != null)
          aItem.addElementNS (sNamespaceURI, "ErrorCode").addText (aError.getErrorCode ());
        if (aError.getOrigin () != null)
          aItem.addElementNS (sNamespaceURI, "Origin").addText (aError.getOrigin ());
        if (aError.getSeverity () != null)
          aItem.addElementNS (sNamespaceURI, "Severity").addText (aError.getSeverity ());
        if (aError.getShortDescription () != null)
          aItem.addElementNS (sNamespaceURI, "ShortDescription").addText (aError.getShortDescription ());
      }
    }

    ret.addElementNS (sNamespaceURI, "OverallDurationMillis").addText (m_nOverallDurationMillis);
    ret.addElementNS (sNamespaceURI, "SendingSuccess").addText (m_bSendingSuccess);
    ret.addElementNS (sNamespaceURI, "OverallSuccess").addText (m_bOverallSuccess);
    return ret;
  }

  @Nonnull
  public String getAsXMLString ()
  {
    return getAsXMLString (null, new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN));
  }

  @Nonnull
  public String getAsXMLString (@Nullable final String sNamespaceURI, @Nonnull final IXMLWriterSettings aXWS)
  {
    return MicroWriter.getNodeAsString (getAsMicroElement (sNamespaceURI, "DBNAllianceSendingReport"), aXWS);
  }
}
