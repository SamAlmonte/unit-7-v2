package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.model.AdvertisementContent;
import com.amazon.ata.advertising.service.model.EmptyGeneratedAdvertisement;
import com.amazon.ata.advertising.service.model.GeneratedAdvertisement;
import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;

import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

/**
 * This class is responsible for picking the advertisement to be rendered.
 */
public class AdvertisementSelectionLogic {

    private static final Logger LOG = LogManager.getLogger(AdvertisementSelectionLogic.class);

    private final ReadableDao<String, List<AdvertisementContent>> contentDao;
    private final ReadableDao<String, List<TargetingGroup>> targetingGroupDao;
    private Random random = new Random();

    /**
     * Constructor for AdvertisementSelectionLogic.
     * @param contentDao Source of advertising content.
     * @param targetingGroupDao Source of targeting groups for each advertising content.
     */
    @Inject
    public AdvertisementSelectionLogic(ReadableDao<String, List<AdvertisementContent>> contentDao,
                                       ReadableDao<String, List<TargetingGroup>> targetingGroupDao) {
        this.contentDao = contentDao;
        this.targetingGroupDao = targetingGroupDao;
    }

    /**
     * Setter for Random class.
     * @param random generates random number used to select advertisements.
     */
    public void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Gets all of the content and metadata for the marketplace and determines which content can be shown.  Returns the
     * eligible content with the highest click through rate.  If no advertisement is available or eligible, returns an
     * EmptyGeneratedAdvertisement.
     *
     * @param customerId - the customer to generate a custom advertisement for
     * @param marketplaceId - the id of the marketplace the advertisement will be rendered on
     * @return an advertisement customized for the customer id provided, or an empty advertisement if one could
     *     not be generated.
     */
    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {
        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();
        if (StringUtils.isEmpty(marketplaceId)) {
            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
        } else {
            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);
            //Stream<AdvertisementContent> streamData = contents.stream();
            TargetingEvaluator targetingEvaluator = new TargetingEvaluator(new RequestContext(customerId, marketplaceId));
            //List<AdvertisementContent> validContents = new ArrayList<>();
            List<AdvertisementContent> eligibleAds = new ArrayList<>();
/*            for (AdvertisementContent content: contents){
                if(content.getContentId() != null){
                    List<TargetingGroup> targetingGroup1 = targetingGroupDao.get(content.getContentId());
                    for (TargetingGroup adTargets : targetingGroup1){
                        if(targetingEvaluator.evaluate(adTargets).equals(TargetingPredicateResult.TRUE)){
                            validContents.add(content);
                        }
                    }
                }
            }*/
            eligibleAds = new ArrayList<>(contents.stream().filter(content->{
                if(content.getContentId()!= null){
                    List<TargetingGroup> targetingGroups = targetingGroupDao.get(content.getContentId());
                    for(TargetingGroup targetingGroup: targetingGroups){
                        if(targetingEvaluator.evaluate(targetingGroup).isTrue()){
                            return true;
                        }
                    }
                }
                return false;
            }).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(eligibleAds)) {
                AdvertisementContent randomAdvertisementContent = eligibleAds.get(random.nextInt(eligibleAds.size()));
                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
            }

        }

        return generatedAdvertisement;
    }

/*    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {

//        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();
//        if (StringUtils.isEmpty(marketplaceId)) {
//            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
//        } else {
//            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);
//
//            if (CollectionUtils.isNotEmpty(contents)) {
//                AdvertisementContent randomAdvertisementContent = contents.get(random.nextInt(contents.size()));
//                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
//            }
//
//        }

        if (StringUtils.isEmpty(marketplaceId)) {
            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
            return new EmptyGeneratedAdvertisement();
        }

        List<AdvertisementContent> contents = contentDao.get(marketplaceId);

        List<AdvertisementContent> eligibleAds = new ArrayList<>();
        TargetingEvaluator targetingEvaluator = new TargetingEvaluator(new RequestContext(customerId, marketplaceId));

        eligibleAds = new ArrayList<>(contents.stream().filter(content->{
            if(content.getContentId()!= null){
                List<TargetingGroup> targetingGroups = targetingGroupDao.get(content.getContentId());
                for(TargetingGroup targetingGroup: targetingGroups){
                    if(targetingEvaluator.evaluate(targetingGroup).isTrue()){
                        return true;
                    }
                }
            }
            return false;
        }).collect(Collectors.toList()));

        System.out.println(eligibleAds.size());

        if (eligibleAds.size() == 0) {
            return new EmptyGeneratedAdvertisement();
        } else {
            AdvertisementContent ad = eligibleAds.get(random.nextInt(eligibleAds.size()));
            return new GeneratedAdvertisement(ad);
        }

    }*/
}
