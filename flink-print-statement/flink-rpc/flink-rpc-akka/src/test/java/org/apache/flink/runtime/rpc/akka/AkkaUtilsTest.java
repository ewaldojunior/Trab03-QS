/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.rpc.akka;

import org.apache.flink.configuration.AkkaOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.SecurityOptions;
import org.apache.flink.runtime.rpc.AddressResolution;
import org.apache.flink.runtime.rpc.RpcSystem;
import org.apache.flink.util.NetUtils;

import com.typesafe.config.Config;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for the {@link AkkaUtils}. */
class AkkaUtilsTest {

    @Test
    void getHostFromAkkaURLForRemoteAkkaURL() throws Exception {
        final String host = "127.0.0.1";
        final int port = 1234;

        final InetSocketAddress address = new InetSocketAddress(host, port);

        final String remoteAkkaUrl =
                AkkaRpcServiceUtils.getRpcUrl(
                        host,
                        port,
                        "actor",
                        AddressResolution.NO_ADDRESS_RESOLUTION,
                        AkkaRpcServiceUtils.AkkaProtocol.TCP);

        final InetSocketAddress result = AkkaUtils.getInetSocketAddressFromAkkaURL(remoteAkkaUrl);

        assertThat(result).isEqualTo(address);
    }

    @Test
    void getHostFromAkkaURLThrowsExceptionIfAddressCannotBeRetrieved() throws Exception {
        final String localAkkaURL = "akka://flink/user/actor";

        assertThatThrownBy(() -> AkkaUtils.getInetSocketAddressFromAkkaURL(localAkkaURL))
                .isInstanceOf(Exception.class);
    }

