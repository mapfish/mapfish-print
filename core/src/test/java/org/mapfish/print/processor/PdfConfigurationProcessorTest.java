package org.mapfish.print.processor;

import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.output.Values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PdfConfigurationProcessorTest {

    @Test
    public void testValidation() {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("title", "titleAtt");
        assertNumErrors(attributeMap, 0);

        attributeMap.clear();
        assertNumErrors(attributeMap, 1);

        attributeMap.put("title", "");
        assertNumErrors(attributeMap, 1);

        attributeMap.clear();
        attributeMap.put("blarg", "att");
        assertNumErrors(attributeMap, 1);

        attributeMap.clear();
        attributeMap.put("title", ".att");
        assertNumErrors(attributeMap, 1);

        attributeMap.clear();
        attributeMap.put("title", "att.inner");
        assertNumErrors(attributeMap, 0);

        attributeMap.clear();
        attributeMap.put("title", new PdfConfigurationProcessor.Update("att.inner", "%s"));
        assertNumErrors(attributeMap, 0);
    }

    @Test
    public void testExec() {
        Map<String, Object> attributeMap = new HashMap<>();
        final String titleKey = "titleAtt";
        String subjectKey = "subjectAtt";
        attributeMap.put("title", titleKey);
        attributeMap.put("subject", subjectKey);
        final PdfConfigurationProcessor pdfConfigurationProcessor = new PdfConfigurationProcessor();
        pdfConfigurationProcessor.setUpdates(attributeMap);
        PdfConfigurationProcessor.In in = new PdfConfigurationProcessor.In();
        in.values = new Values();
        in.pdfConfig = new PDFConfig();

        final String updatedTitle = "updatedTitle";
        final String updatedSubject = "updatedSubject";
        in.values.put(titleKey, updatedTitle);
        in.values.put(subjectKey, updatedSubject);
        pdfConfigurationProcessor.execute(in, null);
        assertEquals(updatedTitle, in.pdfConfig.getTitle());
        assertEquals(updatedSubject, in.pdfConfig.getSubject());

        attributeMap.clear();
        attributeMap.put("title", titleKey + ".value");
        pdfConfigurationProcessor.setUpdates(attributeMap);
        in.values.put(titleKey, new CustomTitleAtt(updatedTitle));
        pdfConfigurationProcessor.execute(in, null);
        assertEquals(updatedTitle, in.pdfConfig.getTitle());
        attributeMap.clear();

        attributeMap
                .put("title", new PdfConfigurationProcessor.Update(titleKey + ".value", "Print Report %s"));
        pdfConfigurationProcessor.setUpdates(attributeMap);
        in.values.put(titleKey, new CustomTitleAtt(updatedTitle));
        pdfConfigurationProcessor.execute(in, null);
        assertEquals("Print Report " + updatedTitle, in.pdfConfig.getTitle());
    }

    @Test
    public void testKeywords() throws Exception {
        Map<String, Object> attributeMap = new HashMap<>();
        String keywordsKey = "keywordsAtt";
        attributeMap.put("keywords", keywordsKey);
        final PdfConfigurationProcessor pdfConfigurationProcessor = new PdfConfigurationProcessor();
        pdfConfigurationProcessor.setUpdates(attributeMap);
        PdfConfigurationProcessor.In in = new PdfConfigurationProcessor.In();
        in.values = new Values();
        in.pdfConfig = new PDFConfig();

        final List<String> keywordList = Arrays.asList("1", " 2", " 3\n");
        in.values.put(keywordsKey, keywordList);
        pdfConfigurationProcessor.execute(in, null);
        assertEquals("1,2,3", in.pdfConfig.getKeywordsAsString());

        pdfConfigurationProcessor.setUpdates(attributeMap);
        in.values.put(keywordsKey, new String[]{"9", " 8 8", " 7"});
        pdfConfigurationProcessor.execute(in, null);
        assertEquals("9,8 8,7", in.pdfConfig.getKeywordsAsString());

        pdfConfigurationProcessor.setUpdates(attributeMap);
        in.values.put(keywordsKey, new LinkedHashSet<>(keywordList));
        pdfConfigurationProcessor.execute(in, null);
        assertEquals("1,2,3", in.pdfConfig.getKeywordsAsString());

        pdfConfigurationProcessor.setUpdates(attributeMap);
        in.values.put(keywordsKey, "4, 5,\n6");
        pdfConfigurationProcessor.execute(in, null);
        assertEquals("4,5,6", in.pdfConfig.getKeywordsAsString());

        pdfConfigurationProcessor.setUpdates(attributeMap);
        in.values.put(keywordsKey, new Integer[]{6, 7});
        pdfConfigurationProcessor.execute(in, null);
        assertEquals("6,7", in.pdfConfig.getKeywordsAsString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExec_WrongFieldName() {
        Map<String, Object> attributeMap = new HashMap<>();
        final String titleKey = "titleAtt";
        final PdfConfigurationProcessor pdfConfigurationProcessor = new PdfConfigurationProcessor();
        pdfConfigurationProcessor.setUpdates(attributeMap);
        PdfConfigurationProcessor.In in = new PdfConfigurationProcessor.In();
        in.values = new Values();
        in.pdfConfig = new PDFConfig();

        attributeMap.put("title", titleKey + ".xxx");
        pdfConfigurationProcessor.setUpdates(attributeMap);
        in.values.put(titleKey, new CustomTitleAtt());
        pdfConfigurationProcessor.execute(in, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExec_NullValue() {
        Map<String, Object> attributeMap = new HashMap<>();
        final String titleKey = "titleAtt";
        final PdfConfigurationProcessor pdfConfigurationProcessor = new PdfConfigurationProcessor();
        pdfConfigurationProcessor.setUpdates(attributeMap);
        PdfConfigurationProcessor.In in = new PdfConfigurationProcessor.In();
        in.values = new Values();
        in.pdfConfig = new PDFConfig();

        attributeMap.put("title", titleKey + ".value");
        pdfConfigurationProcessor.setUpdates(attributeMap);
        in.values.put(titleKey, new CustomTitleAtt());
        pdfConfigurationProcessor.execute(in, null);
    }

    private void assertNumErrors(Map<String, Object> attributeMap, int expectedNumErrors) {
        final PdfConfigurationProcessor pdfConfigurationProcessor = new PdfConfigurationProcessor();
        pdfConfigurationProcessor.setUpdates(attributeMap);
        List<Throwable> errors = new ArrayList<>();
        Configuration configuration = new Configuration();
        pdfConfigurationProcessor.validate(errors, configuration);
        assertEquals(expectedNumErrors, errors.size());
    }

    public static class CustomTitleAtt {
        public String value;

        public CustomTitleAtt(String updatedTitle) {
            this.value = updatedTitle;
        }

        public CustomTitleAtt() {
            // do nothing
        }
    }
}
