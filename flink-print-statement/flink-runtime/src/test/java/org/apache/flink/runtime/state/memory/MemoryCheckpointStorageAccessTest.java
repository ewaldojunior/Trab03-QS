/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.state.memory;

import org.apache.flink.api.common.JobID;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.CheckpointMetadataOutputStream;
import org.apache.flink.runtime.state.CheckpointStateOutputStream;
import org.apache.flink.runtime.state.CheckpointStorageAccess;
import org.apache.flink.runtime.state.CheckpointStorageLocation;
import org.apache.flink.runtime.state.CompletedCheckpointStorageLocation;
import org.apache.flink.runtime.state.StreamStateHandle;
import org.apache.flink.runtime.state.filesystem.AbstractFileCheckpointStorageAccessTestBase;
import org.apache.flink.runtime.state.memory.MemCheckpointStreamFactory.MemoryCheckpointOutputStream;

import org.junit.Test;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for the {@link MemoryBackendCheckpointStorageAccess}, which implements the checkpoint
 * storage aspects of the {@link MemoryStateBackend}.
 */
public class MemoryCheckpointStorageAccessTest extends AbstractFileCheckpointStorageAccessTestBase {

    private static final int DEFAULT_MAX_STATE_SIZE = MemoryStateBackend.DEFAULT_MAX_STATE_SIZE;

    // ------------------------------------------------------------------------
    //  General Fs-based checkpoint storage tests, inherited
    // ------------------------------------------------------------------------

    @Override
    protected CheckpointStorageAccess createCheckpointStorage(Path checkpointDir) throws Exception {
        return new MemoryBackendCheckpointStorageAccess(
                new JobID(), checkpointDir, null, DEFAULT_MAX_STATE_SIZE);
    }

    @Override
    protected CheckpointStorageAccess createCheckpointStorageWithSavepointDir(
            Path checkpointDir, Path savepointDir) throws Exception {
        return new MemoryBackendCheckpointStorageAccess(
                new JobID(), checkpointDir, savepointDir, DEFAULT_MAX_STATE_SIZE);
    }

    // ------------------------------------------------------------------------
    //  MemoryCheckpointStorage-specific tests
    // ------------------------------------------------------------------------

    @Test
    public void testParametrizationDefault() throws Exception {
        final JobID jid = new JobID();

        MemoryStateBackend backend = new MemoryStateBackend();

        MemoryBackendCheckpointStorageAccess storage =
                (MemoryBackendCheckpointStorageAccess) backend.createCheckpointStorage(jid);

        assertFalse(storage.supportsHighlyAvailableStorage());
        assertFalse(storage.hasDefaultSavepointLocation());
        assertNull(storage.getDefaultSavepointDirectory());
        assertEquals(MemoryStateBackend.DEFAULT_MAX_STATE_SIZE, storage.getMaxStateSize());
    }

    @Test
    public void testParametrizationDirectories() throws Exception {
        final JobID jid = new JobID();
        final Path checkpointPath = new Path(tmp.newFolder().toURI().toString());
        final Path savepointPath = new Path(tmp.newFolder().toURI().toString());

        MemoryStateBackend backend =
                new MemoryStateBackend(checkpointPath.toString(), savepointPath.toString());

        MemoryBackendCheckpointStorageAccess storage =
                (MemoryBackendCheckpointStorageAccess) backend.createCheckpointStorage(jid);

        assertTrue(storage.supportsHighlyAvailableStorage());
        assertTrue(storage.hasDefaultSavepointLocation());
        assertNotNull(storage.getDefaultSavepointDirectory());

        assertEquals(savepointPath, storage.getDefaultSavepointDirectory());
    }

    @Test
    public void testParametrizationStateSize() throws Exception {
        final int maxSize = 17;

        MemoryStateBackend backend = new MemoryStateBackend(maxSize);
        MemoryBackendCheckpointStorageAccess storage =
                (MemoryBackendCheckpointStorageAccess) backend.createCheckpointStorage(new JobID());

        assertEquals(maxSize, storage.getMaxStateSize());
    }

