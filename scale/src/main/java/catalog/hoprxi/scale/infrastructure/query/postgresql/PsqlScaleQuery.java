/*
 * Copyright (c) 2026. www.hoprxi.com All Rights Reserved.
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

package catalog.hoprxi.scale.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
import catalog.hoprxi.core.infrastructure.query.JsonByteBufOutputStream;
import catalog.hoprxi.scale.application.query.ScaleQuery;
import catalog.hoprxi.scale.application.query.SqlClause;
import catalog.hoprxi.scale.application.query.SqlClauseSpec;
import catalog.hoprxi.scale.domain.model.Plu;
import catalog.hoprxi.scale.infrastructure.PsqlUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuan</a>
 * @version 0.1 2026/3/7
 * @since JDK 21
 */

public class PsqlScaleQuery implements ScaleQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlScaleQuery.class);
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    @Override
    public Flux<ByteBuf> findAsync(Plu plu) {
        final String sql = """
                SELECT
                    s.plu, s.name, s.grade, s.made_in, s.spec, s.shelf_life,
                    s.last_receipt_price, s.retail_price, s.member_price, s.vip_price,
                    json_build_object('id', c.id, 'name', c.name::jsonb ->> 'name') AS category,
                    json_build_object('id', b.id, 'name', b.name::jsonb ->> 'name') AS brand
                FROM scale s
                LEFT JOIN category c ON s.category_id = c.id
                LEFT JOIN brand b ON b.id = s.brand_id
                WHERE plu = ?
                """;
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        CompletableFuture.runAsync(() -> {
            try (Connection connection = PsqlUtil.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, plu.id());
                try (ResultSet rs = ps.executeQuery();
                     OutputStream os = new JsonByteBufOutputStream(sink, isCancelled);
                     JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                    if (rs.next()) {
                        PsqlScaleQuery.writeAloneScale(rs, gen);
                    }
                }
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((v, err) -> {
            if (err != null && !isCancelled.get()) {
                Throwable cause = err;
                if (err instanceof RuntimeException) cause = err.getCause();// 取出里面的原始 Exception,区分数据库异常和业务异常
                if (cause instanceof SQLException) {
                    LOGGER.warn("Database error while fetching scale {}", plu, cause);
                    sink.tryEmitError(new PersistenceException("Database error while fetching item " + plu, cause));
                } else if (cause instanceof IOException) {
                    LOGGER.warn("Failed to serialize for scale {}", plu, cause);
                    sink.tryEmitError(new PersistenceException("Failed to serialize cause by", cause));
                } else {  //未知错误 (兜底，防止请求挂起),比如代码逻辑错误导致的 NullPointerException 等
                    LOGGER.error("Unexpected system error while fetching scale {}", plu, cause);
                    sink.tryEmitError(new SearchException("Unexpected error: " + err.getMessage(), cause));
                }
            } else if (!isCancelled.get()) {
                sink.tryEmitComplete();
            }
        });
        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for plu: {}", plu))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    @Override
    public Flux<ByteBuf> searchAsync(SqlClauseSpec[] specs, int offset, int size, SortFieldEnum sortField) {
        if (offset < 0 || offset > 10000) throw new IllegalArgumentException("from must lager 10000");
        if (size < 0 || size > 10000) throw new IllegalArgumentException("size must lager 10000");
        if (offset + size > 10000) throw new IllegalArgumentException("Only the first 10,000 items are supported");
        if (sortField == null) {
            sortField = SortFieldEnum._ID;
            //LOGGER.info("The sorting field is not set, and the default id is used in reverse order");
        }
        final StringBuilder sql = new StringBuilder("""
                SELECT
                    s.plu, s.name, s.grade, s.made_in, s.spec, s.shelf_life,
                    s.last_receipt_price, s.retail_price, s.member_price, s.vip_price,
                    json_build_object('id', c.id, 'name', c.name::jsonb ->> 'name') AS category,
                    json_build_object('id', b.id, 'name', b.name::jsonb ->> 'name') AS brand
                FROM scale s
                LEFT JOIN category c ON s.category_id = c.id
                LEFT JOIN brand b ON s.brand_id = b.id
                """);
        // 过滤出满足条件的规格，然后生成 SQL 片段
        List<SqlClause> clauses = Stream.of(specs)
                .filter(SqlClauseSpec::isSatisfied) // 只保留满足的规格
                .map(SqlClauseSpec::toClause)      // 转换为 SQL 片段
                .toList();
        StringBuilder whereClause = new StringBuilder();
        List<Object> allParams = new ArrayList<>();
        for (SqlClause clause : clauses) {
            if (!clause.sql().isEmpty()) {
                if (!whereClause.isEmpty()) {
                    whereClause.append(" AND ");
                }
                whereClause.append(clause.sql().trim());
                allParams.addAll(clause.params());
            }
        }
        if (!whereClause.isEmpty()) {
            sql.append("WHERE ").append(whereClause);
        }
        String orderClause = PsqlScaleQuery.buildOrderClause(sortField);
        sql.append(orderClause);

        sql.append(" LIMIT ? OFFSET ?");
        allParams.add(size);
        allParams.add(offset);
        Object[] paramsArray = allParams.toArray();
        System.out.println(sql.toString());
        System.out.println(Arrays.toString(paramsArray));

        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        CompletableFuture.runAsync(() -> {
            try (Connection connection = PsqlUtil.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                for (int i = 0, j = paramsArray.length; i < j; i++) {
                    ps.setObject(i + 1, paramsArray[i]);
                }
                try (ResultSet rs = ps.executeQuery();
                     OutputStream os = new JsonByteBufOutputStream(sink, isCancelled);
                     JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                    int total = 0;
                    gen.writeStartObject();
                    gen.writeArrayFieldStart("scales");
                    while (rs.next()) {
                        PsqlScaleQuery.writeAloneScale(rs, gen);
                        total += 1;
                    }
                    gen.writeEndArray();
                    gen.writeNumberField("total", total);
                    gen.writeEndObject();
                    gen.flush();
                }
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((v, err) -> {
            if (err != null && !isCancelled.get()) {
                Throwable cause = err;
                if (err instanceof RuntimeException) cause = err.getCause();// 取出里面的原始 Exception,区分数据库异常和业务异常
                if (cause instanceof SQLException) {
                    LOGGER.warn("Database error while fetching scale {}", "info", cause);
                    sink.tryEmitError(new PersistenceException("Database error while fetching item " + "info", cause));
                } else if (cause instanceof IOException) {
                    LOGGER.warn("Failed to serialize for scale {}", "info", cause);
                    sink.tryEmitError(new PersistenceException("Failed to serialize cause by", cause));
                } else {  //未知错误 (兜底，防止请求挂起),比如代码逻辑错误导致的 NullPointerException 等
                    LOGGER.error("Unexpected system error while fetching scale {}", "info", cause);
                    sink.tryEmitError(new SearchException("Unexpected error: " + err.getMessage(), cause));
                }
            } else if (!isCancelled.get()) {
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for scale"))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

    private static void writeAloneScale(ResultSet rs, JsonGenerator gen) throws IOException, SQLException {
        gen.writeStartObject();
        gen.writeNumberField("plu", rs.getInt("plu"));
        gen.writeFieldName("name");
        gen.writeRawValue(rs.getString("name"));
        gen.writeStringField("spec", rs.getString("spec"));
        gen.writeStringField("grade", rs.getString("grade"));
        gen.writeFieldName("madeIn");
        gen.writeRawValue(rs.getString("made_in"));
        gen.writeNumberField("shelf_life", rs.getLong("shelf_life"));
        gen.writeFieldName("last_receipt_price");
        gen.writeRawValue(rs.getString("last_receipt_price"));
        gen.writeFieldName("retail_price");
        gen.writeRawValue(rs.getString("retail_price"));
        gen.writeFieldName("member_price");
        gen.writeRawValue(rs.getString("member_price"));
        gen.writeFieldName("vip_price");
        gen.writeRawValue(rs.getString("vip_price"));
        gen.writeFieldName("category");
        gen.writeRawValue(rs.getString("category"));
        gen.writeFieldName("brand");
        gen.writeRawValue(rs.getString("brand"));
        gen.writeEndObject();
    }

    private static String buildOrderClause(SortFieldEnum sortBy) {
        if (sortBy == null) {
            sortBy = SortFieldEnum._ID;
        }
        return " ORDER BY " + mapEsFieldToDbField(sortBy) + " " + sortBy.sort().toUpperCase();
    }

    private static String mapEsFieldToDbField(SortFieldEnum sortField) {
        return switch (sortField) {
            case ID, _ID -> "s.plu";
            case NAME, _NAME -> "s.name ->> 'mnemonic'";
            case BARCODE, _BARCODE -> "s.barcode";
            case MADE_IN, _MADE_IN -> "s.made_in";
            case GRADE, _GRADE -> "s.grade";
            case SPEC, _SPEC -> "s.spec";
            case CATEGORY, _CATEGORY -> "c.id";
            case BRAND, _BRAND -> "b.id";
            case LAST_RECEIPT_PRICE, _LAST_RECEIPT_PRICE -> "(s.last_receipt_price->>'number')::numeric";
            case RETAIL_PRICE, _RETAIL_PRICE -> "(s.retail_price->>'number')::numeric";
            case MEMBER_PRICE, _MEMBER_PRICE -> "(s.member_price->>'number')::numeric";
            case VIP_PRICE, _VIP_PRICE -> "(s.vip_price->>'number')::numeric";
            //case STOCK -> "i.stock";
        };
    }

    private static Flux<ByteBuf> findAsync(String sql, Consumer<PreparedStatement> paramSetter, String info) {
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        CompletableFuture.runAsync(() -> {
            try (Connection connection = PsqlUtil.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                paramSetter.accept(ps);
                try (ResultSet rs = ps.executeQuery();
                     OutputStream os = new JsonByteBufOutputStream(sink, isCancelled); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                    if (rs.next()) {
                        writeAloneScale(rs, gen);
                    }
                    gen.flush();
                }
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((v, err) -> {
            if (err != null && !isCancelled.get()) {
                Throwable cause = err;
                if (err instanceof RuntimeException) cause = err.getCause();// 取出里面的原始 Exception,区分数据库异常和业务异常
                if (cause instanceof SQLException) {
                    LOGGER.warn("Database error while fetching item {}", info, cause);
                    sink.tryEmitError(new PersistenceException("Database error while fetching item " + info, cause));
                } else if (cause instanceof IOException) {
                    LOGGER.warn("Failed to serialize for item {}", info, cause);
                    sink.tryEmitError(new PersistenceException("Failed to serialize cause by", cause));
                } else {  //未知错误 (兜底，防止请求挂起),比如代码逻辑错误导致的 NullPointerException 等
                    LOGGER.error("Unexpected system error while fetching item {}", info, cause);
                    sink.tryEmitError(new SearchException("Unexpected error: " + err.getMessage(), cause));
                }
            } else if (!isCancelled.get()) {
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux()
                .doOnCancel(() -> isCancelled.set(true))
                .doOnTerminate(() -> LOGGER.debug("Request terminated for id: {}", info))
                .doOnDiscard(ByteBuf.class, ByteBuf::release); // 确保释放资源
    }

}
