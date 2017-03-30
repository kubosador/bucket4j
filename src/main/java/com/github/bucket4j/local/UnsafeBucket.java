/*
 * Copyright 2015 Vladimir Bukhtoyarov
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.bucket4j.local;


import com.github.bucket4j.AbstractBucket;
import com.github.bucket4j.Bandwidth;
import com.github.bucket4j.BucketConfiguration;
import com.github.bucket4j.BucketState;

public class UnsafeBucket extends AbstractBucket {

    private final BucketState state;
    private final BucketConfiguration configuration;

    public UnsafeBucket(BucketConfiguration configuration) {
        super(configuration);
        this.configuration = configuration;
        this.state = BucketState.createInitialState(configuration);
    }

    @Override
    protected long consumeAsMuchAsPossibleImpl(long limit) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();

        state.refillAllBandwidth(bandwidths, currentTimeNanos);
        long availableToConsume = state.getAvailableTokens(bandwidths);
        long toConsume = Math.min(limit, availableToConsume);
        if (toConsume == 0) {
            return 0;
        }
        state.consume(bandwidths, toConsume);
        return toConsume;
    }

    @Override
    protected boolean tryConsumeImpl(long tokensToConsume) {
        Bandwidth[] bandwidths = configuration.getBandwidths();
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();

        state.refillAllBandwidth(bandwidths, currentTimeNanos);
        long availableToConsume = state.getAvailableTokens(bandwidths);
        if (tokensToConsume > availableToConsume) {
            return false;
        }
        state.consume(bandwidths, tokensToConsume);
        return true;
    }

    @Override
    protected boolean consumeOrAwaitImpl(long tokensToConsume, long waitIfBusyTimeLimit) throws InterruptedException {
        // TODO
        return false;
    }

    @Override
    protected void addTokensIml(long tokensToAdd) {
        Bandwidth[] limits = configuration.getBandwidths();
        long currentTimeNanos = configuration.getTimeMeter().currentTimeNanos();
        state.refillAllBandwidth(limits, currentTimeNanos);
        state.addTokens(limits, tokensToAdd);
    }

    @Override
    public BucketState createSnapshot() {
        return state.clone();
    }

    @Override
    public String toString() {
        return "LockFreeBucket{" +
                "state=" + state +
                ", configuration=" + configuration +
                '}';
    }

}