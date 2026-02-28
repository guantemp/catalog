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

package catalog.hoprxi.core.infrastructure.query.postgresql;

import catalog.hoprxi.core.application.query.ItemQueryFilter;
import catalog.hoprxi.core.application.query.SearchException;
import catalog.hoprxi.core.application.query.SortFieldEnum;
import catalog.hoprxi.core.application.view.ItemView;
import catalog.hoprxi.core.domain.model.GradeEnum;
import catalog.hoprxi.core.domain.model.Name;
import catalog.hoprxi.core.domain.model.Specification;
import catalog.hoprxi.core.domain.model.barcode.Barcode;
import catalog.hoprxi.core.domain.model.barcode.BarcodeGenerateServices;
import catalog.hoprxi.core.domain.model.madeIn.Domestic;
import catalog.hoprxi.core.domain.model.madeIn.Imported;
import catalog.hoprxi.core.domain.model.madeIn.MadeIn;
import catalog.hoprxi.core.domain.model.price.*;
import catalog.hoprxi.core.domain.model.shelfLife.ShelfLife;
import catalog.hoprxi.core.infrastructure.PsqlUtil;
import catalog.hoprxi.core.infrastructure.persistence.PersistenceException;
import catalog.hoprxi.core.infrastructure.query.JsonByteBufOutputStream;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import salt.hoprxi.cache.Cache;
import salt.hoprxi.cache.CacheFactory;
import salt.hoprxi.utils.NumberHelper;

import javax.money.MonetaryAmount;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/***
 * @author <a href="www.hoprxi.com/authors/guan xiangHuan">guan xiangHuang</a>
 * @since JDK21
 * @version 0.0.3 builder 2026-02-28
 */

