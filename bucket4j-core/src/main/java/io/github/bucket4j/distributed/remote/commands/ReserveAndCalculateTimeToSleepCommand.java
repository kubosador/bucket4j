/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.distributed.remote.commands;

import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.MutableBucketEntry;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.remote.RemoteCommand;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import io.github.bucket4j.util.ComparableByContent;

import java.io.IOException;

import static io.github.bucket4j.serialization.PrimitiveSerializationHandles.LONG_HANDLE;


public class ReserveAndCalculateTimeToSleepCommand implements RemoteCommand<Long>, ComparableByContent<ReserveAndCalculateTimeToSleepCommand> {

    private long tokensToConsume;
    private long waitIfBusyNanosLimit;

    public static SerializationHandle<ReserveAndCalculateTimeToSleepCommand> SERIALIZATION_HANDLE = new SerializationHandle<ReserveAndCalculateTimeToSleepCommand>() {
        @Override
        public <S> ReserveAndCalculateTimeToSleepCommand deserialize(DeserializationAdapter<S> adapter, S input) throws IOException {
            long tokensToConsume = adapter.readLong(input);
            long waitIfBusyNanosLimit = adapter.readLong(input);

            return new ReserveAndCalculateTimeToSleepCommand(tokensToConsume, waitIfBusyNanosLimit);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, ReserveAndCalculateTimeToSleepCommand command) throws IOException {
            adapter.writeLong(output, command.tokensToConsume);
            adapter.writeLong(output, command.waitIfBusyNanosLimit);
        }

        @Override
        public int getTypeId() {
            return 23;
        }

        @Override
        public Class<ReserveAndCalculateTimeToSleepCommand> getSerializedType() {
            return ReserveAndCalculateTimeToSleepCommand.class;
        }

    };

    public ReserveAndCalculateTimeToSleepCommand(long tokensToConsume, long waitIfBusyNanosLimit) {
        this.tokensToConsume = tokensToConsume;
        this.waitIfBusyNanosLimit = waitIfBusyNanosLimit;
    }

    @Override
    public CommandResult<Long> execute(MutableBucketEntry mutableEntry, long currentTimeNanos) {
        if (!mutableEntry.exists()) {
            return CommandResult.bucketNotFound();
        }

        RemoteBucketState state = mutableEntry.get();
        state.refillAllBandwidth(currentTimeNanos);

        long nanosToCloseDeficit = state.calculateDelayNanosAfterWillBePossibleToConsume(tokensToConsume, currentTimeNanos);
        if (nanosToCloseDeficit == Long.MAX_VALUE || nanosToCloseDeficit > waitIfBusyNanosLimit) {
            return CommandResult.MAX_VALUE;
        } else {
            state.consume(tokensToConsume);
            mutableEntry.set(state);
            return CommandResult.success(nanosToCloseDeficit, LONG_HANDLE);
        }
    }

    public long getTokensToConsume() {
        return tokensToConsume;
    }

    public long getWaitIfBusyNanosLimit() {
        return waitIfBusyNanosLimit;
    }

    @Override
    public SerializationHandle getSerializationHandle() {
        return SERIALIZATION_HANDLE;
    }

    @Override
    public boolean equalsByContent(ReserveAndCalculateTimeToSleepCommand other) {
        return tokensToConsume == other.tokensToConsume &&
                waitIfBusyNanosLimit == other.waitIfBusyNanosLimit;
    }
}