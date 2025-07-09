/*
 * Copyright (c) 2025. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.core.application.setup;


/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.2 builder 2025-07-09
 */
public final class PsqlSetup {
    private static final String DDL_USER = "create user hoprxi createdb password '';";
    private static final String DDL_DATABASE = "create database catalog";
    private static final String DDL_BRAND = "CREATE TABLE public.brand (\n" +
            "id int8 NOT NULL,\n" +
            "\"name\" jsonb NULL,\n" +
            "about jsonb NULL,\n" +
            "CONSTRAINT brand_pkey PRIMARY KEY (id));" +
            "CREATE INDEX brand_name_index ON brand USING gin (((name ->> 'name'::text)) gin_trgm_ops);" +
            "insert into brand (id, name, about) values (-1, '{\"name\": \"undefined\", \"mnemonic\": \"undefined\",\"alias\": \"未定义\"}', null);";
    private static final String DDL_CATEGORY = "CREATE table if not exists category (\n" +
            "id int8 NOT NULL,\n" +
            "\"name\" jsonb NULL,\n" +
            "description varchar(512) NULL,\n" +
            "logo_uri varchar(512) NULL,\n" +
            "root_id int8 NOT NULL,\n" +
            "parent_id int8 NOT NULL,\n" +
            "\"left\" int4 NOT NULL,\n" +
            "\"right\" int4 NOT NULL,\n" +
            "CONSTRAINT category_pkey PRIMARY KEY (id),\n" +
            "CONSTRAINT category_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES category(id),\n" +
            "CONSTRAINT category_root_id_fkey FOREIGN KEY (root_id) REFERENCES category(id)\n" +
            ");\n" +
            "CREATE INDEX category_left_index ON category USING btree (root_id,\"left\");\n" +
            "CREATE INDEX category_right_index ON category USING btree (root_id,\"right\");\n" +
            "CREATE INDEX category_parentid_index ON category USING btree (parent_id);\n" +
            "CREATE INDEX category_full_index ON category USING btree (id,\"left\",\"right\",root_id);";

    public static void setup() {

    }

}