public class PsqlItemQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlItemQuery.class);
    private static final Cache<String, ItemView> CACHE = CacheFactory.build("itemView");
    private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();
    private static final int SINGLE_BUFFER_SIZE = 1024;//2KB缓冲区

    public InputStream find(long id) {
        final String findSql1 = """
                SELECT i.id, i.name, i.barcode, i.grade, i.made_in, i.spec, i.shelf_life,
                       i.retail_price, i.last_receipt_price, i.member_price, i.vip_price,
                        json_build_object('id', c.id, 'name', c.name::jsonb ->> 'name') AS category,
                        json_build_object('id', b.id, 'name', b.name::jsonb ->> 'name') AS brand
                        FROM item i
                        LEFT JOIN category c ON i.category_id = c.id
                        LEFT JOIN brand b ON i.brand_id = b.id
                        WHERE i.id = ?
                        LIMIT 1
                """;
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(SINGLE_BUFFER_SIZE);
        boolean success = false;
        try (Connection connection = PsqlUtil.getConnection(); PreparedStatement ps = connection.prepareStatement(findSql1)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery();
                 OutputStream os = new ByteBufOutputStream(buffer); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                if (!rs.next()) {
                    // 没找到数据，释放 buffer 并返回 null
                    ReferenceCountUtil.safeRelease(buffer);
                    return null;
                }
                gen.writeStartObject();
                gen.writeNumberField("id", rs.getLong("id"));
                gen.writeFieldName("name");
                gen.writeRawValue(rs.getString("name"));
                gen.writeStringField("barcode", rs.getString("barcode"));
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
                gen.flush();
                success = true;
                return new ByteBufInputStream(buffer, true);
            }
        } catch (SQLException e) {
            LOGGER.warn("Database error while fetching item id={}", id, e);
            throw new PersistenceException("Database error while fetching item id= " + id, e);
        } catch (IOException e) {
            LOGGER.error("I/O failed", e);
            throw new SearchException("Failed to serialize item data::", e);
        } finally {
            if (!success && buffer.refCnt() > 0) {
                ReferenceCountUtil.safeRelease(buffer);
            }
        }
    }

    public Flux<ByteBuf> findAsync(long id) {
        final String findSql = """
                SELECT i.id, i.name, i.barcode, i.grade, i.made_in, i.spec, i.shelf_life,
                       i.retail_price, i.last_receipt_price, i.member_price, i.vip_price,
                        json_build_object('id', c.id, 'name', c.name::jsonb ->> 'name') AS category,
                        json_build_object('id', b.id, 'name', b.name::jsonb ->> 'name') AS brand
                        FROM item i
                        LEFT JOIN category c ON i.category_id = c.id
                        LEFT JOIN brand b ON i.brand_id = b.id
                        WHERE i.id = ?
                        LIMIT 1
                """;
        return PsqlItemQuery.findAsync(findSql, ps -> {
            try {
                ps.setLong(1, id);
            } catch (SQLException e) {
                throw new RuntimeException("Parameter setting failed for id=" + id, e);
            }
        }, "id=" + id);
    }

    public Flux<ByteBuf> findByBarcodeAsync(String barcode) {
        final String findSql = """
                SELECT i.id, i.name, i.barcode, i.grade, i.made_in, i.spec, i.shelf_life,
                       i.retail_price, i.last_receipt_price, i.member_price, i.vip_price,
                        json_build_object('id', c.id, 'name', c.name::jsonb ->> 'name') AS category,
                        json_build_object('id', b.id, 'name', b.name::jsonb ->> 'name') AS brand
                        FROM item i
                        LEFT JOIN category c ON i.category_id = c.id
                        LEFT JOIN brand b ON i.brand_id = b.id
                        WHERE i.barcode = ?
                        LIMIT 1
                """;
        return PsqlItemQuery.findAsync(findSql, ps -> {
            try {
                ps.setString(1, barcode);
            } catch (SQLException e) {
                throw new RuntimeException("Parameter setting failed for barcode=" + barcode, e);
            }
        }, "barcode=" + barcode);
    }

    /*
     * 通用异步查询执行器
     *
     * @param sql         SQL 查询语句
     * @param paramSetter 参数设置回调
     * @param logId       用于日志记录的标识符 (例如: "id=123" 或 "barcode=ABC")
     */
    private static Flux<ByteBuf> findAsync(String sql, Consumer<PreparedStatement> paramSetter, String info) {
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        Sinks.Many<ByteBuf> sink = Sinks.many().unicast().onBackpressureBuffer();  // 使用单播接收器（更高效）
        CompletableFuture.runAsync(() -> {
            try (Connection connection = PsqlUtil.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                paramSetter.accept(ps);
                try (ResultSet rs = ps.executeQuery();
                     OutputStream os = new JsonByteBufOutputStream(sink, isCancelled); JsonGenerator gen = JSON_FACTORY.createGenerator(os)) {
                    if (rs.next()) {
                        gen.writeStartObject();
                        gen.writeNumberField("id", rs.getLong("id"));
                        gen.writeFieldName("name");
                        gen.writeRawValue(rs.getString("name"));
                        gen.writeStringField("barcode", rs.getString("barcode"));
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


    public ItemView[] belongToBrand(String brandId, long offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit,i.show\n" +
                    "from item i left join brand b on i.brand_id = b.id left join category c on c.id = i.category_id where b.id = ? offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, Long.parseLong(brandId));
            ps.setLong(2, offset);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }


    public ItemView[] belongToCategory(String categoryId, long offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where c.id = ? offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, NumberHelper.longOf(categoryId, 0L));
            ps.setLong(2, offset);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }


    public ItemView[] belongToCategoryAndDescendants(String categoryId, long offset, int limit) {
        categoryId = categoryId.trim();
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic'  mnemonic,i.name::jsonb ->> 'alias'  alias,i.barcode,\n" +
                    "i.category_id,c.name::jsonb ->> 'name'  category_name,i.brand_id,b.name::jsonb ->> 'name'  brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number'  retail_price_number,i.retail_price::jsonb ->> 'currencyCode'  retail_price_currencyCode,i.retail_price::jsonb ->> 'unit'  retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name'  member_price_name,i.member_price::jsonb -> 'price' ->> 'number'  member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode'  member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit'  member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name'  vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number'  vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode'  vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit'  vip_price_unit,i.show\n" +
                    "from category c left join item i on i.category_id = c.id left join brand b on b.id = i.brand_id\n" +
                    "where c.id in (select id from category where root_id = (select root_id from category where id = ?) and \"left\" >= (select \"left\" from category where id = ?)\n" +
                    "and \"right\" <= (select \"right\" from category where id =?)) offset ? limit ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            long cid = NumberHelper.longOf(categoryId, 0l);
            ps.setLong(1, cid);
            ps.setLong(2, cid);
            ps.setLong(3, cid);
            ps.setLong(4, offset);
            ps.setInt(5, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }

    public Flux<ByteBuf> searchAsync(ItemQueryFilter[] filters, int offset, int size, SortFieldEnum sortField){
        return null;
    }


    public ItemView[] queryAll(long offset, int limit) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String findSql = "select i.id,i.name::jsonb ->> 'name' name, i.name::jsonb ->> 'mnemonic' mnemonic,i.name::jsonb ->> 'alias' alias,i.barcode,\n" +
                    "i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id limit ? offset ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setLong(2, offset);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }

    public long size() {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String sql = "select count(*) from item";
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("count");
            }
        } catch (SQLException e) {
            LOGGER.error("Can't count itemView size", e);
        }
        return 0L;
    }

    public ItemView[] queryByBarcode(String barcode) {
        barcode = barcode.trim();
        try (Connection connection = PsqlUtil.getConnection()) {
            final String findSql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,\n" +
                    "i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode ~ ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setString(1, barcode);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild itemView", e);
        }
        return new ItemView[0];
    }


    public ItemView[] queryByRegular(String regularExpression) {
        try (Connection connection = PsqlUtil.getConnection()) {
            final String findSql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode ~ ?" +
                    "union\n" +
                    "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where text(textsend_i(i.name ->> 'name')) ~ ltrim(text(textsend_i(?)), '\\x')\n" +
                    "union\n" +
                    "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where text(textsend_i(i.name ->> 'alias')) ~ ltrim(text(textsend_i(?)), '\\x')\n" +
                    "union\n" +
                    "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                    "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                    "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                    "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                    "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                    "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.\"name\" ->> 'mnemonic' ~ ?";
            PreparedStatement ps = connection.prepareStatement(findSql);
            ps.setString(1, regularExpression);
            ps.setString(2, regularExpression);
            ps.setString(3, regularExpression);
            ps.setString(4, regularExpression);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }


    public ItemView[] queryByName(String expression, long offset, int limit) {
        final String sql = "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.barcode ~ ?" +
                "union\n" +
                "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where text(textsend_i(i.name ->> 'name')) ~ ltrim(text(textsend_i(?)), '\\x')\n" +
                "union\n" +
                "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where text(textsend_i(i.name ->> 'alias')) ~ ltrim(text(textsend_i(?)), '\\x')\n" +
                "union\n" +
                "select i.id,i.\"name\"::jsonb ->> 'name' name,i.\"name\"::jsonb ->> 'mnemonic' mnemonic,i.\"name\"::jsonb ->> 'alias' alias,i.barcode,i.category_id,c.name::jsonb ->> 'name' category_name,i.brand_id,b.name::jsonb ->> 'name' brand_name,i.grade, i.made_in,i.spec,i.shelf_life,\n" +
                "i.last_receipt_price::jsonb ->> 'name' last_receipt_price_name,i.last_receipt_price::jsonb -> 'price' ->> 'number' last_receipt_price_number,i.last_receipt_price::jsonb -> 'price' ->> 'currencyCode' last_receipt_price_currencyCode,i.last_receipt_price::jsonb -> 'price' ->> 'unit' last_receipt_price_unit,\n" +
                "i.retail_price::jsonb ->> 'number' retail_price_number,i.retail_price::jsonb ->> 'currencyCode' retail_price_currencyCode,i.retail_price::jsonb ->> 'unit' retail_price_unit,\n" +
                "i.member_price::jsonb ->> 'name' member_price_name,i.member_price::jsonb -> 'price' ->> 'number' member_price_number,i.member_price::jsonb -> 'price' ->> 'currencyCode' member_price_currencyCode,i.member_price::jsonb -> 'price' ->> 'unit' member_price_unit,\n" +
                "i.vip_price::jsonb ->> 'name' vip_price_name,i.vip_price::jsonb -> 'price' ->> 'number' vip_price_number,i.vip_price::jsonb -> 'price' ->> 'currencyCode' vip_price_currencyCode,i.vip_price::jsonb -> 'price' ->> 'unit' vip_price_unit,i.show\n" +
                "from item i left join category c on i.category_id = c.id left join brand b on b.id = i.brand_id where i.\"name\" ->> 'mnemonic' ~ ? limit ? offset ?";
        try (Connection connection = PsqlUtil.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, expression);
            ps.setString(2, expression);
            ps.setString(3, expression);
            ps.setString(4, expression);
            ps.setInt(5, limit);
            ps.setLong(6, offset);
            ResultSet rs = ps.executeQuery();
            return transform(rs);
        } catch (SQLException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 IOException e) {
            LOGGER.error("Can't rebuild item", e);
        }
        return new ItemView[0];
    }

    private ItemView[] transform(ResultSet rs) throws SQLException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<ItemView> itemViews = new ArrayList<>();
        while (rs.next()) {
            itemViews.add(rebuild(rs));
        }
        return itemViews.toArray(new ItemView[0]);
    }

    private static ItemView rebuild(ResultSet rs) throws InvocationTargetException, InstantiationException, IllegalAccessException, SQLException, IOException {
        String id = rs.getString("id");
        Name name = new Name(rs.getString("name"), rs.getString("alias"));
        Barcode barcode = BarcodeGenerateServices.createBarcode(rs.getString("barcode"));
        GradeEnum grade = GradeEnum.valueOf(rs.getString("grade"));
        MadeIn madeIn = toMadeIn(rs.getString("made_in"));
        Specification spec = new Specification(rs.getString("spec"));
        ShelfLife shelfLife = ShelfLife.create(rs.getInt("shelf_life"));
        ItemView itemView = new ItemView(id, barcode, name, madeIn, spec, grade, shelfLife);

        ItemView.CategoryView categoryView = new ItemView.CategoryView(rs.getString("category_id"), rs.getString("category_name"));
        itemView.setCategoryView(categoryView);
        ItemView.BrandView brandView = new ItemView.BrandView(rs.getString("brand_id"), rs.getString("brand_name"));
        itemView.setBrandView(brandView);

        String priceName = rs.getString("last_receipt_price_name");
        MonetaryAmount amount = Money.of(rs.getBigDecimal("last_receipt_price_number"), rs.getString("last_receipt_price_currencyCode"));
        UnitEnum unit = UnitEnum.valueOf(rs.getString("last_receipt_price_unit"));
        LastReceiptPrice lastReceiptPrice = new LastReceiptPrice(priceName, new Price(amount, unit));
        itemView.setLastReceiptPrice(lastReceiptPrice);
        amount = Money.of(rs.getBigDecimal("retail_price_number"), rs.getString("retail_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("retail_price_unit"));
        RetailPrice retailPrice = new RetailPrice(new Price(amount, unit));
        itemView.setRetailPrice(retailPrice);
        priceName = rs.getString("member_price_name");
        amount = Money.of(rs.getBigDecimal("member_price_number"), rs.getString("member_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("member_price_unit"));
        MemberPrice memberPrice = new MemberPrice(priceName, new Price(amount, unit));
        itemView.setMemberPrice(memberPrice);
        priceName = rs.getString("vip_price_name");
        amount = Money.of(rs.getBigDecimal("vip_price_number"), rs.getString("vip_price_currencyCode"));
        unit = UnitEnum.valueOf(rs.getString("vip_price_unit"));
        VipPrice vipPrice = new VipPrice(priceName, new Price(amount, unit));
        itemView.setVipPrice(vipPrice);
        itemView.setImages(toImages(rs.getBinaryStream("show")));
        //for (URI uri : toImages(rs.getBinaryStream("show")))
        //System.out.println(barcode + ":" + uri);
        return itemView;
    }

    private static MadeIn toMadeIn(String json) throws IOException {
        String _class = null;
        String madeIn = null;
        String code = MadeIn.UNKNOWN.code();
        JsonParser parser = JSON_FACTORY.createParser(json.getBytes(StandardCharsets.UTF_8));
        while (!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();
            if (JsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                switch (fieldName) {
                    case "_class":
                        _class = parser.getValueAsString();
                        break;
                    case "madeIn":
                        madeIn = parser.getValueAsString();
                        break;
                    case "code":
                        code = parser.getValueAsString();
                        break;
                }
            }
        }
        if (MadeIn.UNKNOWN.code().equals(code)) {
            return MadeIn.UNKNOWN;
        } else if (Domestic.class.getName().equals(_class)) {
            return new Domestic(code, madeIn);
        } else if (Imported.class.getName().equals(_class)) {
            return new Imported(code, madeIn);
        }
        return MadeIn.UNKNOWN;
    }

    private static URI[] toImages(InputStream is) throws IOException {
        URI[] result = new URI[0];
        JsonParser parser = JSON_FACTORY.createParser(is);
        while (!parser.isClosed()) {
            if (JsonToken.FIELD_NAME == parser.nextToken()) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();
                if (fieldName.equals("images")) {
                    result = readImages(parser);
                }
            }
        }
        return result;
    }

    private static URI[] readImages(JsonParser parser) throws IOException {
        List<URI> uriList = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            uriList.add(URI.create(parser.getValueAsString()));
        }
        return uriList.toArray(new URI[0]);
    }
}
