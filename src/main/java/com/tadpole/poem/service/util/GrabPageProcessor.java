package com.tadpole.poem.service.util;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlParagraph;
import com.tadpole.poem.domain.Author;
import com.tadpole.poem.domain.DetailResource;
import com.tadpole.poem.domain.Job;
import com.tadpole.poem.domain.Poem;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jerryjiang on 22/4/2016.
 */
public class GrabPageProcessor {


    public static final void grabPoemCaseOneOneZero(Job job) {
        try {
            URL url = new URL(job.getTarget());
            final HtmlPage singleDataPage = newWebClient().getPage(url);

            List<?> poemList = singleDataPage.getByXPath("/html/body/div[4]/div[1]/div[2]/p[5]");

            Object firstElementInPoem = poemList.get(0);
            if (firstElementInPoem != null && firstElementInPoem instanceof HtmlParagraph) {

                HtmlParagraph htmlParagraph = (HtmlParagraph) firstElementInPoem;

                System.out.println(htmlParagraph.asText());
            }


        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static WebClient newWebClient() {

        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setTimeout(15000);
        webClient.getOptions().setJavaScriptEnabled(false);

        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

        return webClient;
    }

    public static List<DetailResource> getPoemDetailUrls(Job job, WebClient webClient, int pageNumber, String type) {

        String baseUrl = job.getTarget();

        List<DetailResource> page = new ArrayList<>(10);

        String fullUrl = baseUrl + pageNumber;
        if (StringUtils.isNotEmpty(type)) {
            try {
                fullUrl += ("&t=" + URLEncoder.encode(type, "UTF-8"));

                System.err.println("Visiting " + fullUrl);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        try {

            HtmlPage htmlPage = webClient.getPage(new URL(fullUrl));

            List<HtmlDivision> divisions = (List<HtmlDivision>) htmlPage.getByXPath("//*[contains(concat(\" \", normalize-space(@class), \" \"), \" sons \")]");

            if (divisions == null || divisions.isEmpty()) {
                return null;
            }
            for (HtmlDivision division : divisions) {

                List<HtmlAnchor> anchors = division.getHtmlElementsByTagName("a");

                DetailResource matchedResourceUrl = getSinglePoemDetailUrl(anchors);

                if (matchedResourceUrl != null && !page.contains(matchedResourceUrl)) {
                    page.add(matchedResourceUrl);
                }
            }

        } catch (IOException e) {

            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();

        }

        return page;

    }

    private static DetailResource getSinglePoemDetailUrl(List<HtmlAnchor> anchors) {

        if (anchors == null) return null;

        for (HtmlAnchor anchor : anchors) {

            if (anchor.getHrefAttribute().startsWith("/view")) {

                DetailResource resource = new DetailResource();
                resource.setUrl(anchor.getHrefAttribute());
                resource.setOutsideId(MathUtil.getNumber(anchor.getHrefAttribute()).toString());
                resource.setTitle(anchor.getTextContent());

                return resource;

            }
        }

        return null;
    }

    public static Poem getPoemContent(Job job, DetailResource detailResource, WebClient webClient) {

        String fullUrl = job.getTarget() + detailResource.getUrl();

        Poem poem = new Poem();
        try {
            HtmlPage htmlPage = webClient.getPage(new URL(fullUrl));

            List<HtmlDivision> divisions = (List<HtmlDivision>) htmlPage.getByXPath("//*[contains(concat(\" \", normalize-space(@class), \" \"), \" son2 \")]");

            for (HtmlDivision division : divisions) {

                String text = division.getTextContent();
                if (text.contains("原文：")) {
                    int start = text.indexOf("原文：") + "原文：".length();

                    String content = text.substring(start);

                    poem.setContent(content.trim());
                    poem.setTitle(detailResource.getTitle());

                    List<HtmlAnchor> anchors = (List<HtmlAnchor>) division.getByXPath("//a");

                    for (HtmlAnchor htmlAnchor : anchors) {

                        String href = htmlAnchor.getHrefAttribute();

                        if (href.startsWith("/author_")) {
                            poem.setAnthorName(htmlAnchor.getTextContent());
                        }
                    }
                }

            }

        } catch (IOException e) {

            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();

        }

        return null;
    }

    public static void main(String[] args) throws Exception {
//%e7%99%bd%e5%b1%85%e6%98%93
        //%E7%99%BD%E5%B1%85%E6%98%93
        System.out.print(URLEncoder.encode("白居易", "utf-8"));

    }


}
