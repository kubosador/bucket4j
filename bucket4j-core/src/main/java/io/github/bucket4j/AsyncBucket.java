package io.github.bucket4j;

import java.util.concurrent.CompletableFuture;

/**
 * Created by vladimir.bukhtoyarov on 09.08.2017.
 */
public interface AsyncBucket {

    /**
     * Tries to consume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     * @return {@code true} if the tokens were consumed, {@code false} otherwise.
     */
    CompletableFuture<Boolean> tryConsume(long numTokens);

    /**
     * Tries to consume a specified number of tokens from this bucket.
     *
     * @param numTokens The number of tokens to consume from the bucket, must be a positive number.
     * @return {@link ConsumptionProbe} which describes both result of consumption and tokens remaining in the bucket after consumption.
     */
    CompletableFuture<ConsumptionProbe> tryConsumeAndReturnRemaining(long numTokens);

    /**
     * Tries to consume as much tokens from this bucket as available at the moment of invocation.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    CompletableFuture<Long> tryConsumeAsMuchAsPossible();

    /**
     * Tries to consume as much tokens from bucket as available in the bucket at the moment of invocation,
     * but tokens which should be consumed is limited by than not more than {@code limit}.
     *
     * @param limit maximum number of tokens to consume, should be positive.
     *
     * @return number of tokens which has been consumed, or zero if was consumed nothing.
     */
    CompletableFuture<Long> tryConsumeAsMuchAsPossible(long limit);

    /**
     * Consumes a specified number of tokens from the bucket. If required count of tokens is not currently available then this method will block
     * until  required number of tokens will be available or current thread is interrupted, or {@code maxWaitTimeNanos} has elapsed.
     *
     * @param numTokens The number of tokens to consume from the bucket.
     * @param maxWaitTimeNanos limit of time which thread can wait.
     * @param blockingStrategy specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket
     *
     * @return true if {@code numTokens} has been consumed or false when {@code numTokens} has not been consumed
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     * @throws IllegalArgumentException if <tt>numTokens</tt> is greater than capacity of bucket
     */
    CompletableFuture<Boolean> consume(long numTokens, long maxWaitTimeNanos, DelayedCompletionStrategy blockingStrategy) throws InterruptedException;

    /**
     * Consumes {@code numTokens} from the bucket. If enough tokens are not currently available then this method will block
     * until required number of tokens will be available or current thread is interrupted.
     *
     * @param numTokens The number of tokens to consume from bucket, must be a positive number.
     * @param blockingStrategy specifies the way to block current thread to amount of time required to refill missed number of tokens in the bucket
     *
     * @throws InterruptedException in case of current thread has been interrupted during waiting
     * @throws IllegalArgumentException if <tt>numTokens</tt> is greater than capacity of bucket
     */
    CompletableFuture<Void> consume(long numTokens, DelayedCompletionStrategy blockingStrategy) throws InterruptedException;

    /**
     * Add <tt>tokensToAdd</tt> to each bandwidth of bucket.
     * Resulted count of tokens are calculated by following formula:
     * <pre>newTokens = Math.min(capacity, currentTokens + tokensToAdd)</pre>
     * in other words resulted number of tokens never exceeds capacity independent of <tt>tokensToAdd</tt>.
     *
     * <h3>Example of usage</h3>
     * The "compensating transaction" is one of obvious use case, when any piece of code consumed tokens from bucket, tried to do something and failed, the "addTokens" will be helpful to return tokens back to bucket:
     * <pre>{@code
     *      Bucket wallet;
     *      ...
     *      wallet.consume(50); // get 50 cents from wallet
     *      try {
     *          buyCocaCola();
     *      } catch(NoCocaColaException e) {
     *          // return money to wallet
     *          wallet.addTokens(50);
     *      }
     * }</pre>
     *
     * @param tokensToAdd number of tokens to add
     * @throws IllegalArgumentException in case of tokensToAdd less than 1
     */
    CompletableFuture<Void> addTokens(long tokensToAdd);


}