/*
 *
 * Copyright 2015-2019 Vladimir Bukhtoyarov
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

package io.github.bucket4j.distributed.remote;

import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;

import java.io.IOException;

public interface RemoteCommand<T> {

    CommandResult<T> execute(MutableBucketEntry mutableEntry, long currentTimeNanos);

    default boolean isInitializationCommand() {
        return false;
    }

    SerializationHandle<RemoteCommand<?>> getSerializationHandle();

    static <O> void serialize(SerializationAdapter<O> adapter, O output, RemoteCommand<?> command) throws IOException {
        SerializationHandle<RemoteCommand<?>> serializer = command.getSerializationHandle();
        adapter.writeInt(output, serializer.getTypeId());
        serializer.serialize(adapter, output, command);
    }

    static <I> RemoteCommand<?> deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
        int typeId = adapter.readInt(input);
        SerializationHandle<?> serializer = SerializationHandle.CORE_HANDLES.getHandleByTypeId(typeId);
        return (RemoteCommand<?>) serializer.deserialize(adapter, input);
    }

}