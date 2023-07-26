/*
 * Copyright (c) 2023. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure;


import salt.hoprxi.crypto.application.PasswordService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-08-22
 */
public class PsqlSetup {
    private static final String CREATE_BRAND_SQL = "create table if not exists brand ( id bigint not null primary key,name  jsonb, about jsonb );\n" +
            "create index if not exists brand_name_index on brand USING gin (name);\n" +
            "insert into brand (id, name, about) values (-1, '{\"name\": \"undefined\", \"mnemonic\": \"undefined\",\"alias\": \"未定义\"}', null);";
    private static final String CREATE_CATEGORY_SQL = "create table if not exists category\n" +
            "(\n" +
            "    id          bigint  not null\n" +
            "        primary key,\n" +
            "    name        jsonb,\n" +
            "    description varchar(512),\n" +
            "    logo_uri    varchar(512),\n" +
            "    root_id     bigint  not null REFERENCES category (id),\n" +
            "    parent_id   bigint  not null REFERENCES category (id),\n" +
            "    \"left\"      integer not null,\n" +
            "    \"right\"     integer not null\n" +
            ");\n" +
            "\n" +
            "CREATE EXTENSION if not exists pg_trgm;\n" +
            "create index if not exists category_name_index\n" +
            "    on category using gin ((name ->> 'name') gin_trgm_ops);\n" +
            "create index if not exists category_rootId_parentId_index\n" +
            "    on category (root_id, parent_id);\n" +
            "create index if not exists category_left_index\n" +
            "    on category (\"left\");\n" +
            "create index if not exists category_right_index\n" +
            "    on category (\"right\");";
    private static final String ddl = "create user hoprxi createdb password '';create database catalog";

    public static void setup(String databaseName) throws SQLException {
        String password = PasswordService.generateVeryStrongPassword();
        String user = "create user catalog with password '" + password + "'";
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            statement.addBatch(CREATE_BRAND_SQL);
            statement.addBatch(CREATE_CATEGORY_SQL);
            statement.executeBatch();
            connection.setAutoCommit(true);
            PsqlUtil.release(connection);
        }
    }
}