    @Test
    void getHostFromAkkaURLReturnsHostAfterAtSign() throws Exception {
        final String url = "akka.tcp://flink@localhost:1234/user/jobmanager";
        final InetSocketAddress expected = new InetSocketAddress("localhost", 1234);

        final InetSocketAddress result = AkkaUtils.getInetSocketAddressFromAkkaURL(url);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getHostFromAkkaURLHandlesAkkaTcpProtocol() throws Exception {
        final String url = "akka.tcp://flink@localhost:1234/user/jobmanager";
        final InetSocketAddress expected = new InetSocketAddress("localhost", 1234);

        final InetSocketAddress result = AkkaUtils.getInetSocketAddressFromAkkaURL(url);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getHostFromAkkaURLHandlesAkkaSslTcpProtocol() throws Exception {
        final String url = "akka.ssl.tcp://flink@localhost:1234/user/jobmanager";
        final InetSocketAddress expected = new InetSocketAddress("localhost", 1234);

        final InetSocketAddress result = AkkaUtils.getInetSocketAddressFromAkkaURL(url);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getHostFromAkkaURLHandlesIPv4Addresses() throws Exception {
        final String ipv4Address = "192.168.0.1";
        final int port = 1234;
        final InetSocketAddress address = new InetSocketAddress(ipv4Address, port);

        final String url = "akka://flink@" + ipv4Address + ":" + port + "/user/jobmanager";
        final InetSocketAddress result = AkkaUtils.getInetSocketAddressFromAkkaURL(url);

        assertThat(result).isEqualTo(address);
    }

    @Test
    void getHostFromAkkaURLHandlesIPv6Addresses() throws Exception {
        final String ipv6Address = "2001:db8:10:11:12:ff00:42:8329";
        final int port = 1234;
        final InetSocketAddress address = new InetSocketAddress(ipv6Address, port);

        final String url = "akka://flink@[" + ipv6Address + "]:" + port + "/user/jobmanager";
        final InetSocketAddress result = AkkaUtils.getInetSocketAddressFromAkkaURL(url);

        assertThat(result).isEqualTo(address);
    }

    @Test
    void getHostFromAkkaURLHandlesIPv6AddressesTcp() throws Exception {
        final String ipv6Address = "2001:db8:10:11:12:ff00:42:8329";
        final int port = 1234;
        final InetSocketAddress address = new InetSocketAddress(ipv6Address, port);

        final String url = "akka.tcp://flink@[" + ipv6Address + "]:" + port + "/user/jobmanager";
        final InetSocketAddress result = AkkaUtils.getInetSocketAddressFromAkkaURL(url);

        assertThat(result).isEqualTo(address);
    }

    @Test
    void getHostFromAkkaURLHandlesIPv6AddressesSsl() throws Exception {
        final String ipv6Address = "2001:db8:10:11:12:ff00:42:8329";
        final int port = 1234;
        final InetSocketAddress address = new InetSocketAddress(ipv6Address, port);

        final String url =
                "akka.ssl.tcp://flink@[" + ipv6Address + "]:" + port + "/user/jobmanager";
        final InetSocketAddress result = AkkaUtils.getInetSocketAddressFromAkkaURL(url);

        assertThat(result).isEqualTo(address);
    }

    @Test
    void getAkkaConfigNormalizesHostName() {
        final Configuration configuration = new Configuration();
        final String hostname = "AbC123foOBaR";
        final int port = 1234;

        final Config akkaConfig =
                AkkaUtils.getAkkaConfig(configuration, new HostAndPort(hostname, port));

        assertThat(akkaConfig.getString("akka.remote.classic.netty.tcp.hostname"))
                .isEqualTo(NetUtils.unresolvedHostToNormalizedString(hostname));
    }

    @Test
    void getAkkaConfigDefaultsToLocalHost() throws UnknownHostException {
        final Config akkaConfig =
                AkkaUtils.getAkkaConfig(new Configuration(), new HostAndPort("", 0));

        final String hostname = akkaConfig.getString("akka.remote.classic.netty.tcp.hostname");

        assertThat(InetAddress.getByName(hostname).isLoopbackAddress()).isTrue();
    }

    @Test
    void getAkkaConfigDefaultsToForkJoinExecutor() {
        final Config akkaConfig = AkkaUtils.getAkkaConfig(new Configuration(), null);

        assertThat(akkaConfig.getString("akka.actor.default-dispatcher.executor"))
                .isEqualTo("fork-join-executor");
    }

    @Test
    void getAkkaConfigSetsExecutorWithThreadPriority() {
        final int threadPriority = 3;
        final int minThreads = 1;
        final int maxThreads = 3;

        final Config akkaConfig =
                AkkaUtils.getAkkaConfig(
                        new Configuration(),
                        new HostAndPort("localhost", 1234),
                        null,
                        AkkaUtils.getThreadPoolExecutorConfig(
                                new RpcSystem.FixedThreadPoolExecutorConfiguration(
                                        minThreads, maxThreads, threadPriority)));

        assertThat(akkaConfig.getString("akka.actor.default-dispatcher.executor"))
                .isEqualTo("thread-pool-executor");
        assertThat(akkaConfig.getInt("akka.actor.default-dispatcher.thread-priority"))
                .isEqualTo(threadPriority);
        assertThat(
                        akkaConfig.getInt(
                                "akka.actor.default-dispatcher.thread-pool-executor.core-pool-size-min"))
                .isEqualTo(minThreads);
        assertThat(
                        akkaConfig.getInt(
                                "akka.actor.default-dispatcher.thread-pool-executor.core-pool-size-max"))
                .isEqualTo(maxThreads);
    }

    @Test
    void getAkkaConfigHandlesIPv6Address() {
        final String ipv6AddressString = "2001:db8:10:11:12:ff00:42:8329";
        final Config akkaConfig =
                AkkaUtils.getAkkaConfig(
                        new Configuration(), new HostAndPort(ipv6AddressString, 1234));

        assertThat(akkaConfig.getString("akka.remote.classic.netty.tcp.hostname"))
                .isEqualTo(NetUtils.unresolvedHostToNormalizedString(ipv6AddressString));
    }

    @Test
    void getAkkaConfigDefaultsStartupTimeoutTo10TimesOfAskTimeout() {
        final Configuration configuration = new Configuration();
        configuration.set(AkkaOptions.ASK_TIMEOUT_DURATION, Duration.ofMillis(100));

        final Config akkaConfig =
                AkkaUtils.getAkkaConfig(configuration, new HostAndPort("localhost", 31337));

        assertThat(akkaConfig.getString("akka.remote.startup-timeout")).isEqualTo("1000ms");
    }

    @Test
    void getAkkaConfigSslEngineProviderWithoutCertFingerprint() {
        final Configuration configuration = new Configuration();
        configuration.setBoolean(SecurityOptions.SSL_INTERNAL_ENABLED, true);

        final Config akkaConfig =
                AkkaUtils.getAkkaConfig(configuration, new HostAndPort("localhost", 31337));
        final Config sslConfig = akkaConfig.getConfig("akka.remote.classic.netty.ssl");

        assertThat(sslConfig.getString("ssl-engine-provider"))
                .isEqualTo("org.apache.flink.runtime.rpc.akka.CustomSSLEngineProvider");
        assertThat(sslConfig.getStringList("security.cert-fingerprints")).isEmpty();
    }

    @Test
    void getAkkaConfigSslEngineProviderWithCertFingerprint() {
        final Configuration configuration = new Configuration();
        configuration.setBoolean(SecurityOptions.SSL_INTERNAL_ENABLED, true);

        final String fingerprint = "A8:98:5D:3A:65:E5:E5:C4:B2:D7:D6:6D:40:C6:DD:2F:B1:9C:54:36";
        configuration.setString(SecurityOptions.SSL_INTERNAL_CERT_FINGERPRINT, fingerprint);

        final Config akkaConfig =
                AkkaUtils.getAkkaConfig(configuration, new HostAndPort("localhost", 31337));
        final Config sslConfig = akkaConfig.getConfig("akka.remote.classic.netty.ssl");

        assertThat(sslConfig.getString("ssl-engine-provider"))
                .isEqualTo("org.apache.flink.runtime.rpc.akka.CustomSSLEngineProvider");
        assertThat(sslConfig.getStringList("security.cert-fingerprints")).contains(fingerprint);
    }
}
