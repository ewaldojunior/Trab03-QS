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

package org.apache.flink.streaming.api.windowing.time;

import org.apache.flink.annotation.Public;

import java.util.concurrent.TimeUnit;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * The definition of a time interval for windowing. The time characteristic referred to is the
 * default time characteristic set on the execution environment.
 */
@Public
public final class Time {

    /** The time unit for this policy's time interval. */
    private final TimeUnit unit;

    /** The size of the windows generated by this policy. */
    private final long size;

    /** Instantiation only via factory method. */
    private Time(long size, TimeUnit unit) {
        this.unit = checkNotNull(unit, "time unit may not be null");
        this.size = size;
    }

    // ------------------------------------------------------------------------
    //  Properties
    // ------------------------------------------------------------------------

    /**
     * Gets the time unit for this policy's time interval.
     *
     * @return The time unit for this policy's time interval.
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Gets the length of this policy's time interval.
     *
     * @return The length of this policy's time interval.
     */
    public long getSize() {
        return size;
    }

    /**
     * Converts the time interval to milliseconds.
     *
     * @return The time interval in milliseconds.
     */
    public long toMilliseconds() {
        return unit.toMillis(size);
    }

    // ------------------------------------------------------------------------
    //  Factory
    // ------------------------------------------------------------------------

    /**
     * Creates a new {@link Time} of the given duration and {@link TimeUnit}.
     *
     * <p>The {@code Time} refers to the time characteristic that is set on the dataflow via {@link
     * org.apache.flink.streaming.api.environment.StreamExecutionEnvironment#setStreamTimeCharacteristic(org.apache.flink.streaming.api.TimeCharacteristic)}.
     *
     * @param size The duration of time.
     * @param unit The unit of time of the duration, for example {@code TimeUnit.SECONDS}.
     * @return The time policy.
     */
    public static Time of(long size, TimeUnit unit) {
        return new Time(size, unit);
    }

    /** Creates a new {@link Time} that represents the given number of milliseconds. */
    public static Time milliseconds(long milliseconds) {
        return of(milliseconds, TimeUnit.MILLISECONDS);
    }

    /** Creates a new {@link Time} that represents the given number of seconds. */
    public static Time seconds(long seconds) {
        return of(seconds, TimeUnit.SECONDS);
    }

    /** Creates a new {@link Time} that represents the given number of minutes. */
    public static Time minutes(long minutes) {
        return of(minutes, TimeUnit.MINUTES);
    }

    /** Creates a new {@link Time} that represents the given number of hours. */
    public static Time hours(long hours) {
        return of(hours, TimeUnit.HOURS);
    }

    /** Creates a new {@link Time} that represents the given number of days. */
    public static Time days(long days) {
        return of(days, TimeUnit.DAYS);
    }
}
