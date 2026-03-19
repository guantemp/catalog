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

import salt.hoprxi.crypto.PasswordService;

import java.sql.*;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.2 builder 2026-03-04
 */
public final class Setup {
    public static void setup() throws SQLException {
        String pass = PasswordService.nextStrongPasswd();
        String url = "jdbc:postgresql://slave.tooo.top:6543/postgres";
        try (Connection conn = DriverManager.getConnection(url, "postgres", "Qwe123465")) {
            Statement stmt = conn.createStatement();

            // ---------------------- 修正1：创建scale用户（先查是否存在） ----------------------
            // 查询scale用户是否存在
            String checkUserSql = "SELECT 1 FROM pg_roles WHERE rolname = 'scale'";
            ResultSet userRs = stmt.executeQuery(checkUserSql);
            if (!userRs.next()) { // 不存在则创建
                String createUserSql = String.format(
                        "CREATE USER scale WITH PASSWORD '%s' LOGIN;",
                        pass
                );
                stmt.executeUpdate(createUserSql);
                System.out.println("scale用户创建成功,password<UNK>" + pass);
            } else {
                System.out.println("scale用户已存在，跳过创建");
            }
            userRs.close();

            // ---------------------- 修正2：创建scale数据库（先查是否存在） ----------------------
            String checkDbSql = "SELECT 1 FROM pg_database WHERE datname = 'scale'";
            ResultSet dbRs = stmt.executeQuery(checkDbSql);
            if (!dbRs.next()) { // 不存在则创建
                String createDbSql = "CREATE DATABASE scale OWNER scale;";
                stmt.executeUpdate(createDbSql);
                System.out.println("scale数据库创建成功");
            } else {
                System.out.println("scale数据库已存在，跳过创建");
            }
            dbRs.close();

            stmt.close();
            conn.close();

            String SCALE_URL = "jdbc:postgresql://slave.tooo.top:6543/scale";
            Connection scaleConn = DriverManager.getConnection(SCALE_URL, "postgres", "Qwe123465");
            Statement scaleStmt = scaleConn.createStatement();
            // ---------------------- 修正3：创建grade枚举类型（先查是否存在） ----------------------
            String checkEnumSql = "SELECT 1 FROM pg_type WHERE typname = 'grade' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')";
            ResultSet enumRs = scaleStmt.executeQuery(checkEnumSql);
            if (!enumRs.next()) { // 不存在则创建
                String createEnumSql = "CREATE TYPE public.grade AS ENUM ('QUALIFIED', 'UNQUALIFIED', 'ONE_LEVEL','PREMIUM')";
                scaleStmt.executeUpdate(createEnumSql);
                System.out.println("grade枚举类型创建成功");
            } else {
                System.out.println("grade枚举类型已存在，跳过创建");
            }
            enumRs.close();

            scaleStmt.executeUpdate("""
                    CREATE TABLE IF NOT exists public.scale (
                    	plu int4 NOT NULL,
                    	"name" jsonb NOT NULL,
                    	category_id int8 NOT NULL,
                    	brand_id int8 NOT NULL,
                    	"grade" public."grade" DEFAULT 'QUALIFIED'::grade NULL,
                    	made_in jsonb DEFAULT '{"code": -1, "madeIn": "UNKNOWN"}'::jsonb NULL,
                    	spec varchar(64) NULL,
                    	shelf_life int4 DEFAULT 0 NULL,
                    	last_receipt_price jsonb DEFAULT '{"name": "最近入库价", "price": {"unit": "PCS", "number": 0, "currencyCode": "CNY"}}'::jsonb NULL,
                    	retail_price jsonb DEFAULT '{"unit": "PCS", "number": 0, "currencyCode": "CNY"}'::jsonb NULL,
                    	member_price jsonb DEFAULT '{"name": "会员价", "price": {"unit": "PCS", "number": 0, "currencyCode": "CNY"}}'::jsonb NULL,
                    	vip_price jsonb DEFAULT '{"name": "VIP价", "price": {"unit": "PCS", "number": 0, "currencyCode": "CNY"}}'::jsonb NULL,
                    	search_vector TSVECTOR,
                    	CONSTRAINT scale_pkey PRIMARY KEY (plu));
                    """);
            scaleStmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_scale_search_vector ON public.scale USING GIN (search_vector);
                    CREATE INDEX IF NOT exists idx_scale_retail_price_numeric ON scale USING BTREE (( (retail_price->>'number')::numeric ));
                    CREATE INDEX IF NOT EXISTS idx_scale_category_id ON scale(category_id);
                    CREATE INDEX IF NOT EXISTS idx_scale_brand_id ON scale(brand_id);
                    """);
            System.out.println("scale表创建成功");

            // 关闭scale库连接
            scaleStmt.close();
            scaleConn.close();
        }
    }
}
