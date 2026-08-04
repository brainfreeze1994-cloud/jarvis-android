package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.Locale;

/**
 * UAELawHelper — Argument Shield for UAE disputes.
 * Detects legal queries and provides deep-links to UAE government complaint portals.
 */
public class UAELawHelper {

    public enum LegalCategory {
        TENANCY, LABOUR, CONSUMER, TRAFFIC, FINANCIAL, GENERAL
    }

    public static boolean isLegalQuery(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);
        return t.matches(".*\\b(rights|illegal|fine|visa|labour|labor|tenancy|landlord|employer|salary|" +
            "contract|dispute|complaint|sue|court|arrested|deportation|refund|rera|mohre|rta|ded|" +
            "cbuae|consumer protection|traffic fine|work permit|am i entitled|uae law|dubai law|" +
            "can they|is it legal|my landlord|my employer|my boss|my company)\\b.*");
    }

    public static LegalCategory classify(String text) {
        String t = text.toLowerCase(Locale.US);
        if (t.matches(".*\\b(landlord|tenant|rent|apartment|flat|villa|deposit|eviction|rera|lease)\\b.*"))
            return LegalCategory.TENANCY;
        if (t.matches(".*\\b(employer|employee|salary|salary|labour|labor|mohre|visa|termination|end of service|annual leave|overtime|wps)\\b.*"))
            return LegalCategory.LABOUR;
        if (t.matches(".*\\b(shop|store|refund|warranty|product|consumer|ded|misleading|overcharged)\\b.*"))
            return LegalCategory.CONSUMER;
        if (t.matches(".*\\b(traffic|fine|salik|nol|parking|rta|accident|license|speed camera)\\b.*"))
            return LegalCategory.TRAFFIC;
        if (t.matches(".*\\b(bank|loan|mortgage|credit card|interest|charges|cbuae|financial)\\b.*"))
            return LegalCategory.FINANCIAL;
        return LegalCategory.GENERAL;
    }

    /** Open relevant UAE government complaint portal */
    public static void openComplaintPortal(Context ctx, LegalCategory category) {
        String url;
        switch (category) {
            case TENANCY:   url = "https://dubairest.ae"; break;
            case LABOUR:    url = "https://www.mohre.gov.ae/en/services/labour-complaints.aspx"; break;
            case CONSUMER:  url = "https://consumerdubai.ded.ae"; break;
            case TRAFFIC:   url = "https://www.rta.ae/wps/portal/rta/ae/home/rta-services/traffic-fines"; break;
            case FINANCIAL: url = "https://www.cbuae.gov.ae/en/page/consumer-protection"; break;
            default:        url = "https://services.dubai.gov.ae/en/services-directory/"; break;
        }
        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    /** Get complaint portal name for display */
    public static String getPortalName(LegalCategory category) {
        switch (category) {
            case TENANCY:   return "RERA (Dubai REST)";
            case LABOUR:    return "MOHRE";
            case CONSUMER:  return "DED Consumer";
            case TRAFFIC:   return "RTA";
            case FINANCIAL: return "CBUAE";
            default:        return "Dubai Services Portal";
        }
    }

    /** Get the complaint hotline */
    public static String getHotline(LegalCategory category) {
        switch (category) {
            case TENANCY:   return "800RERA (7372)";
            case LABOUR:    return "800MOHRE (80 6473)";
            case CONSUMER:  return "600545555 (DED)";
            case TRAFFIC:   return "8009090 (RTA)";
            case FINANCIAL: return "800CBUAE (22823)";
            default:        return "800 DUBAI (38224)";
        }
    }
}
