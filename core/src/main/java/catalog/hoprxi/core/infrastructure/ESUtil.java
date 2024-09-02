/*
 * Copyright (c) 2024. www.hoprxi.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package catalog.hoprxi.core.infrastructure;

import catalog.hoprxi.core.Bootstrap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import salt.hoprxi.crypto.util.AESUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2024-06-15
 */
public class ESUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ESUtil.class);
    private static final Pattern ENCRYPTED = Pattern.compile("^ENC:.*");
    private static final Properties props = new Properties();

    static {
        Config config = ConfigFactory.load("databases");
        List<? extends Config> databases = config.getConfigList("databases");
        for (Config database : databases) {
            if (database.getString("type").equals("read") && database.getString("provider").equals("elasticsearch")) {
                props.put("host", database.getString("host"));
                props.put("port", database.getInt("port"));
                String entry = database.getString("host") + ":" + database.getString("port");
                props.put("user", decrypt(entry, database.getString("user")));
                props.put("password", decrypt(entry, database.getString("password")));
            }
        }
    }

    public static String host() {
        return "slave.tooo.top";
    }

    public static int port() {
        return Integer.parseInt(props.getProperty("port", "9200"));
    }

    public static String encrypt() {
        return "Basic " + Base64.getEncoder().encodeToString((props.get("user") + ":" + props.get("password")).getBytes(StandardCharsets.UTF_8));
    }

    private static String decrypt(String entry, String securedPlainText) {
        if (ENCRYPTED.matcher(securedPlainText).matches()) {
            securedPlainText = securedPlainText.split(":")[1];
            byte[] aesData = Base64.getDecoder().decode(securedPlainText);
            Bootstrap.SECRET_KEY_MAP.get(entry);
            byte[] decryptData = AESUtil.decryptSpec(aesData, Bootstrap.SECRET_KEY_MAP.get(entry));
            return new String(decryptData, StandardCharsets.UTF_8);
        }
        return securedPlainText;
    }
}
