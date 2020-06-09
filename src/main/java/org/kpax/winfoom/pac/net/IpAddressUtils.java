/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kpax.winfoom.pac.net;


import org.apache.commons.validator.routines.InetAddressValidator;
import org.kpax.winfoom.exception.CheckedExceptionWrapper;
import org.kpax.winfoom.exception.NativeException;
import org.kpax.winfoom.util.functional.SingletonSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * IP address utilities: resolving hostname, comparing IP addresses.
 */
public class IpAddressUtils {

    public static final String LOCALHOST = "127.0.0.1";

    public static final SingletonSupplier<InetAddress[]> ALL_PRIMARY_ADDRESSES = SingletonSupplier.of(() -> {
                try {
                    return InetAddress.getAllByName(HostnameUtils.removeDomain(HostnameUtils.getHostName()));
                } catch (UnknownHostException e) {
                    throw new CheckedExceptionWrapper(e);
                } catch (NativeException e) {
                    throw new CheckedExceptionWrapper(new UnknownHostException(e.getMessage() + ", error code : " + e.getErrorCode()));
                }
            }
    );

    public static final SingletonSupplier<InetAddress> PRIMARY_IPv4_ADDRESS = SingletonSupplier.of(() -> {
        try {
            return Arrays.stream(ALL_PRIMARY_ADDRESSES.get()).
                    filter(a -> a.getClass() == Inet4Address.class).
                    findFirst().orElseThrow(() -> new UnknownHostException("No IPv4 address found"));
        } catch (UnknownHostException e) {
            throw new CheckedExceptionWrapper(e);
        }
    });

    public static final Comparator<InetAddress> IPv6_FIRST_COMPARATOR = (a1, a2) -> {
        if (a1.getClass() == Inet4Address.class && a2.getClass() == Inet6Address.class) {
            return 1;
        } else if (a1.getClass() == Inet6Address.class && a2.getClass() == Inet4Address.class) {
            return -1;
        } else {
            return 0;
        }
    };

    public static final Comparator<InetAddress> IPv4_FIRST_COMPARATOR = (a1, a2) -> {
        if (a1.getClass() == Inet4Address.class && a2.getClass() == Inet6Address.class) {
            return -1;
        } else if (a1.getClass() == Inet6Address.class && a2.getClass() == Inet4Address.class) {
            return 1;
        } else {
            return 0;
        }
    };

    public static final Comparator<InetAddress> IPv6_FIRST_TOTAL_ORDERING_COMPARATOR = (a1, a2) -> {
        int compareByType = IPv6_FIRST_COMPARATOR.compare(a1, a2);
        if (compareByType == 0) {
            return compareByteByByte(a1, a2);
        }
        return compareByType;
    };

    private static final Logger logger = LoggerFactory.getLogger(IpAddressUtils.class);

    IpAddressUtils() {
    }

    private static int compareByteByByte(InetAddress a1, InetAddress a2) {
        byte[] bArr1 = a1.getAddress();
        byte[] bArr2 = a2.getAddress();
        // Compare byte-by-byte.
        for (int i = 0; i < bArr1.length; i++) {
            int x1 = Byte.toUnsignedInt(bArr1[i]);
            int x2 = Byte.toUnsignedInt(bArr2[i]);

            if (x1 == x2) {
                continue;
            }
            if (x1 < x2) {
                return -1;
            } else {
                return 1;
            }
        }
        return 0;
    }

    public static Comparator<InetAddress> addressComparator(boolean preferIPv6Addresses) {
        return preferIPv6Addresses ? IPv6_FIRST_COMPARATOR : IPv4_FIRST_COMPARATOR;
    }

    public static List<InetAddress> resolve(String host)
            throws UnknownHostException {
        return resolve(host, null);
    }

    public static List<InetAddress> resolve(String host,
                                            Predicate<InetAddress> filter)
            throws UnknownHostException {
        if (InetAddressValidator.getInstance().isValid(host)) {
            // No DNS lookup is needed in this case
            InetAddress addr = InetAddress.getByName(host);
            if (filter == null || filter.test(addr)) {
                return Collections.singletonList(addr);
            } else {
                return Collections.emptyList();
            }
        } else {
            InetAddress[] ipAddresses = InetAddress.getAllByName(host);
            Stream<InetAddress> addressStream = Arrays.stream(ipAddresses);
            if (filter != null) {
                addressStream = addressStream.filter(filter);
            }
            return addressStream.collect(Collectors.toList());
        }
    }

}
