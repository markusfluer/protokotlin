package com.protokotlin.util

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.OptIn
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Google protobuf Timestamp representation
 * Represents a point in time independent of any time zone or calendar,
 * represented as seconds and fractions of seconds at nanosecond
 * resolution in UTC Epoch time.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Timestamp(
    @ProtoNumber(1)
    val seconds: Long = 0L,
    @ProtoNumber(2) 
    val nanos: Int = 0
) {
    /**
     * Convert protobuf Timestamp to kotlinx.datetime.Instant
     */
    fun toInstant(): Instant {
        return Instant.fromEpochSeconds(seconds, nanos.toLong())
    }
    
    companion object {
        /**
         * Create protobuf Timestamp from kotlinx.datetime.Instant
         */
        fun fromInstant(instant: Instant): Timestamp {
            return Timestamp(
                seconds = instant.epochSeconds,
                nanos = instant.nanosecondsOfSecond
            )
        }
        
        /**
         * Create protobuf Timestamp from current time
         */
        fun now(): Timestamp {
            return fromInstant(kotlinx.datetime.Clock.System.now())
        }
    }
}

/**
 * Google protobuf Duration representation
 * Represents a signed, fixed-length span of time represented
 * as a count of seconds and fractions of seconds at nanosecond
 * resolution.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProtoDuration(
    @ProtoNumber(1)
    val seconds: Long = 0L,
    @ProtoNumber(2)
    val nanos: Int = 0
) {
    /**
     * Convert protobuf Duration to kotlin.time.Duration
     */
    fun toDuration(): Duration {
        val secondsDuration = Duration.convert(seconds.toDouble(), DurationUnit.SECONDS, DurationUnit.NANOSECONDS)
        val nanosDuration = nanos.toDouble()
        return Duration.convert(secondsDuration + nanosDuration, DurationUnit.NANOSECONDS, DurationUnit.NANOSECONDS)
    }
    
    companion object {
        /**
         * Create protobuf Duration from kotlin.time.Duration
         */
        fun fromDuration(duration: Duration): ProtoDuration {
            val totalNanos = duration.inWholeNanoseconds
            val seconds = totalNanos / 1_000_000_000L
            val nanos = (totalNanos % 1_000_000_000L).toInt()
            return ProtoDuration(seconds, nanos)
        }
    }
}