package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true;
    private final RequestContext requestContext;

    private ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */
    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all of the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */
    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
        List<TargetingPredicate> targetingPredicates = targetingGroup.getTargetingPredicates();
        Stream<TargetingPredicate> dataStream = targetingPredicates.stream();
        
/*        boolean allTruePredicates = true;
        for (TargetingPredicate predicate : targetingPredicates) {
            TargetingPredicateResult predicateResult = predicate.evaluate(requestContext);
            if (!predicateResult.isTrue()) {
                allTruePredicates = false;
                break;
            }
        }*/
/*        dataStream.filter(predicate -> {
            TargetingPredicateResult predicateResult = predicate.evaluate(requestContext);
            if (!predicateResult.isTrue()) {
                return false;
            }
            return true;
        });*/
        boolean allTruePredicates1 = dataStream.allMatch(predicate -> {
            TargetingPredicateResult predicateResult = TargetingPredicateResult.FALSE;
            Callable<TargetingPredicateResult> myCallable = () -> {
                // Code for the task to be executed asynchronously
                return predicate.evaluate(requestContext);
            };
            Future<TargetingPredicateResult> myFuture = executor.submit(myCallable);
            try {
                predicateResult = myFuture.get();
                return predicateResult.isTrue();
            } catch (Exception e){
                System.out.println("exception thrown");
            }
            if(predicateResult.equals(TargetingPredicateResult.TRUE))
                return true;
            else
                return false;
        });


        return allTruePredicates1 ? TargetingPredicateResult.TRUE :
                                   TargetingPredicateResult.FALSE;
    }
}
