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


import salt.hoprxi.crypto.PasswordService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-08-22
 */
public final class PsqlSetup {
    private static final String DDL_BRAND = "CREATE TABLE public.brand (\n" +
            "\tid int8 NOT NULL,\n" +
            "\t\"name\" jsonb NULL,\n" +
            "\tabout jsonb NULL,\n" +
            "\tCONSTRAINT brand_pkey PRIMARY KEY (id));" +
            "insert into brand (id, name, about) values (-1, '{\"name\": \"undefined\", \"mnemonic\": \"undefined\",\"alias\": \"未定义\"}', null);";
    private static final String DDL_CATEGORY = "CREATE TABLE category (\n" +
            "id int8 NOT NULL,\n" +
            "\"name\" jsonb NULL,\n" +
            "description varchar(512) NULL,\n" +
            "logo_uri varchar(512) NULL,\n" +
            "root_id int8 NOT NULL,\n" +
            "parent_id int8 NOT NULL,\n" +
            "\"left\" int4 NOT NULL,\n" +
            "\"right\" int4 NOT NULL,\n" +
            "CONSTRAINT category_pkey PRIMARY KEY (id),\n" +
            "CONSTRAINT category_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.category(id),\n" +
            "CONSTRAINT category_root_id_fkey FOREIGN KEY (root_id) REFERENCES public.category(id));\n" +
            "CREATE INDEX category_left_index ON category USING btree (\"left\");\n" +
            "CREATE INDEX category_right_index ON category USING btree (\"right\");\n" +
            "CREATE INDEX category_rootid_parentid_index ON category USING btree (root_id, parent_id);";
    private static final String ddl = "create user hoprxi createdb password '';create database catalog";

    public static void setup(String databaseName) throws SQLException {
        String password = PasswordService.nextStrongPasswd();
        String user = "create user catalog with password '" + password + "'";
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            statement.addBatch(DDL_BRAND);
            statement.addBatch(DDL_CATEGORY);
            statement.executeBatch();
            connection.setAutoCommit(true);
            PsqlUtil.release(connection);
        }
    }
}
