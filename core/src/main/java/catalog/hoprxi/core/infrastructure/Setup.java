/*
 * Copyright (c) 2022. www.hoprxi.com All Rights Reserved.
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

import org.postgresql.util.PGobject;
import salt.hoprxi.crypto.PasswordService;

import java.sql.*;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK8.0
 * @version 0.0.1 builder 2022-08-22
 */
public class Setup {
    private static final String CREATE_BRAND_SQL = "create table if not exists brand\n" +
            "(\n" +
            "    id    bigint not null\n" +
            "        primary key,\n" +
            "    name  jsonb,\n" +
            "    about jsonb\n" +
            ");\n" +
            "\n" +
            "create index if not exists brand_name_index\n" +
            "    on brand USING gin (name);";
    String ddl = "create user hoprxi createdb password '';create database catalog";

    public static void setup(String databaseName) throws SQLException {
        String password = PasswordService.generateVeryStrongPassword();
        String user = "create user hoprxi with password '" + password + "'";
        System.out.println(user);
        try (Connection connection = PsqlUtil.getConnection(databaseName)) {
            Statement statement = connection.createStatement();
            statement.execute(CREATE_BRAND_SQL);

            final String replaceInto = "insert into brand (id,name,about) values (?,?,?) on conflict(id) do update set name=?,about=?";
            PreparedStatement preparedStatement = connection.prepareStatement(replaceInto);

            preparedStatement.setLong(1, -2L);
            PGobject name = new PGobject();
            name.setType("jsonb");
            name.setValue("{\"name\":\"undefined\",\"mnemonic\":\"undefined\",\"alias\":\"我想改变\"}");
            PGobject about = new PGobject();
            about.setType("jsonb");
            about.setValue(null);
            preparedStatement.setObject(2, name);
            preparedStatement.setObject(3, about);
            preparedStatement.setObject(4, name);
            preparedStatement.setObject(5, about);
            preparedStatement.execute();

            ResultSet rs = statement.executeQuery("select id,name from brand where id=-1");
            while (rs.next()) {
                System.out.println(rs.getString("name"));
                System.out.println(rs.getString("id"));
            }
            PsqlUtil.release(connection);


        }
    }
}