    @Test
    public void testNonPersistentCheckpointLocation() throws Exception {
        MemoryBackendCheckpointStorageAccess storage =
                new MemoryBackendCheckpointStorageAccess(
                        new JobID(), null, null, DEFAULT_MAX_STATE_SIZE);

        CheckpointStorageLocation location = storage.initializeLocationForCheckpoint(9);

        CheckpointMetadataOutputStream stream = location.createMetadataOutputStream();
        stream.write(99);

        CompletedCheckpointStorageLocation completed = stream.closeAndFinalizeCheckpoint();
        StreamStateHandle handle = completed.getMetadataHandle();
        assertTrue(handle instanceof ByteStreamStateHandle);

        // the reference is not valid in that case
        try {
            storage.resolveCheckpoint(completed.getExternalPointer());
            fail("should fail with an exception");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testLocationReference() throws Exception {
        // non persistent memory state backend for checkpoint
        {
            MemoryBackendCheckpointStorageAccess storage =
                    new MemoryBackendCheckpointStorageAccess(
                            new JobID(), null, null, DEFAULT_MAX_STATE_SIZE);
            CheckpointStorageLocation location = storage.initializeLocationForCheckpoint(42);
            assertTrue(location.getLocationReference().isDefaultReference());
        }

        // non persistent memory state backend for checkpoint
        {
            MemoryBackendCheckpointStorageAccess storage =
                    new MemoryBackendCheckpointStorageAccess(
                            new JobID(), randomTempPath(), null, DEFAULT_MAX_STATE_SIZE);
            CheckpointStorageLocation location = storage.initializeLocationForCheckpoint(42);
            assertTrue(location.getLocationReference().isDefaultReference());
        }

        // memory state backend for savepoint
        {
            MemoryBackendCheckpointStorageAccess storage =
                    new MemoryBackendCheckpointStorageAccess(
                            new JobID(), null, null, DEFAULT_MAX_STATE_SIZE);
            CheckpointStorageLocation location =
                    storage.initializeLocationForSavepoint(1337, randomTempPath().toString());
            assertTrue(location.getLocationReference().isDefaultReference());
        }
    }

    @Test
    public void testTaskOwnedStateStream() throws Exception {
        final List<String> state = Arrays.asList("Flopsy", "Mopsy", "Cotton Tail", "Peter");

        final MemoryBackendCheckpointStorageAccess storage =
                new MemoryBackendCheckpointStorageAccess(
                        new JobID(), null, null, DEFAULT_MAX_STATE_SIZE);

        StreamStateHandle stateHandle;

        try (CheckpointStateOutputStream stream = storage.createTaskOwnedStateStream()) {
            assertTrue(stream instanceof MemoryCheckpointOutputStream);

            new ObjectOutputStream(stream).writeObject(state);
            stateHandle = stream.closeAndGetHandle();
        }

        try (ObjectInputStream in = new ObjectInputStream(stateHandle.openInputStream())) {
            assertEquals(state, in.readObject());
        }
    }

    /**
     * This test checks that the expected mkdirs action for checkpoint storage, only called when
     * initializeLocationForCheckpoint.
     */
    @Test
    public void testStorageLocationMkdirs() throws Exception {
        MemoryBackendCheckpointStorageAccess storage =
                new MemoryBackendCheckpointStorageAccess(
                        new JobID(), randomTempPath(), null, DEFAULT_MAX_STATE_SIZE);

        File baseDir = new File(storage.getCheckpointsDirectory().getPath());
        assertFalse(baseDir.exists());

        // mkdirs only be called when initializeLocationForCheckpoint
        storage.initializeLocationForCheckpoint(177L);
        assertTrue(baseDir.exists());
    }
}
