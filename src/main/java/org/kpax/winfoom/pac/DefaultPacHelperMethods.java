package org.kpax.winfoom.pac;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.pac.datetime.PacUtilsDateTime;
import org.kpax.winfoom.pac.net.IpAddressMatcher;
import org.kpax.winfoom.pac.net.IpAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Default implementation of a the PAC 'helper functions'.
 *
 * <p>
 * This implementation aims to be as complete and compatible as possible. It
 * implements both the original specification from Netscape, plus the additions
 * from Microsoft.
 *
 * <ul>
 *    <li>Complete. Implements all methods from original Netscape specification
 *        as well as all IPv6 aware additions from Microsoft.</li>
 *    <li>Compatible. Aims for maximum compatibility with all browser
 *         implementations and aims to never fail and never stall.</li>
 * </ul>
 *
 * @author lbruun
 */
@Lazy
@Component
public class DefaultPacHelperMethods implements PacHelperMethodsNetscape, PacHelperMethodsMicrosoft {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPacHelperMethods.class);

    @Autowired
    private SystemConfig systemConfig;

    // *************************************************************
    //  Official helper functions.
    // 
    //  These are pure-Java implementations of the JavaScript
    //  helper functions defined in Netscape's original document.
    // *************************************************************

    @Override
    public boolean isPlainHostName(String host) {
        return !host.contains(".");
    }

    @Override
    public boolean dnsDomainIs(String host, String domain) {
        int dotPos = host.indexOf(".");
        if (dotPos != -1 && dotPos < host.length() - 1) {
            if (host.substring(dotPos).equals(domain)) {
                return true;
            }
            return host.substring(dotPos + 1).equals(domain);
        }
        return false;
    }

    @Override
    public boolean localHostOrDomainIs(String host, String hostdom) {
        if (host.equals(hostdom)) {
            return true;
        }
        return Arrays.stream(hostdom.split("\\.")).
                filter(StringUtils::isNotEmpty).
                findFirst().
                map(host::equals).
                orElse(false);
    }

    @Override
    public boolean isResolvable(String host) {
        try {
            return !IpAddressUtils.resolve(host, a -> a.getClass() == Inet4Address.class).isEmpty();
        } catch (UnknownHostException ex) {
            logger.debug("Error on resolving host [{}]", host);
            return false;
        }
    }

    @Override
    public String dnsResolve(String host) {
        try {
            List<InetAddress> addresses = IpAddressUtils.resolve(host, a -> a.getClass() == Inet4Address.class);
            if (!addresses.isEmpty()) {
                return addresses.get(0).getHostAddress();
            }
        } catch (UnknownHostException ex) {
            logger.debug("Error on resolving host [{}]", host);
        }
        // Returning null is what Chrome and Firefox do in this situation
        return null;
    }

    @Override
    public String myIpAddress() {
        try {
            return IpAddressUtils.PRIMARY_IPv4_ADDRESS.get().getHostAddress();
        } catch (Exception e) {
            logger.warn("Cannot get localhost ip address", e);
            return IpAddressUtils.LOCALHOST;
        }
    }

    @Override
    public boolean isInNet(String host, String pattern, String mask) {
        final String dnsResolve = dnsResolve(host);
        if (dnsResolve == null) {
            return false;
        }
        return new SubnetUtils(pattern, mask).getInfo().isInRange(dnsResolve);
    }

    @Override
    public int dnsDomainLevels(String host) {
        return StringUtils.countMatches(host, '.');
    }

    @Override
    public boolean shExpMatch(String str, String shexp) {
        Pattern pattern = PacUtils.createGlobRegexPattern(shexp);
        return pattern.matcher(str).matches();
    }


    @Override
    public boolean weekdayRange(Object... args) {
        try {
            return PacUtilsDateTime.isInWeekdayRange(new Date(), args);
        } catch (PacUtilsDateTime.PacDateTimeInputException ex) {
            logger.warn("PAC script error : arguments passed to weekdayRange() function {} are faulty: {}",
                    Arrays.toString(args), ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean dateRange(Object... args) {
        try {
            return PacUtilsDateTime.isInDateRange(new Date(), args);
        } catch (PacUtilsDateTime.PacDateTimeInputException ex) {
            logger.warn("PAC script error : arguments passed to dateRange() function {} are faulty: {}",
                    Arrays.toString(args), ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean timeRange(Object... args) {
        try {
            return PacUtilsDateTime.isInTimeRange(new Date(), args);
        } catch (PacUtilsDateTime.PacDateTimeInputException ex) {
            logger.warn("PAC script error : arguments passed to timeRange() function {} are faulty: {}",
                    Arrays.toString(args), ex.getMessage());
            return false;
        }
    }

    // *************************************************************
    //  Microsoft extensions
    // 
    // *************************************************************

    @Override
    public boolean isResolvableEx(String host) {
        try {
            return !IpAddressUtils.resolve(host).isEmpty();
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    @Override
    public String dnsResolveEx(String host) {
        try {
            List<InetAddress> addresses = IpAddressUtils.resolve(host);
            if (!addresses.isEmpty()) {
                if (addresses.size() > 1) {
                    Collections.sort(addresses, IpAddressUtils.addressComparator(systemConfig.isPreferIPv6Addresses()));
                }
                return addresses.get(0).getHostAddress();
            }
        } catch (UnknownHostException ex) {
            logger.debug("Error on resolving host [{}]", host);
        }
        return "";
    }

    @Override
    public String myIpAddressEx() {
        try {
            InetAddress[] addresses = IpAddressUtils.ALL_PRIMARY_ADDRESSES.get();
            return Arrays.stream(addresses).
                    sorted(IpAddressUtils.addressComparator(systemConfig.isPreferIPv6Addresses())).
                    map(InetAddress::getHostAddress).
                    collect(Collectors.joining(";"));
        } catch (Exception e) {
            logger.warn("Cannot get localhost ip addresses", e);
            return IpAddressUtils.LOCALHOST;
        }
    }

    @Override
    public String sortIpAddressList(String ipAddressList) {
        if (StringUtils.isEmpty(ipAddressList)) {
            return "";
        }

        // We convert to InetAddress (because we know how to sort
        // those) but at the same time we have to preserve the way
        // the original input was represented and return in the same
        // format.
        String[] arrAddresses = ipAddressList.split(";");
        List<InetAddress> addresses = new ArrayList<>();
        for (String host : arrAddresses) {
            try {
                addresses.add(InetAddress.getByName(host.trim()));
            } catch (UnknownHostException ex) {
                return "";
            }
        }

        return addresses.stream().
                sorted(IpAddressUtils.IPv6_FIRST_TOTAL_ORDERING_COMPARATOR).
                map(a -> arrAddresses[addresses.indexOf(a)].trim()).
                collect(Collectors.joining(";"));
    }

    @Override
    public String getClientVersion() {
        return "1.0";
    }

    @Override
    public boolean isInNetEx(String ipAddress, String ipPrefix) {
        try {
            return new IpAddressMatcher(ipPrefix).matches(ipAddress);
        } catch (UnknownHostException e) {
            return false;
        }
    }


    // *************************************************************
    //  Utility functions.
    // 
    //  Other functions - not defined in PAC spec, but still 
    //  exposed to the JavaScript engine.
    // *************************************************************

    public void alert(String message) {
        logger.debug("PAC script says : {}", message);
    }


}
