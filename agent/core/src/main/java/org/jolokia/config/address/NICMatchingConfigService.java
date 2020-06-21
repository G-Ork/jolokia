package org.jolokia.config.address;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/*
 * 
 * Copyright 2020 Georg Tsakumagos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This service using the {@value #CONFIG_KEY} configuration to optain an
 * {@link InetAddress} from an matching network interfacename
 * 
 * @author Georg Tsakumagos
 * @since 21.06.2020
 *
 */
public class NICMatchingConfigService implements AddressConfigService {

    static final String CONFIG_KEY = "nicmatch";
    private static final String ERROR_PARSE_PATTERN = "Error parsing pattern: '%02$s' from config: '%01$s'.";
    private static final String ERROR_ENUM_NICS = "Error enumerate system NIC information for config: '%01$s'.";

    /**
     * Default Constructor
     */
    public NICMatchingConfigService() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final AtomicReference<InetAddress> optain(final Map<String, String> agentConfig) throws RuntimeException {
        final String value = agentConfig.get(CONFIG_KEY);

        if (null != value && value.length() > 0) {
            // Mark responsibility to the callee
            final AtomicReference<InetAddress> result = new AtomicReference<InetAddress>();
            
            try {
                final Pattern matchPattern = Pattern.compile(value);

                try {
                    Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

                    while (nets.hasMoreElements()) {
                        final NetworkInterface netIF = nets.nextElement();

                        if (matchPattern.matcher(netIF.getName()).matches()) {
                            final Enumeration<InetAddress> addresses = netIF.getInetAddresses();

                            if (addresses.hasMoreElements()) {
                                result.set(addresses.nextElement());
                                return result;
                            }
                        }
                    }

                } catch (final SocketException exception) {
                    throw new IllegalArgumentException(String.format(ERROR_ENUM_NICS, CONFIG_KEY, value), exception);
                }

            } catch (final PatternSyntaxException exception) {
                throw new IllegalArgumentException(String.format(ERROR_PARSE_PATTERN, CONFIG_KEY, value), exception);
            } finally {
                // // secure alternative -- if no host, use *loopback*
                if (null == result.get()) {
                    try {
                        result.set(InetAddress.getByName(null));
                    } catch (final UnknownHostException exception) {
                        throw new IllegalArgumentException("Can not lookup loopback interface", exception);
                    }
                }
            }
            return result;
        }
        return null;
    }
}