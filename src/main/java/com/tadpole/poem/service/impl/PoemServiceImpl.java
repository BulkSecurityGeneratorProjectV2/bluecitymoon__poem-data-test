package com.tadpole.poem.service.impl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.tadpole.poem.domain.*;
import com.tadpole.poem.repository.AuthorRepository;
import com.tadpole.poem.repository.DetailResourceRepository;
import com.tadpole.poem.repository.JobLogRepository;
import com.tadpole.poem.service.AuthorService;
import com.tadpole.poem.service.PoemService;
import com.tadpole.poem.repository.PoemRepository;
import com.tadpole.poem.service.util.GrabPageProcessor;
import com.tadpole.poem.service.util.PinyinTranslator;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service Implementation for managing Poem.
 */
@Service
public class PoemServiceImpl implements PoemService {

    private final Logger log = LoggerFactory.getLogger(PoemServiceImpl.class);

    @Inject
    private PoemRepository poemRepository;

    @Inject
    private DetailResourceRepository detailResourceRepository;

    @Inject
    private AuthorRepository authorRepository;

    @Inject
    private AuthorService authorService;

    @Inject
    private JobLogRepository jobLogRepository;

    /**
     * Save a poem.
     *
     * @param poem the entity to save
     * @return the persisted entity
     */
    public Poem save(Poem poem) {
        log.debug("Request to save Poem : {}", poem);
        Poem result = poemRepository.save(poem);
        return result;
    }

    /**
     * Get all the poems.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Poem> findAll(Pageable pageable) {
        log.debug("Request to get all Poems");
        Page<Poem> result = poemRepository.findAll(pageable);
        return result;
    }

    /**
     * Get one poem by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Poem findOne(Long id) {
        log.debug("Request to get Poem : {}", id);
        Poem poem = poemRepository.findOne(id);
        return poem;
    }

    /**
     * Delete the  poem by id.
     *
     * @param id the id of the entity
     */
    @Transactional
    public void delete(Long id) {
        log.debug("Request to delete Poem : {}", id);
        poemRepository.delete(id);
    }

    /**
     * @param job
     * @param detailResource
     * @return
     */
    public Poem grabSinglePoem(Job job, DetailResource detailResource, WebClient webClient) {

        Poem existedPoem = poemRepository.findByResourceId(detailResource.getOutsideId());

        JobLog jobLog = new JobLog();
        if (existedPoem != null) {

            jobLog.setJob(job);
            jobLog.setStart(ZonedDateTime.now());
            jobLog.setMessage("skip saving existed poem" + existedPoem.getTitle());

            jobLogRepository.save(jobLog);

            return null;
        }


        String fullUrl = "http://so.gushiwen.org" + detailResource.getUrl();

        Poem poem = new Poem();
        poem.setTitle(detailResource.getTitle());
        poem.setTitlePinyin(PinyinTranslator.getFullSpell(detailResource.getTitle()));
        poem.setTag(detailResource.getTag());
        poem.setResourceId(detailResource.getOutsideId());
        try {

           Document document = Jsoup.connect(fullUrl).get();

            Element element = document.getElementsByClass("son2").last();
            Elements elements = element.getElementsByTag("p");

            String periodElement = elements.get(0).text().substring("朝代：".length());
            poem.setPeriod(periodElement.trim());

            Elements authorElements = elements.get(1).children();
            Element authorElement = null;
            String authorName = "";
            if (authorElements.size() == 2) {

                authorElement = authorElements.last();
                authorName = authorElement.text();

                String authorHref = authorElement.attr("href");
                Author author = authorRepository.findByLink(authorHref);

                if (author == null) {

                    Author newAuthor = new Author();
                    newAuthor.setLink(authorHref);
                    newAuthor.setName(authorName);

                    Author savedAuthor = authorService.save(newAuthor);

                    poem.setAuthor(savedAuthor);
                } else {
                    poem.setAuthor(author);
                }

            } else {
                authorName = elements.get(1).text().substring("作者：".length());
            }

            poem.setAnthorName(authorName);

            //poem content is placed in <p>
            if (elements.size() > 3) {
                String content = "";
                for (int i = 3; i < elements.size(); i++) {
                    Element singleContentElement = elements.get(i);

                    String singleLineContent = singleContentElement.text().trim();
                    if (StringUtils.isNotEmpty(singleLineContent)) {
                        content += PinyinTranslator.removeGuahaoThingsInString(singleLineContent);
                    }
                }

                poem.setContent(content);

                //poem content placed directly.
            } else {
                String poemContentTotal = element.text().substring(element.text().indexOf("原文：") + "原文：".length());
                poem.setContent(PinyinTranslator.removeGuahaoThingsInString(poemContentTotal));
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());

        }


        //too long, I don't want it
        if (StringUtils.isNotEmpty(poem.getContent()) && poem.getContent().length() > 20000) {
            detailResource.setActive(false);

            detailResourceRepository.save(detailResource);

        } else if (StringUtils.isEmpty(poem.getContent())){

            System.out.println(poem.toString());
        } else {

            try {

                poemRepository.save(poem);

                jobLog.setJob(job);
                jobLog.setStart(ZonedDateTime.now());
                jobLog.setMessage("save new poem " + poem.getTitle());


                jobLogRepository.save(jobLog);

            } catch (org.springframework.orm.jpa.JpaSystemException e) {

                jobLog.setJob(job);
                jobLog.setStart(ZonedDateTime.now());
                jobLog.setMessage("save poem with exception " + poem.getTitle() + " resource = " + detailResource.getUrl());

                return null;
            }

        }

        return poem;
    }

    /**
     * @param job
     * @return
     */
    public boolean grabAllPoems(Job job) {

        List<DetailResource> urls = detailResourceRepository.findByVisitCountIsNull();

        WebClient webClient = GrabPageProcessor.newWebClient();

        for (DetailResource detailResource : urls) {
            grabSinglePoem(job, detailResource, webClient);

            Integer visitCount = detailResource.getVisitCount();
            if (visitCount == null) {
                detailResource.setVisitCount(1);
            } else {
                detailResource.setVisitCount(visitCount + 1);
            }

            detailResourceRepository.save(detailResource);
        }
        return false;
    }
}
